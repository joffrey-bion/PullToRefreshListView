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
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

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
    private final static int STATE_REST = 0;
    /**
     * State scrolling x position
     */
    private final static int STATE_SCROLLING_X = 1;
    /**
     * State scrolling y position
     */
    private final static int STATE_SCROLLING_Y = 2;

    private final SwipeListView listView;
    private final SwipeOptions opts;

    // Cached ViewConfiguration and system-wide constant values
    private final int slop;
    private final int pageSlop;
    private final int minFlingVelocity;
    private final int maxFlingVelocity;
    private final long configShortAnimationTime;

    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

    private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();
    private int dismissAnimationRefCount = 0;

    private boolean paused;
    private List<Boolean> swiped = new ArrayList<Boolean>();
    private List<Boolean> swipedToRight = new ArrayList<Boolean>();
    private List<Boolean> checked = new ArrayList<Boolean>();

    private final Item movingItem = new Item();
    private final Motion currentMotion = new Motion();

    private int swipeCurrentAction;
    private int currentActionLeft;
    private int currentActionRight;

    private final Rect rect = new Rect();

    private class Motion {
        int scrollState = STATE_REST;
        float downX;
        float lastX;
        float lastY;

        boolean pullingX;
        boolean toRight;
        VelocityTracker tracker;

        public void reset() {
            currentMotion.downX = 0;
            currentMotion.pullingX = false;
            currentMotion.tracker.recycle();
            currentMotion.tracker = null;
        }
    }

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
        pageSlop = vc.getScaledPagingTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        configShortAnimationTime = ctx.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        if (opts.animationTime <= 0) {
            opts.animationTime = configShortAnimationTime;
        }
        currentActionLeft = opts.swipeActionLeft;
        currentActionRight = opts.swipeActionRight;
        swipeCurrentAction = SwipeOptions.ACTION_NONE;
    }

    /**
     * Check if swipe is enabled
     * 
     * @return {@code true} if swipe is enabled
     */
    protected boolean isSwipeEnabled() {
        return !paused && opts.swipeMode != SwipeOptions.SWIPE_MODE_NONE;
    }

    /**
     * Adds new items when adapter is modified
     */
    public void resetItems() {
        if (listView.getAdapter() != null) {
            swiped.clear();
            swipedToRight.clear();
            checked.clear();
            for (int i = 0; i <= listView.getAdapter().getCount(); i++) {
                swiped.add(false);
                swipedToRight.add(false);
                checked.add(false);
            }
        }
    }

    /**
     * Swap choice state in item
     * 
     * @param position
     *            position of list
     */
    private void swapCheckedState(int position) {
        Log.i(LOG_TAG, "Swapping checked state for position " + position);
        int lastCount = getCountSelected();
        boolean lastChecked = checked.get(position);
        checked.set(position, !lastChecked);
        int count = lastChecked ? lastCount - 1 : lastCount + 1;
        if (lastCount == 0 && count == 1) {
            listView.onChoiceStarted();
            unswipeAllItems();
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
        reloadViewState(movingItem.frontView, position);
    }

    /**
     * Unselected choice state in item
     */
    protected void unselectedChoiceStates() {
        int start = listView.getFirstVisiblePosition();
        int end = listView.getLastVisiblePosition();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i) && i >= start && i <= end) {
                reloadViewState(listView.getChildAt(i - start).findViewById(opts.frontViewId), i);
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
     * @param convertView
     *            the front view to reload
     * @param position
     *            position in list
     */
    protected void reloadViewState(View convertView, int position) {
        View frontView = convertView.findViewById(opts.frontViewId);
        if (isChecked(position)) {
            if (opts.drawableChecked > 0)
                frontView.setBackgroundResource(opts.drawableChecked);
        } else {
            if (opts.drawableUnchecked > 0)
                frontView.setBackgroundResource(opts.drawableUnchecked);
        }
        if (swiped.get(position)) {
            Log.d(LOG_TAG, "resetting View " + position + " to open position");
            setTranslationX(frontView, calculateSwipedOffset(swipedToRight.get(position)));
        } else {
            Log.d(LOG_TAG, "resetting View " + position + " to closed position");
            setTranslationX(frontView, 0);
        }
    }

    /**
     * Get if item is selected
     * 
     * @param position
     *            position in list
     * @return {@code true} if item is selected
     */
    protected boolean isChecked(int position) {
        return position < checked.size() && checked.get(position);
    }

    /**
     * Count selected
     * 
     * @return the number of swiped items
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
     * @return a list of the swiped positions
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
     * Close all swiped items
     */
    void unswipeAllItems() {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        // animate all visible closing items
        for (int i = first; i <= last; i++) {
            closeAnimate(listView.getChildAt(i - first).findViewById(opts.frontViewId), i);
        }
        // close all items
        for (int i = 0; i < swiped.size(); i++) {
            swiped.set(i, false);
        }
    }

    /**
     * Open item
     * 
     * @param position
     *            Position of list
     */
    protected void swipe(int position) {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        if (position >= first && position <= last) {
            // the affected view is visible
            openAnimate(listView.getChildAt(position - first).findViewById(opts.frontViewId),
                    position);
        } else {
            swiped.set(position, true);
        }
    }

    /**
     * Close item
     * 
     * @param position
     *            Position of list
     */
    protected void unswipe(int position) {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        if (position >= first && position <= last) {
            // the affected view is visible
            closeAnimate(listView.getChildAt(position - first).findViewById(opts.frontViewId),
                    position);
        } else {
            swiped.set(position, false);
        }
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
        if (!swiped.get(position)) {
            generateRevealAnimate2(view, true, false, position);
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
        if (swiped.get(position)) {
            generateRevealAnimate2(view, true, swipedToRight.get(position), position);
        }
    }

    /**
     * Create animation
     * 
     * @param view
     *            affected view
     * @param validMove
     *            If state should change. If "false" returns to the original position
     * @param toRight
     *            If swap is true, this parameter tells if move is to the right or
     *            left
     * @param position
     *            Position of list
     */
    private void generateAnimate(final View view, final boolean validMove, final boolean toRight,
            final int position) {
        Log.d("SwipeListView", "swap: " + validMove + " - swapRight: " + toRight + " - position: "
                + position);
        int action = swiped.get(position) ? SwipeOptions.ACTION_REVEAL
                : toRight ? currentActionRight : currentActionLeft;
        if (action == SwipeOptions.ACTION_REVEAL) {
            generateRevealAnimate2(view, validMove, toRight, position);
        }
        if (action == SwipeOptions.ACTION_DISMISS) {
            generateDismissAnimate(movingItem.view, validMove, toRight, position);
        }
        if (action == SwipeOptions.ACTION_CHOICE) {
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
    private void generateChoiceAnimate(final View view, final int position) {
        animate(view, 0, opts.animationTime, new Runnable() {
            @Override
            public void run() {
                currentMotion.scrollState = STATE_REST;
                releaseMovingItem();
            }
        });
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
    private void generateDismissAnimate(final View view, final boolean swap,
            final boolean swapRight, final int position) {
        int moveTo = 0;
        if (swiped.get(position)) {
            if (!swap) {
                moveTo = calculateSwipedOffset(swipedToRight.get(position));
            }
        } else {
            if (swap) {
                moveTo = calculateSwipedOffset(swapRight);
            }
        }

        int alpha = 1;
        if (swap) {
            ++dismissAnimationRefCount;
            alpha = 0;
        }

        animate(view, moveTo, alpha, opts.animationTime, new Runnable() {
            @Override
            public void run() {
                if (swap) {
                    unswipeAllItems();
                    performDismiss(view, position, true);
                }
                releaseMovingItem();
            }
        });

    }

    /**
     * Create reveal animation
     * 
     * @param view
     *            affected view
     * @param changeState
     *            If will change state. If "false" returns to the original position
     * @param toRight
     *            If swap is true, this parameter tells if movement is toward right
     *            or left
     * @param position
     *            list position
     */
    private void generateRevealAnimate2(final View view, final boolean changeState,
            final boolean toRight, final int position) {
        final boolean isOpen = swiped.get(position);

        if (!opts.multipleSelectEnabled && changeState && !isOpen) {
            unswipeAllItems();
        }

        int moveTo = changeState ^ isOpen ? calculateSwipedOffset(toRight) : 0;

        animate(view, moveTo, opts.animationTime, new Runnable() {
            @Override
            public void run() {
                currentMotion.scrollState = STATE_REST;
                if (changeState && !isOpen) {
                    swiped.set(position, true);
                    swipedToRight.set(position, toRight);
                    listView.onSwiped(position, toRight);
                } else if (changeState && isOpen) {
                    swiped.set(position, false);
                    listView.onUnswiped(position, !toRight);
                }
                releaseMovingItem();
            }
        });
    }

    /**
     * Calculates the X distance to move the view to its open position.
     * 
     * @param swapRight
     *            Whether opening to the right or left.
     * @return the X position of the open view relatively to the closed view.
     */
    private int calculateSwipedOffset(boolean swapRight) {
        if (opts.swipeOffsetType == SwipeOptions.OFFSET_TYPE_TRAVELED) {
            return swapRight ? (int) (opts.swipeOffsetLeft) : (int) (-opts.swipeOffsetRight);
        } else {
            return swapRight ? (int) (viewWidth - opts.swipeOffsetRight)
                    : (int) (-viewWidth + opts.swipeOffsetLeft);
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

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING) {
                    Log.i(LOG_TAG, "ScrollStateChanged: scrolling/flinging now");
                    if (opts.closeAllItemsOnScroll) {
                        Log.w(LOG_TAG, "ScrollStateChanged: close everything!");
                        unswipeAllItems();
                    }
                } else {
                    Log.i(LOG_TAG, "ScrollStateChanged: no more scrolling");
                    movingItem.position = AdapterView.INVALID_POSITION;
                    currentMotion.scrollState = STATE_REST;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                /*
                 * Log.d(LOG_TAG, "first=" + firstVisibleItem + " last=" +
                 * (firstVisibleItem + visibleItemCount - 1));
                 */
            }
        };
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
        final int xDiff = (int) Math.abs(x - currentMotion.lastX);
        final int yDiff = (int) Math.abs(y - currentMotion.lastY);

        boolean xMoved = xDiff > pageSlop;
        boolean yMoved = yDiff > pageSlop;

        if (xMoved || yMoved) {
            if (xDiff > yDiff) {
                currentMotion.scrollState = STATE_SCROLLING_X;
                Log.d(LOG_TAG, "update direction to X (xDiff=" + xDiff + ", yDiff=" + yDiff + ")");
            } else {
                currentMotion.scrollState = STATE_SCROLLING_Y;
                Log.d(LOG_TAG, "update direction to Y (xDiff=" + xDiff + ", yDiff=" + yDiff + ")");
            }
            currentMotion.lastX = x;
            currentMotion.lastY = y;
        }
    }

    /**
     * Initializes {@link #movingItem} fields with the data from the touched element
     * in the list. The touched element is determined based on the specified
     * {@link MotionEvent}'s coordinates.
     * 
     * @param ev
     *            The touch event to use to find the touched item.
     * @return {@code true} if an item was indeed found and initialized,
     *         {@code false} otherwise.
     */
    private boolean initMovingItem(MotionEvent ev) {
        View item;
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        // find the item located at (x,y)
        for (int i = 0; i < listView.getChildCount(); i++) {
            item = listView.getChildAt(i);
            item.getHitRect(rect);
            if (!rect.contains(x, y)) {
                continue; // not this one, keep searching
            }
            int touchedItemPosition = listView.getPositionForView(item);
            // don't allow pullingX if this is on the header or footer or
            // IGNORE_ITEM_VIEW_TYPE or disabled item
            ListAdapter adapter = listView.getAdapter();
            if (adapter.isEnabled(touchedItemPosition)
                    && adapter.getItemViewType(touchedItemPosition) != AdapterView.ITEM_VIEW_TYPE_IGNORE) {
                movingItem.view = item;
                movingItem.position = touchedItemPosition;
                movingItem.setFrontView(item.findViewById(opts.frontViewId));
                movingItem.frontView.setClickable(!swiped.get(movingItem.position));
                movingItem.frontView.setLongClickable(!swiped.get(movingItem.position));

                if (opts.backViewId > 0) {
                    movingItem.setBackView(item.findViewById(opts.backViewId));
                }
                return true;
            } else {
                movingItem.position = AdapterView.INVALID_POSITION;
                return false;
            }
        }
        movingItem.position = AdapterView.INVALID_POSITION;
        return false;
    }

    private void releaseMovingItem() {
        if (movingItem.position != AdapterView.INVALID_POSITION) {
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                movingItem.backView.setVisibility(View.VISIBLE);
            }
            movingItem.frontView.setClickable(swiped.get(movingItem.position));
            movingItem.frontView.setLongClickable(swiped.get(movingItem.position));
            movingItem.frontView = null;
            movingItem.backView = null;
            movingItem.position = AdapterView.INVALID_POSITION;
        }
    }

    private void initCurrentMotion(MotionEvent motionEvent) {
        swipeCurrentAction = SwipeOptions.ACTION_NONE;
        currentMotion.pullingX = false;
        currentMotion.downX = motionEvent.getX();
        boolean itemLoaded = initMovingItem(motionEvent);
        if (itemLoaded) {
            currentMotion.tracker = VelocityTracker.obtain();
            currentMotion.tracker.addMovement(motionEvent);
        }
    }

    private boolean isCurrentMotionAValidXFling() {
        float velocityX = Math.abs(currentMotion.tracker.getXVelocity());
        float velocityY = Math.abs(currentMotion.tracker.getYVelocity());
        if (velocityX < minFlingVelocity || velocityX > maxFlingVelocity
                || velocityY * 2 > velocityX) {
            return false; // not an X-fling
        }

        boolean toRight = currentMotion.tracker.getXVelocity() > 0;

        if (swiped.get(movingItem.position)) {
            if (swipedToRight.get(movingItem.position) && toRight) {
                // swiped to right, flinging right
                return false;
            }
            if (!swipedToRight.get(movingItem.position) && !toRight) {
                // swiped to left, flinging left
                return false;
            }
        } else {
            if (opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT && toRight) {
                // opens to left, flinging right
                return false;
            }
            if (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT && !toRight) {
                // opens to right, flinging left
                return false;
            }
        }
        return true;
    }

    private void updateCurrentAction() {
        if (!currentMotion.pullingX) {
            swipeCurrentAction = SwipeOptions.ACTION_NONE;
        }
        if (swiped.get(movingItem.position)) {
            swipeCurrentAction = SwipeOptions.ACTION_REVEAL;
        } else {
            if (currentMotion.toRight) {
                swipeCurrentAction = currentActionRight;
            } else {
                swipeCurrentAction = currentActionLeft;
            }
        }
        // update back view visibility depending on the new action
        if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
            movingItem.backView.setVisibility(View.GONE);
        } else {
            movingItem.backView.setVisibility(View.VISIBLE);
        }
    }

    public boolean shouldIntercept(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        if (isSwipeEnabled()) {
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                initCurrentMotion(ev);
                //$FALL-THROUGH$
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentMotion.scrollState = STATE_REST;
                return false;
            case MotionEvent.ACTION_MOVE:
                updateScrollDirection(x, y);
                Log.d(LOG_TAG, "Intercept MOVE " + (currentMotion.scrollState == STATE_SCROLLING_X)
                        + " (state=" + currentMotion.scrollState + ")");
                return currentMotion.scrollState == STATE_SCROLLING_X;
            default:
                break;
            }
        }
        return false;
    }

    /**
     * @see android.view.View.OnTouchListener#onTouch(android.view.View,
     *      android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        if (!isSwipeEnabled()) {
            Log.v(LOG_TAG, "onTouch XXXX returns false (swipe disabled)");
            return false;
        }

        if (viewWidth < 2) {
            viewWidth = listView.getWidth();
        }

        switch (MotionEventCompat.getActionMasked(ev)) {
        case MotionEvent.ACTION_DOWN:
            if (paused && movingItem.position != AdapterView.INVALID_POSITION) {
                Log.v(LOG_TAG, "onTouch DOWN returns false");
                // feature disabled for now
                return false;
            }
            initCurrentMotion(ev);
            Log.d(LOG_TAG, "onTouch DOWN returns true");
            return true;

        case MotionEvent.ACTION_MOVE: {
            if (paused || movingItem.position == AdapterView.INVALID_POSITION) {
                Log.v(LOG_TAG, "onTouch MOVE returns false");
                return false;
            }
            currentMotion.tracker.addMovement(ev);

            // finger currently traveled distance
            float deltaX = ev.getX() - currentMotion.downX;

            currentMotion.toRight = deltaX > 0;

            boolean okToMoveThere = isAllowedDirection(currentMotion.toRight, movingItem.position);
            if (!okToMoveThere) {
                Log.w(LOG_TAG, "Trying to pull item " + movingItem.position + " the wrong way");
                currentMotion.downX = ev.getX(); // to restart pulling from here
                deltaX = 0;
            }

            // update pulling state if needed
            if (!currentMotion.pullingX) {
                currentMotion.tracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(currentMotion.tracker.getXVelocity());
                float velocityY = Math.abs(currentMotion.tracker.getYVelocity());

                if (Math.abs(deltaX) > slop && velocityY < velocityX) {
                    currentMotion.pullingX = true;
                    Log.i(LOG_TAG, "Start pulling item " + movingItem.position + " towards "
                            + (currentMotion.toRight ? "right" : "left"));
                    // TODO shouldn't be here
                    updateCurrentAction();
                }
            }

            // update front view position
            if (currentMotion.pullingX) {
                float targetX = deltaX;
                if (swiped.get(movingItem.position)) {
                    targetX += calculateSwipedOffset(swipedToRight.get(movingItem.position));
                }
                Log.v(LOG_TAG, "moving item " + movingItem.position + " to x=" + deltaX);
                updateFrontViewXPosition(targetX);
                Log.v(LOG_TAG, "onTouch MOVE returns true");
                return true;
            }
            Log.v(LOG_TAG, "onTouch MOVE returns false");
            return false;
        }

        case MotionEvent.ACTION_UP: {
            if (!currentMotion.pullingX || movingItem.position == AdapterView.INVALID_POSITION) {
                Log.v(LOG_TAG, "onTouch UP returns false");
                // we were not following this event
                return false;
            }

            float deltaX = ev.getX() - currentMotion.downX;
            currentMotion.tracker.addMovement(ev);
            currentMotion.tracker.computeCurrentVelocity(1000);

            boolean toRight = deltaX > 0;
            boolean validSwipe = Math.abs(deltaX) > (viewWidth / 2);
            boolean validFling = isCurrentMotionAValidXFling();

            if (validSwipe) {
                Log.i(LOG_TAG, "Swipe item " + movingItem.position + " to "
                        + (toRight ? "right" : "left") + "!");
            } else if (validFling) {
                // may be different from the one calculated with deltaX
                // (if the item is pulled one way and flung towards the other side)
                toRight = currentMotion.tracker.getXVelocity() > 0;
                Log.i(LOG_TAG, "Fling item " + movingItem.position + " to "
                        + (toRight ? "right" : "left") + "!");
            } else {
                Log.i(LOG_TAG, "Release item " + movingItem.position);
            }
            generateAnimate(movingItem.frontView, validFling || validSwipe, toRight,
                    movingItem.position);
            if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
                swapCheckedState(movingItem.position);
            }

            currentMotion.reset();
            Log.v(LOG_TAG, "onTouch UP returns true");
            return true;
        }
        default:
            return false;
        }
    }

    /**
     * Returns whether the specified movement is valid for the specified item,
     * depending on its state.
     * 
     * @param toRight
     *            The desired moving direction, {@code true} means towards right.
     * @param position
     *            The position of the item to check.
     * @return {@code true} if the item at the specified {@code position} is allowed
     *         to move in the specified direction.
     */
    private boolean isAllowedDirection(boolean toRight, int position) {
        if (opts.swipeMode == SwipeOptions.SWIPE_MODE_NONE) {
            // no swipe
            return false;
        } else if (opts.swipeMode != SwipeOptions.SWIPE_MODE_BOTH) {
            if (swiped.get(position)) {
                if ((opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT && !toRight)
                        || (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT && toRight)) {
                    // trying to close the element the wrong way
                    return false;
                }
            } else {
                if ((opts.swipeMode == SwipeOptions.SWIPE_MODE_LEFT && toRight)
                        || (opts.swipeMode == SwipeOptions.SWIPE_MODE_RIGHT && !toRight)) {
                    // trying to open the element the wrong way
                    return false;
                }
            }
        } else {
            // always a swipe in BOTH mode
        }
        return true;
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
     * @param targetX
     *            delta
     */
    public void updateFrontViewXPosition(float targetX) {
        if (swipeCurrentAction == SwipeOptions.ACTION_DISMISS) {
            setTranslationX(movingItem.view, targetX);
            setAlpha(movingItem.view,
                    Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(targetX) / viewWidth)));
        } else if (swipeCurrentAction == SwipeOptions.ACTION_CHOICE) {
            float posX = getX(movingItem.frontView);
            if (swiped.get(movingItem.position)) {
                posX -= calculateSwipedOffset(swipedToRight.get(movingItem.position));
            }
            if ((currentMotion.toRight && targetX > 0 && posX < DISPLACE_CHOICE)
                    || (!currentMotion.toRight && targetX < 0 && posX > -DISPLACE_CHOICE)
                    || (currentMotion.toRight && targetX < DISPLACE_CHOICE)
                    || (!currentMotion.toRight && targetX > -DISPLACE_CHOICE)) {
                setTranslationX(movingItem.frontView, targetX);
            }
        } else {
            setTranslationX(movingItem.frontView, targetX);
        }
        listView.onMove(movingItem.position, targetX);
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
                setAlpha(pendingDismiss.view, 1f);
                setTranslationX(pendingDismiss.view, 0);
                lp = pendingDismiss.view.getLayoutParams();
                lp.height = originalHeight;
                pendingDismiss.view.setLayoutParams(lp);
            }
        }
        resetPendingDismisses();
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

    private class Item {
        public View view;
        public View frontView;
        public View backView;
        public int position;

        /**
         * Sets current item's front view
         * 
         * @param frontView
         *            Front view
         */
        public void setFrontView(View frontView) {
            this.frontView = frontView;
            this.frontView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listView.onClickFrontView(movingItem.position);
                }
            });
            if (opts.openOnLongClick) {
                movingItem.frontView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        swipe(movingItem.position);
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
        public void setBackView(View backView) {
            this.backView = backView;
            this.backView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listView.onClickBackView(movingItem.position);
                }
            });
        }
    }

    /*
     * POSITIONING/ANIMATION LOW-LEVEL METHODS
     */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static float getX(View v) {
        return v.getX();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setTranslationX(View v, float translationX) {
        v.setTranslationX(translationX);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setAlpha(View v, float alpha) {
        v.setAlpha(alpha);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void animate(View v, float translationX, long animationTime,
            final Runnable animationEndCallback) {
        v.animate().translationX(translationX).setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animationEndCallback.run();
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void animate(View v, float translationX, float alpha, long animationTime,
            final Runnable animationEndCallback) {
        v.animate().translationX(translationX).alpha(alpha).setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animationEndCallback.run();
                    }
                });
    }

}