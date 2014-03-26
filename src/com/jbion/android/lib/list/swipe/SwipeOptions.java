package com.jbion.android.lib.list.swipe;

import android.content.Context;
import android.content.res.TypedArray;

import com.jbion.android.pulltorefresh.R;

class SwipeOptions {

    /**
     * Used when user want change swipe list swipeMode on some rows
     */
    public final static int SWIPE_MODE_DEFAULT = -1;

    /**
     * Disables all swipes
     */
    public final static int SWIPE_MODE_NONE = 0;

    /**
     * Enables both left and right swipe
     */
    public final static int SWIPE_MODE_BOTH = 1;

    /**
     * Enables right swipe
     */
    public final static int SWIPE_MODE_RIGHT = 2;

    /**
     * Enables left swipe
     */
    public final static int SWIPE_MODE_LEFT = 3;

    /**
     * Binds the swipe gesture to reveal a view behind the row (Drawer style)
     */
    public final static int ACTION_REVEAL = 0;

    /**
     * Dismisses the cell when swiped over
     */
    public final static int ACTION_DISMISS = 1;

    /**
     * Marks the cell as checked when swiped and release
     */
    public final static int ACTION_CHOICE = 2;

    /**
     * No action when swiped
     */
    public final static int ACTION_NONE = 3;

    /**
     * Defines the offset as the remaining part of the view in the screen after a
     * swipe.
     */
    public final static int OFFSET_TYPE_REMAINING = 0;

    /**
     * Defines the offset as the traveled distance from initial position.
     */
    public final static int OFFSET_TYPE_TRAVELED = 1;

    /**
     * Default ids for front view
     */
    public final static String SWIPE_DEFAULT_FRONT_VIEW = "swipelist_frontview";

    /**
     * Default id for back view
     */
    public final static String SWIPE_DEFAULT_BACK_VIEW = "swipelist_backview";

    private final int defaultAnimationTime;

    int frontViewId = 0;
    int backViewId = 0;

    int swipeMode = SWIPE_MODE_BOTH;
    int swipeActionLeft = ACTION_REVEAL;
    int swipeActionRight = ACTION_REVEAL;

    int swipeOffsetType = 0;
    float swipeOffsetLeft = 0;
    float swipeOffsetRight = 0;

    boolean openOnLongClick = true;
    boolean multipleSelectEnabled = true;
    boolean closeAllItemsOnScroll = true;

    long animationTime = 0;
    int drawableChecked = 0;
    int drawableUnchecked = 0;

    public SwipeOptions(Context ctx) {
        defaultAnimationTime = ctx.getResources()
                .getInteger(android.R.integer.config_shortAnimTime);
        animationTime = defaultAnimationTime;
    }

    public void set(Context ctx, TypedArray styled) {
        frontViewId = styled.getResourceId(R.styleable.SwipeListView_itemFrontViewId, 0);
        backViewId = styled.getResourceId(R.styleable.SwipeListView_itemBackViewId, 0);

        swipeMode = styled.getInt(R.styleable.SwipeListView_swipeMode, SWIPE_MODE_BOTH);
        swipeActionLeft = styled.getInt(R.styleable.SwipeListView_swipeActionLeft, ACTION_REVEAL);
        swipeActionRight = styled.getInt(R.styleable.SwipeListView_swipeActionRight, ACTION_REVEAL);

        swipeOffsetType = styled.getInt(R.styleable.SwipeListView_swipeOffsetType,
                OFFSET_TYPE_REMAINING);
        swipeOffsetLeft = styled.getDimension(R.styleable.SwipeListView_swipeOffsetLeft, 0);
        swipeOffsetRight = styled.getDimension(R.styleable.SwipeListView_swipeOffsetRight, 0);

        openOnLongClick = styled.getBoolean(R.styleable.SwipeListView_openOnLongPress, true);
        multipleSelectEnabled = styled.getBoolean(R.styleable.SwipeListView_multipleSelectEnabled,
                true);
        closeAllItemsOnScroll = styled.getBoolean(R.styleable.SwipeListView_closeAllItemsOnScroll,
                true);

        animationTime = styled.getInteger(R.styleable.SwipeListView_animationTime,
                defaultAnimationTime);
        drawableChecked = styled.getResourceId(R.styleable.SwipeListView_swipeDrawableChecked, 0);
        drawableUnchecked = styled.getResourceId(R.styleable.SwipeListView_swipeDrawableUnchecked,
                0);

        if (frontViewId == 0 || backViewId == 0) {
            // try default views ids
            frontViewId = ctx.getResources().getIdentifier(SwipeOptions.SWIPE_DEFAULT_FRONT_VIEW,
                    "id", ctx.getPackageName());
            backViewId = ctx.getResources().getIdentifier(SwipeOptions.SWIPE_DEFAULT_BACK_VIEW,
                    "id", ctx.getPackageName());

            if (frontViewId == 0 || backViewId == 0) {
                throw new RuntimeException(
                        String.format(
                                "Missing attribute frontViewId or backViewId. You can add these attributes or use '%s' and '%s' identifiers",
                                SwipeOptions.SWIPE_DEFAULT_FRONT_VIEW,
                                SwipeOptions.SWIPE_DEFAULT_BACK_VIEW));
            }
        }
    }
}