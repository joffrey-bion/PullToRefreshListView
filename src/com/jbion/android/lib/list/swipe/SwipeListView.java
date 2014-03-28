package com.jbion.android.lib.list.swipe;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jbion.android.lib.list.pulltoloadmore.PullToLoadListView;
import com.jbion.android.pulltorefresh.R;

/**
 * ListView subclass that provides the swipe functionality. To use it, simply use
 * {@link #setSwipeListViewListener(SwipeListViewListener)} to get notified of some
 * events, or use the getters to have information about the selected items.
 * <p>
 * <b>Important note:</b> You should call {@link #initSwipeState(View, int)} from
 * your adapter's {@link ListAdapter#getView(int, View, android.view.ViewGroup)}
 * method, to initialize its swipe state properly.
 * </p>
 */
public class SwipeListView extends PullToLoadListView {

    private static final String LOG_TAG = SwipeListView.class.getSimpleName();

    /**
     * User options container.
     */
    private final SwipeOptions opts = new SwipeOptions(getContext());
    /**
     * Internal listener for common swipe events
     */
    private SwipeListViewListener swipeListViewListener;
    /**
     * Internal touch listener
     */
    private SwipeListViewTouchListener touchListener;

    // TODO notify this listener in our private scroll listener
    private OnScrollListener userScrollListener;

    private boolean superTouchEventsEnabled = true;

    /**
     * If you create a SwipeListView programmatically you need to specifiy back and
     * front identifier.
     * 
     * @param context
     *            Context
     * @param backViewId
     *            Back view resource identifier
     * @param frontViewId
     *            Front view resource identifier
     */
    public SwipeListView(Context context, int frontViewId, int backViewId) {
        super(context);
        opts.frontViewId = frontViewId;
        opts.backViewId = backViewId;
        init(null);
    }

    /**
     * @see android.widget.ListView#ListView(android.content.Context,
     *      android.util.AttributeSet)
     */
    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * @see android.widget.ListView#ListView(android.content.Context,
     *      android.util.AttributeSet, int)
     */
    public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    /**
     * Init ListView
     * 
     * @param attrs
     *            AttributeSet
     */
    private void init(AttributeSet attrs) {
        // populate options
        if (attrs != null) {
            TypedArray styled = getContext().obtainStyledAttributes(attrs,
                    R.styleable.SwipeListView);
            opts.set(getContext(), styled);
            styled.recycle();
        }

        touchListener = new SwipeListViewTouchListener(this, opts);
        // super.setOnTouchListener(touchListener);
        super.setOnScrollListener(touchListener.makeScrollListener());
    }

    /*
     * BUSINESS LOGIC
     */

    /**
     * @see android.widget.ListView#onInterceptTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean customIntercept = false;
        if (isEnabled()) {
            customIntercept = touchListener.shouldIntercept(ev);
        }
        boolean superIntercept = super.onInterceptTouchEvent(ev);
        // execute super in any case (hence the order)
        if (superIntercept) {
            Log.i(LOG_TAG, "SUPER INTERCEPTS TOUCH EVENTS (scroll)");
        }
        return superIntercept || customIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean res = touchListener != null ? touchListener.onTouch(this, ev) : false;
        // execute super in any case (hence the order)
        return superTouchEventsEnabled && super.onTouchEvent(ev) || res;
    }

    @Override
    protected void onHeaderPullStateChanged(boolean pullingOnHeader, State pullState) {
        if (touchListener != null) {
            touchListener.setSwipeEnabled(!pullingOnHeader);
        }
    }

    /*
     * USER METHODS
     */

    /**
     * Sets the Listener
     * 
     * @param swipeListViewListener
     *            Listener
     */
    public void setSwipeListViewListener(SwipeListViewListener swipeListViewListener) {
        this.swipeListViewListener = swipeListViewListener;
    }

    /**
     * Adjusts the swipe state of the specified view to match the item it represents.
     * This method <b>should be called from getView</b> in the Adapter when
     * initializing a view (either a new or a recycled one).
     * 
     * @param itemView
     *            The item's view to be initialized
     * @param position
     *            The position of the item in the adapter
     */
    public void initSwipeState(View itemView, int position) {
        // the position argument for getView does not take headers into account
        touchListener.initViewSwipeState(itemView, position + getHeaderViewsCount());
    }

    /**
     * Get if item is selected
     * 
     * @param position
     *            position in list
     * @return whether the specified position is currently in a swiped state.
     */
    public boolean isChecked(int position) {
        return touchListener.isChecked(position);
    }

    /**
     * Returns a list of the positions that are currently swiped.
     * 
     * @return a list of the swiped positions.
     */
    public List<Integer> getSelectedPositions() {
        return touchListener.getCheckedPositions();
    }

    /**
     * Returns the number of currently swiped items.
     * 
     * @return the number of currently swiped items.
     */
    public int getCountSelected() {
        return touchListener.getCountChecked();
    }

    /**
     * Close all opened items
     */
    public void closeOpenedItems() {
        touchListener.unswipeAllItems();
    }

    /**
     * Unselected choice state in item
     */
    public void unselectedChoiceStates() {
        touchListener.uncheckAllItems();
    }

    /**
     * Dismiss item
     * 
     * @param position
     *            Position that you want open
     */
    public void dismiss(int position) {
        int height = touchListener.dismiss(position);
        if (height > 0) {
            touchListener.handlerPendingDismisses(height);
        } else {
            int[] dismissPositions = new int[1];
            dismissPositions[0] = position;
            onDismiss(dismissPositions);
            touchListener.resetPendingDismisses();
        }
    }

    /**
     * Dismiss items selected
     */
    public void dismissSelected() {
        List<Integer> list = touchListener.getCheckedPositions();
        int[] dismissPositions = new int[list.size()];
        int height = 0;
        for (int i = 0; i < list.size(); i++) {
            int position = list.get(i);
            dismissPositions[i] = position;
            int auxHeight = touchListener.dismiss(position);
            if (auxHeight > 0) {
                height = auxHeight;
            }
        }
        if (height > 0) {
            touchListener.handlerPendingDismisses(height);
        } else {
            onDismiss(dismissPositions);
            touchListener.resetPendingDismisses();
        }
        touchListener.resetOldActions();
    }

    /**
     * Open ListView's item
     * 
     * @param position
     *            Position that you want open
     */
    public void swipe(int position) {
        touchListener.swipe(position);
    }

    /**
     * Close ListView's item
     * 
     * @param position
     *            Position that you want open
     */
    public void unswipe(int position) {
        touchListener.unswipe(position);
    }

    /*
     * SUPER SETTERS INTERCEPTION
     */

    /**
     * @see android.widget.ListView#setAdapter(android.widget.ListAdapter)
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        touchListener.resetItems();
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onListChanged();
                touchListener.resetItems();
            }
        });
    }

    @Override
    public void setOnScrollListener(OnScrollListener listener) {
        userScrollListener = listener;
    }

    /*
     * OPTIONS SETTERS
     */

    public void setSwipeEnabled(boolean enabled) {
        if (touchListener != null) {
            touchListener.setSwipeEnabled(enabled);
        }
    }

    /**
     * Sets the swipe swipeMode
     * 
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        opts.swipeMode = swipeMode;
    }

    /**
     * Return action on left
     * 
     * @return Action
     */
    public int getSwipeActionLeft() {
        return opts.swipeActionLeft;
    }

    /**
     * Set action on left
     * 
     * @param swipeActionLeft
     *            Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        opts.swipeActionLeft = swipeActionLeft;
    }

    /**
     * Return action on right
     * 
     * @return Action
     */
    public int getSwipeActionRight() {
        return opts.swipeActionRight;
    }

    /**
     * Set action on right
     * 
     * @param swipeActionRight
     *            Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        opts.swipeActionRight = swipeActionRight;
    }

    /**
     * Defines how to consider the given offsets.
     * 
     * @param swipeOffsetType
     */
    public void setOffsetType(int swipeOffsetType) {
        opts.swipeOffsetType = swipeOffsetType;
    }

    /**
     * Set the left offset
     * 
     * @param leftOffset
     *            Offset
     */
    public void setLeftOffset(float leftOffset) {
        opts.swipeOffsetLeft = leftOffset;
    }

    /**
     * Sets the right offset
     * 
     * @param rightOffset
     *            Offset
     */
    public void setRightOffset(float rightOffset) {
        opts.swipeOffsetRight = rightOffset;
    }

    /**
     * Set if the user can open an item with long press on cell
     * 
     * @param openOnLongClick
     */
    public void setOpenOnLongClick(boolean openOnLongClick) {
        opts.openOnLongClick = openOnLongClick;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     * 
     * @param multipleSelectEnabled
     */
    public void setMultipleSelectEnabled(boolean multipleSelectEnabled) {
        opts.multipleSelectEnabled = multipleSelectEnabled;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     * 
     * @param closeAllItemsOnScroll
     */
    public void setCloseAllItemsOnScroll(boolean closeAllItemsOnScroll) {
        opts.closeAllItemsOnScroll = closeAllItemsOnScroll;
    }

    /**
     * Sets animation time when the user drops the cell
     * 
     * @param animationTime
     *            milliseconds
     */
    public void setAnimationTime(long animationTime) {
        if (animationTime > 0) {
            opts.animationTime = animationTime;
        }
    }

    /*
     * LISTENER CALLBACKS
     */

    /**
     * Notifies onDismiss
     * 
     * @param reverseSortedPositions
     *            All dismissed positions
     */
    protected void onDismiss(int[] reverseSortedPositions) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onDismiss(reverseSortedPositions);
        }
    }

    /**
     * Notifies onClickFrontView
     * 
     * @param position
     *            item clicked
     */
    protected void onClickFrontView(int position) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onClickFrontView(position);
        }
    }

    /**
     * Notifies onClickBackView
     * 
     * @param position
     *            back item clicked
     */
    protected void onClickBackView(int position) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onClickBackView(position);
        }
    }

    /**
     * Notifies onOpened
     * 
     * @param position
     *            Item opened
     * @param toRight
     *            If should be opened toward the right
     */
    protected void onSwiped(int position, boolean toRight) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onSwiped(position, toRight);
        }
    }

    /**
     * Notifies onClosed
     * 
     * @param position
     *            Item closed
     * @param fromRight
     *            If open from right
     */
    protected void onUnswiped(int position, boolean fromRight) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onUnswiped(position, fromRight);
        }
    }

    /**
     * Notifies onChoiceChanged
     * 
     * @param position
     *            position that choice
     * @param selected
     *            if item is selected or not
     */
    protected void onChoiceChanged(int position, boolean selected) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onChoiceChanged(position, selected);
        }
    }

    /**
     * User start choice items
     */
    protected void onChoiceStarted() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onChoiceStarted();
        }
    }

    /**
     * User end choice items
     */
    protected void onChoiceEnded() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onChoiceEnded();
        }
    }

    /**
     * Notifies onListChanged
     */
    protected void onListChanged() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onListChanged();
        }
    }

    /**
     * Notifies onMove
     * 
     * @param position
     *            Item moving
     * @param x
     *            Current position
     */
    protected void onMove(int position, float x) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onMove(position, x);
        }
    }

    public void disableSuperTouchEvent(boolean disable) {
        this.superTouchEventsEnabled = !disable;
    }
}