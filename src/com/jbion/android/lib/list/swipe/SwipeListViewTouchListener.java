package com.jbion.android.lib.list.swipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

// import com.nineoldandroids.animation.Animator;
// import com.nineoldandroids.animation.AnimatorListenerAdapter;
// import com.nineoldandroids.animation.AnimatorUpdateListener;
// import com.nineoldandroids.animation.ValueAnimator;
// import com.nineoldandroids.view.ViewHelper;
// import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * Touch listener impl for the SwipeListView
 */
class SwipeListViewTouchListener implements View.OnTouchListener {

    private static final String LOG_TAG = SwipeListViewTouchListener.class.getSimpleName();

    private static final int DISPLACE_CHOICE = 80;

    /**
     * Indicates no movement
     */
    private final static int TOUCH_STATE_REST = 0;
    /**
     * State scrolling x position
     */
    private final static int TOUCH_STATE_SCROLLING_X = 1;
    /**
     * State scrolling y position
     */
    private final static int TOUCH_STATE_SCROLLING_Y = 2;

    private final SwipeListView listView;
    private final SwipeOptions opts;

    private Rect rect = new Rect();

    // Cached ViewConfiguration and system-wide constant values
    private final int slop;
    private final int minFlingVelocity;
    private final int maxFlingVelocity;
    private final long configShortAnimationTime;

    // Fixed properties
    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

    private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();
    private int dismissAnimationRefCount = 0;

    private int swipeCurrentAction = SwipeOptions.ACTION_NONE;
    private int currentActionLeft = SwipeOptions.ACTION_REVEAL;
    private int currentActionRight = SwipeOptions.ACTION_REVEAL;

    private List<Boolean> opened = new ArrayList<Boolean>();
    private List<Boolean> openedRight = new ArrayList<Boolean>();
    private List<Boolean> checked = new ArrayList<Boolean>();

    private int touchState = TOUCH_STATE_REST;
    private int touchSlop;
    private float lastMotionX;
    private float lastMotionY;

    private float downX;
    private boolean swiping;
    private boolean swipingRight;
    private VelocityTracker velocityTracker;

    private int downPosition;
    private View parentView;
    private View frontView;
    private View backView;
    private boolean paused;

    /**
     * Constructor
     * 
     * @param listView
     *            SwipeListView
     * @param options
     */
    public SwipeListViewTouchListener(SwipeListView listView, SwipeOptions options) {
        this.listView = listView;
        this.opts = options;

        Context ctx = listView.getContext();
        ViewConfiguration vc = ViewConfiguration.get(ctx);
        slop = vc.getScaledTouchSlop();
        touchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(vc);
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        configShortAnimationTime = ctx.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        if (opts.animationTime <= 0) {
            opts.animationTime = configShortAnimationTime;
        }
        currentActionLeft = opts.swipeActionLeft;
        currentActionRight = opts.swipeActionRight;
    }

    /**
     * Sets current item's parent view
     * 
     * @param parentView
     *            Parent view
     */
    private void setParentView(View parentView) {
        this.parentView = parentView;
    }

    /**
     * Sets current item's front view
     * 
     * @param frontView
     *            Front view
     */
    private void setFrontView(View frontView) {
        this.frontView = frontView;
        frontView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.onClickFrontView(downPosition);
            }
        });
        if (opts.openOnLongClick) {
            frontView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openAnimate(downPosition);
                    return false;
                }
            });
        }
    }

    /**
     * Set current item's back view
     * 
     * @param backView
     */
    private void setBackView(View backView) {
        this.backView = backView;
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.onClickBackView(downPosition);
            }
        });
    }

    /**
     * Check is swiping is enabled
     * 
     * @return
     */
    protected boolean isSwipeEnabled() {
        return opts.swipeMode != SwipeOptions.SWIPE_MODE_NONE;
    }

    /**
     * Adds new items when adapter is modified
     */
    public void resetItems() {
        if (listView.getAdapter() != null) {
            int count = listView.getAdapter().getCount();
            for (int i = opened.size(); i <= count; i++) {
                opened.add(false);
                openedRight.add(false);
                checked.add(false);
            }
        }
    }

    /**
     * Open item
     * 
     * @param position
     *            Position of list
     */
    protected void openAnimate(int position) {
        openAnimate(listView.getChildAt(position - listView.getFirstVisiblePosition())
                .findViewById(opts.frontViewId), position);
    }

    /**
     * Close item
     * 
     * @param position
     *            Position of list
     */
    protected void closeAnimate(int position) {
        closeAnimate(listView.getChildAt(position - listView.getFirstVisiblePosition())
                .findViewById(opts.frontViewId), position);
    }

    /**
     * Swap choice state in item
     * 
     * @param position
     *            position of list
     */
    private void swapChoiceState(int position) {
        int lastCount = getCountSelected();
        boolean lastChecked = checked.get(position);
        checked.set(position, !lastChecked);
        int count = lastChecked ? lastCount - 1 : lastCount + 1;
        if (lastCount == 0 && count == 1) {
            listView.onChoiceStarted();
            closeOpenedItems();
            setActionsTo(SwipeOptions.ACTION_CHOICE);
        }
        if (lastCount == 1 && count == 0) {
            listView.onChoiceEnded();
            resetOldActions();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            listView.setItemChecked(position, !lastChecked);
        }
        listView.onChoiceChanged(position, !lastChecked);
        reloadChoiceStateInView(frontView, position);
    }

    /**
     * Unselected choice state in item
     */
    protected void unselectedChoiceStates() {
        int start = listView.getFirstVisiblePosition();
        int end = listView.getLastVisiblePosition();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i) && i >= start && i <= end) {
                reloadChoiceStateInView(
                        listView.getChildAt(i - start).findViewById(opts.frontViewId), i);
            }
            checked.set(i, false);
        }
        listView.onChoiceEnded();
        resetOldActions();
    }

    /**
     * Unselected choice state in item
     */
    protected int dismiss(int position) {
        int start = listView.getFirstVisiblePosition();
        int end = listView.getLastVisiblePosition();
        View view = listView.getChildAt(position - start);
        ++dismissAnimationRefCount;
        if (position >= start && position <= end) {
            performDismiss(view, position, false);
            return view.getHeight();
        } else {
            pendingDismisses.add(new PendingDismissData(position, null));
            return 0;
        }
    }

    /**
     * Draw cell for display if item is selected or not
     * 
     * @param view
     *            the front view to reload
     * @param position
     *            position in list
     */
    protected void reloadChoiceStateInView(View view, int position) {
        if (isChecked(position)) {
            if (opts.drawableChecked > 0)
                view.setBackgroundResource(opts.drawableChecked);
        } else {
            if (opts.drawableUnchecked > 0)
                view.setBackgroundResource(opts.drawableUnchecked);
        }
    }

    /**
     * Get if item is selected
     * 
     * @param position
     *            position in list
     * @return
     */
    protected boolean isChecked(int position) {
        return position < checked.size() && checked.get(position);
    }

    /**
     * Count selected
     * 
     * @return
     */
    protected int getCountSelected() {
        int count = 0;
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i)) {
                count++;
            }
        }
        Log.d("SwipeListView", "selected: " + count);
        return count;
    }

    /**
     * Get positions selected
     * 
     * @return
     */
    protected List<Integer> getSelectedPositions() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i)) {
                list.add(i);
            }
        }
        return list;
    }

    /**
     * Open item
     * 
     * @param view
     *            affected view
     * @param position
     *            Position of list
     */
    private void openAnimate(View view, int position) {
        if (!opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Close item
     * 
     * @param view
     *            affected view
     * @param position
     *            Position of list
     */
    private void closeAnimate(View view, int position) {
        if (opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Create animation
     * 
     * @param view
     *            affected view
     * @param swap
     *            If state should change. If "false" returns to the original position
     * @param swapRight
     *            If swap is true, this parameter tells if move is to the right or
     *            left
     * @param position
     *            Position of list
     */
    private void generateAnimate(final View view, final boolean swap, final boolean swapRight,
            final int position) {
        Log.d("SwipeListView", "swap: " + swap + " - swapRight: " + swapRight + " - position: "
                + position);
        if (swipeCurrentAction == SwipeOptions.ACTION_REVEAL) {
            generateRevealAnimate(view, swap, swapRight, position);
        }
        if (swipeCurrentAction == SwipeOptions.ACTION_DISMISS) {
            generateDismissAnimate(parentView, swap, swapRight, position);
        }
        if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
            generateChoiceAnimate(view, position);
        }
    }

    /**
     * Create choice animation
     * 
     * @param view
     *            affected view
     * @param position
     *            list position
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void generateChoiceAnimate(final View view, final int position) {
        view.animate().translationX(0).setDuration(opts.animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        touchState = TOUCH_STATE_REST;
                        resetCell();
                    }
                });
    }

    private int calculateOffset(boolean swapRight) {
        if (opts.swipeOffsetType == SwipeOptions.OFFSET_TYPE_TRAVELED) {
            return swapRight ? (int) (opts.swipeOffsetLeft) : (int) (-opts.swipeOffsetRight);
        } else {
            return swapRight ? (int) (viewWidth - opts.swipeOffsetRight)
                    : (int) (-viewWidth + opts.swipeOffsetLeft);
        }
    }

    /**
     * Create dismiss animation
     * 
     * @param view
     *            affected view
     * @param swap
     *            If will change state. If is "false" returns to the original
     *            position
     * @param swapRight
     *            If swap is true, this parameter tells if move is to the right or
     *            left
     * @param position
     *            Position of list
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void generateDismissAnimate(final View view, final boolean swap,
            final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = calculateOffset(openedRight.get(position));
            }
        } else {
            if (swap) {
                moveTo = calculateOffset(swapRight);
            }
        }

        int alpha = 1;
        if (swap) {
            ++dismissAnimationRefCount;
            alpha = 0;
        }

        view.animate().translationX(moveTo).alpha(alpha).setDuration(opts.animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (swap) {
                            closeOpenedItems();
                            performDismiss(view, position, true);
                        }
                        resetCell();
                    }
                });

    }

    /**
     * Create reveal animation
     * 
     * @param view
     *            affected view
     * @param swap
     *            If will change state. If "false" returns to the original position
     * @param swapRight
     *            If swap is true, this parameter tells if movement is toward right
     *            or left
     * @param position
     *            list position
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void generateRevealAnimate(final View view, final boolean swap,
            final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = calculateOffset(openedRight.get(position));
            }
        } else {
            if (swap) {
                moveTo = calculateOffset(swapRight);
            }
        }

        view.animate().translationX(moveTo).setDuration(opts.animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        touchState = TOUCH_STATE_REST;
                        if (swap) {
                            boolean aux = !opened.get(position);
                            opened.set(position, aux);
                            if (aux) {
                                listView.onOpened(position, swapRight);
                                openedRight.set(position, swapRight);
                            } else {
                                listView.onClosed(position, openedRight.get(position));
                            }
                        }
                        resetCell();
                    }
                });
    }

    private void resetCell() {
        if (downPosition != AdapterView.INVALID_POSITION) {
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                backView.setVisibility(View.VISIBLE);
            }
            frontView.setClickable(opened.get(downPosition));
            frontView.setLongClickable(opened.get(downPosition));
            frontView = null;
            backView = null;
            downPosition = AdapterView.INVALID_POSITION;
        }
    }

    /**
     * Set enabled
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        paused = !enabled;
    }

    /**
     * Return ScrollListener for ListView
     * 
     * @return OnScrollListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {

            private boolean isFirstItem = false;
            private boolean isLastItem = false;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                if (opts.closeAllItemsOnScroll && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    closeOpenedItems();
                }
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    setEnabled(false);
                }
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING
                        && scrollState != SCROLL_STATE_TOUCH_SCROLL) {
                    downPosition = AdapterView.INVALID_POSITION;
                    touchState = TOUCH_STATE_REST;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setEnabled(true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                if (isFirstItem) {
                    boolean onSecondItemList = firstVisibleItem == 1;
                    if (onSecondItemList) {
                        isFirstItem = false;
                    }
                } else {
                    boolean onFirstItemList = firstVisibleItem == 0;
                    if (onFirstItemList) {
                        isFirstItem = true;
                        listView.onFirstListItem();
                    }
                }
                if (isLastItem) {
                    boolean onBeforeLastItemList = firstVisibleItem + visibleItemCount == totalItemCount - 1;
                    if (onBeforeLastItemList) {
                        isLastItem = false;
                    }
                } else {
                    boolean onLastItemList = firstVisibleItem + visibleItemCount >= totalItemCount;
                    if (onLastItemList) {
                        isLastItem = true;
                        listView.onLastListItem();
                    }
                }
            }
        };
    }

    /**
     * Close all opened items
     */
    void closeOpenedItems() {
        if (opened != null) {
            int start = listView.getFirstVisiblePosition();
            int end = listView.getLastVisiblePosition();
            for (int i = start; i <= end; i++) {
                if (opened.get(i)) {
                    closeAnimate(listView.getChildAt(i - start).findViewById(opts.frontViewId), i);
                }
            }
        }

    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        if (isSwipeEnabled()) {

            if (touchState == TOUCH_STATE_SCROLLING_X) {
                return onTouch(listView, ev);
            }

            switch (action) {
            case MotionEvent.ACTION_DOWN:
                onTouch(listView, ev);
                touchState = TOUCH_STATE_REST;
                lastMotionX = x;
                lastMotionY = y;
                return false;
            case MotionEvent.ACTION_MOVE:
                updateScrollDirection(x, y);
                return touchState == TOUCH_STATE_SCROLLING_Y;
            case MotionEvent.ACTION_UP:
                onTouch(listView, ev);
                return touchState == TOUCH_STATE_SCROLLING_Y;
            case MotionEvent.ACTION_CANCEL:
                touchState = TOUCH_STATE_REST;
                break;
            default:
                break;
            }
        }
        return false;
    }

    /**
     * Check if the user is moving the cell
     * 
     * @param x
     *            Position X
     * @param y
     *            Position Y
     */
    private void updateScrollDirection(float x, float y) {
        final int xDiff = (int) Math.abs(x - lastMotionX);
        final int yDiff = (int) Math.abs(y - lastMotionY);

        boolean xMoved = xDiff > touchSlop;
        boolean yMoved = yDiff > touchSlop;

        if (xMoved || yMoved) {
            if (xDiff > yDiff) {
                touchState = TOUCH_STATE_SCROLLING_X;
            } else {
                touchState = TOUCH_STATE_SCROLLING_Y;
            }
            lastMotionX = x;
            lastMotionY = y;
        }
    }

    /**
     * @see android.view.View.OnTouchListener#onTouch(android.view.View,
     *      android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!isSwipeEnabled()) {
            Log.d(LOG_TAG, "onTouch XXXX returns false (swipe disabled)");
            return false;
        }

        if (viewWidth < 2) {
            viewWidth = listView.getWidth();
        }

        switch (MotionEventCompat.getActionMasked(motionEvent)) {
        case MotionEvent.ACTION_DOWN: {
            if (paused && downPosition != AdapterView.INVALID_POSITION) {
                Log.d(LOG_TAG, "onTouch DOWN returns false");
                return false;
            }
            swipeCurrentAction = SwipeOptions.ACTION_NONE;

            int childCount = listView.getChildCount();
            int[] listViewCoords = new int[2];
            listView.getLocationOnScreen(listViewCoords);
            int x = (int) motionEvent.getRawX() - listViewCoords[0];
            int y = (int) motionEvent.getRawY() - listViewCoords[1];
            View child;
            for (int i = 0; i < childCount; i++) {
                child = listView.getChildAt(i);
                child.getHitRect(rect);

                int childPosition = listView.getPositionForView(child);

                // dont allow swiping if this is on the header or footer or
                // IGNORE_ITEM_VIEW_TYPE or enabled is false on the adapter
                boolean allowSwipe = listView.getAdapter().isEnabled(childPosition)
                        && listView.getAdapter().getItemViewType(childPosition) >= 0;

                if (allowSwipe && rect.contains(x, y)) {
                    setParentView(child);
                    setFrontView(child.findViewById(opts.frontViewId));

                    downX = motionEvent.getRawX();
                    downPosition = childPosition;

                    frontView.setClickable(!opened.get(downPosition));
                    frontView.setLongClickable(!opened.get(downPosition));

                    velocityTracker = VelocityTracker.obtain();
                    velocityTracker.addMovement(motionEvent);
                    if (opts.backViewId > 0) {
                        setBackView(child.findViewById(opts.backViewId));
                    }
                    break;
                }
            }
            view.onTouchEvent(motionEvent);
            Log.d(LOG_TAG, "onTouch DOWN returns true");
            return true;
        }

        case MotionEvent.ACTION_UP: {
            if (velocityTracker == null || !swiping || downPosition == AdapterView.INVALID_POSITION) {
                break;
            }

            float deltaX = motionEvent.getRawX() - downX;
            velocityTracker.addMovement(motionEvent);
            velocityTracker.computeCurrentVelocity(1000);
            float velocityX = Math.abs(velocityTracker.getXVelocity());
            if (!opened.get(downPosition)) {
                if (opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT
                        && velocityTracker.getXVelocity() > 0) {
                    velocityX = 0;
                }
                if (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT
                        && velocityTracker.getXVelocity() < 0) {
                    velocityX = 0;
                }
            }
            float velocityY = Math.abs(velocityTracker.getYVelocity());
            boolean swap = false;
            boolean swapRight = false;
            if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity
                    && velocityY * 2 < velocityX) {
                swapRight = velocityTracker.getXVelocity() > 0;
                Log.d("SwipeListView", "swapRight: " + swapRight + " - swipingRight: "
                        + swipingRight);
                if (swapRight != swipingRight && opts.swipeActionLeft != opts.swipeActionRight) {
                    swap = false;
                } else if (opened.get(downPosition) && openedRight.get(downPosition) && swapRight) {
                    swap = false;
                } else if (opened.get(downPosition) && !openedRight.get(downPosition) && !swapRight) {
                    swap = false;
                } else {
                    swap = true;
                }
            } else if (Math.abs(deltaX) > viewWidth / 2) {
                swap = true;
                swapRight = deltaX > 0;
            }
            generateAnimate(frontView, swap, swapRight, downPosition);
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                swapChoiceState(downPosition);
            }

            velocityTracker.recycle();
            velocityTracker = null;
            downX = 0;
            // change clickable front view
            // if (swap) {
            // frontViewId.setClickable(opened.get(downPosition));
            // frontViewId.setLongClickable(opened.get(downPosition));
            // }
            swiping = false;
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            if (velocityTracker == null || paused || downPosition == AdapterView.INVALID_POSITION) {
                break;
            }

            velocityTracker.addMovement(motionEvent);
            velocityTracker.computeCurrentVelocity(1000);
            float velocityX = Math.abs(velocityTracker.getXVelocity());
            float velocityY = Math.abs(velocityTracker.getYVelocity());

            float deltaX = motionEvent.getRawX() - downX;
            float deltaMode = Math.abs(deltaX);

            int changeSwipeMode = listView.onChangeSwipeMode(downPosition);
            if (changeSwipeMode >= 0) {
            }

            if (opts.swipeMode == SwipeOptions.SWIPE_MODE_NONE) {
                deltaMode = 0;
            } else if (opts.swipeMode != SwipeOptions.SWIPE_MODE_BOTH) {
                if (opened.get(downPosition)) {
                    if (opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT && deltaX < 0) {
                        deltaMode = 0;
                    } else if (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT && deltaX > 0) {
                        deltaMode = 0;
                    }
                } else {
                    if (opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT && deltaX > 0) {
                        deltaMode = 0;
                    } else if (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT && deltaX < 0) {
                        deltaMode = 0;
                    }
                }
            }
            if (deltaMode > slop && swipeCurrentAction == SwipeOptions.ACTION_NONE
                    && velocityY < velocityX) {
                swiping = true;
                swipingRight = (deltaX > 0);
                Log.d("SwipeListView", "deltaX: " + deltaX + " - swipingRight: " + swipingRight);
                if (opened.get(downPosition)) {
                    listView.onStartClose(downPosition, swipingRight);
                    swipeCurrentAction = SwipeOptions.ACTION_REVEAL;
                } else {
                    if (swipingRight && currentActionRight == SwipeOptions.ACTION_DISMISS) {
                        swipeCurrentAction = SwipeOptions.ACTION_DISMISS;
                    } else if (!swipingRight
                            && currentActionLeft == SwipeOptions.ACTION_DISMISS) {
                        swipeCurrentAction = SwipeOptions.ACTION_DISMISS;
                    } else if (swipingRight
                            && currentActionRight == SwipeOptions.ACTION_CHOICE) {
                        swipeCurrentAction = SwipeOptions.ACTION_CHOICE;
                    } else if (!swipingRight
                            && currentActionLeft == SwipeOptions.ACTION_CHOICE) {
                        swipeCurrentAction = SwipeOptions.ACTION_CHOICE;
                    } else {
                        swipeCurrentAction = SwipeOptions.ACTION_REVEAL;
                    }
                    listView.onStartOpen(downPosition, swipeCurrentAction, swipingRight);
                }
                listView.requestDisallowInterceptTouchEvent(true);
                MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                cancelEvent
                        .setAction(MotionEvent.ACTION_CANCEL
                                | (MotionEventCompat.getActionIndex(motionEvent) << MotionEventCompat.ACTION_POINTER_INDEX_SHIFT));
                listView.onTouchEvent(cancelEvent);
                cancelEvent.recycle();
                if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                    backView.setVisibility(View.GONE);
                }
            }

            if (swiping && downPosition != AdapterView.INVALID_POSITION) {
                if (opened.get(downPosition)) {
                    deltaX += calculateOffset(openedRight.get(downPosition));
                }
                move(deltaX);
                Log.d(LOG_TAG, "onTouch MOVE returns true");
                return true;
            }
            break;
        }
        }
        Log.d(LOG_TAG, "onTouch XXXX returns false");
        return false;
    }

    private void setActionsTo(int action) {
        currentActionRight = action;
        currentActionLeft = action;
    }

    protected void resetOldActions() {
        currentActionRight = opts.swipeActionRight;
        currentActionLeft = opts.swipeActionLeft;
    }

    /**
     * Moves the view
     * 
     * @param deltaX
     *            delta
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void move(float deltaX) {
        listView.onMove(downPosition, deltaX);
        float posX = frontView.getX();
        if (opened.get(downPosition)) {
            posX += calculateOffset(openedRight.get(downPosition));
        }
        if (posX > 0 && !swipingRight) {
            Log.d("SwipeListView", "change to right");
            swipingRight = !swipingRight;
            swipeCurrentAction = opts.swipeActionRight;
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                backView.setVisibility(View.GONE);
            } else {
                backView.setVisibility(View.VISIBLE);
            }
        }
        if (posX < 0 && swipingRight) {
            Log.d("SwipeListView", "change to left");
            swipingRight = !swipingRight;
            swipeCurrentAction = currentActionLeft;
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                backView.setVisibility(View.GONE);
            } else {
                backView.setVisibility(View.VISIBLE);
            }
        }
        if (swipeCurrentAction == SwipeOptions.ACTION_DISMISS) {
            parentView.setTranslationX(deltaX);
            parentView.setAlpha(Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(deltaX) / viewWidth)));
        } else if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
            if ((swipingRight && deltaX > 0 && posX < DISPLACE_CHOICE)
                    || (!swipingRight && deltaX < 0 && posX > -DISPLACE_CHOICE)
                    || (swipingRight && deltaX < DISPLACE_CHOICE)
                    || (!swipingRight && deltaX > -DISPLACE_CHOICE)) {
                frontView.setTranslationX(deltaX);
            }
        } else {
            frontView.setTranslationX(deltaX);
        }
    }

    /**
     * Class that saves pending dismiss data
     */
    private class PendingDismissData implements Comparable<PendingDismissData> {
        private int position;
        private View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    /**
     * Perform dismiss action
     * 
     * @param dismissView
     *            View
     * @param dismissPosition
     *            Position of list
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void performDismiss(final View dismissView, final int dismissPosition,
            boolean doPendingDismiss) {
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(
                opts.animationTime);

        if (doPendingDismiss) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    --dismissAnimationRefCount;
                    if (dismissAnimationRefCount == 0) {
                        removePendingDismisses(originalHeight);
                    }
                }
            });
        }

        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

    protected void resetPendingDismisses() {
        pendingDismisses.clear();
    }

    protected void handlerPendingDismisses(final int originalHeight) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removePendingDismisses(originalHeight);
            }
        }, opts.animationTime + 100);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void removePendingDismisses(int originalHeight) {
        // No active animations, process all pending dismisses.
        // Sort by descending position
        Collections.sort(pendingDismisses);

        int[] dismissPositions = new int[pendingDismisses.size()];
        for (int i = pendingDismisses.size() - 1; i >= 0; i--) {
            dismissPositions[i] = pendingDismisses.get(i).position;
        }
        listView.onDismiss(dismissPositions);

        ViewGroup.LayoutParams lp;
        for (PendingDismissData pendingDismiss : pendingDismisses) {
            // Reset view presentation
            if (pendingDismiss.view != null) {
                pendingDismiss.view.setAlpha(1f);
                pendingDismiss.view.setTranslationX(0);
                lp = pendingDismiss.view.getLayoutParams();
                lp.height = originalHeight;
                pendingDismiss.view.setLayoutParams(lp);
            }
        }

        resetPendingDismisses();

    }

}