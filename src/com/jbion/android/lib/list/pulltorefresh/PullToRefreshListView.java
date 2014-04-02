package com.jbion.android.lib.list.pulltorefresh;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.jbion.android.pulltorefresh.R;

/**
 * A customizable Android {@code ListView} implementation that has 'Pull to Refresh'
 * functionality. This {@code ListView} can be used in place of the normal Android
 * {@link android.widget.ListView} class.
 * <p>
 * Users of this class should implement {@link OnPullToRefreshListener} and call
 * {@link #setOnPullToRefreshListener(OnPullToRefreshListener)} to get notified on
 * refresh events. The using class should call {@link #onRefreshComplete()} when
 * refreshing is finished.
 * </p>
 * <p>
 * The using class can call {@link #setRefreshing()} to set the state explicitly to
 * refreshing. This is useful when you want to show the spinner and 'Refreshing' text
 * when the refresh was not triggered by 'Pull to Refresh', for example on start.
 * </p>
 * <p>
 * If the using class wants to fully control the refreshing state UI, it may use
 * {@link #setRefreshingHeaderEnabled(boolean)} to disable the built-in refreshing
 * view. It will still have to call {@link #onRefreshComplete()} when refreshing is
 * finished, for state consistency.
 * </p>
 */
public class PullToRefreshListView extends ListView {

    private static final String LOG_TAG = PullToRefreshListView.class.getSimpleName();
    
    /**
     * Interface to implement when you want to get notified of 'pull to refresh'
     * events. Call setOnRefreshListener(..) to activate an OnPullToRefreshListener.
     */
    public static interface OnPullToRefreshListener {

        /**
         * Called when a refresh is requested.
         */
        public void onPullToRefresh();
    }

    private static final float PULL_RESISTANCE = 1.7f;

    /** Duration of the animation to rotate the arrow in the header (pull/release). */
    private static final int ROTATE_ARROW_ANIMATION_DURATION = 250;
    /** Duration of the animation to send the header back to the top (when released). */
    private static final int BOUNCE_ANIMATION_DURATION = 500;

    /**
     * The current 0 value disables overshoot when bouncing the header back. It could
     * be set to another value to see an overshoot spring effect.
     */
    private static final float BOUNCE_OVERSHOOT_TENSION = 0f;

    private static final int HEADER_POSITION = 0;
    private static final int FIRST_ITEM_POSITION = 1;

    /**
     * Possible internal states for this {@link PullToRefreshListView}.
     */
    protected static enum State {
        PULL_TO_REFRESH,
        RELEASE_TO_REFRESH,
        REFRESHING
    }

    /*
     * User settings
     */

    /** Whether the pull-to-refresh functionality is enabled. */
    protected boolean ptrEnabled = true;

    private OnItemClickListener userOnItemClickListener;
    private OnItemLongClickListener userOnItemLongClickListener;

    private OnPullToRefreshListener onRefreshListener;

    private String pullToRefreshText = "Pull to refresh";
    private String releaseToRefreshText = "Release to refresh";
    private String refreshingText = "Loading…";
    private String lastUpdatedText = "Updated: %1$s";

    private java.text.DateFormat lastUpdatedDateFormat = SimpleDateFormat.getDateTimeInstance();

    private boolean lockScrollWhileRefreshing = false;
    private boolean showLastUpdatedText = false;
    private boolean refreshingHeaderEnabled = true;
    private int pullThreshold = 0;

    /*
     * Header view
     */

    private View headerContainer;
    private View header;
    private ImageView image;
    private View spinner;
    private TextView text;
    private TextView lastUpdatedTextView;

    /*
     * Private business logic
     */

    protected State state;

    private RotateAnimation ccwRotation;
    private RotateAnimation cwRotation;

    private int measuredHeaderHeight;
    private boolean scrollbarEnabled;
    private boolean scrollbarHidden = false;

    private boolean pullingOnHeader = false;
    private float pullOrigin;
    private int headerTopMargin;

    private boolean hasResetHeader;
    private long lastUpdated = -1;

    {
        setVerticalFadingEdgeEnabled(false);

        // arrow counterclockwise rotation animation
        ccwRotation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        ccwRotation.setInterpolator(new LinearInterpolator());
        ccwRotation.setDuration(ROTATE_ARROW_ANIMATION_DURATION);
        ccwRotation.setFillAfter(true);

        // arrow clockwise rotation animation
        cwRotation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        cwRotation.setInterpolator(new LinearInterpolator());
        cwRotation.setDuration(ROTATE_ARROW_ANIMATION_DURATION);
        cwRotation.setFillAfter(true);

        // initialize this property to super's value
        scrollbarEnabled = super.isVerticalScrollBarEnabled();

        // header initialization
        headerContainer = LayoutInflater.from(getContext()).inflate(
                R.layout.pull_to_refresh_header, null);
        header = headerContainer.findViewById(R.id.ptr_id_header);
        text = (TextView) headerContainer.findViewById(R.id.ptr_id_text);
        image = (ImageView) headerContainer.findViewById(R.id.ptr_id_image);
        spinner = headerContainer.findViewById(R.id.ptr_id_spinner);
        lastUpdatedTextView = (TextView) headerContainer.findViewById(R.id.ptr_id_last_updated);
        addHeaderView(headerContainer);

        setState(State.PULL_TO_REFRESH);

        ViewTreeObserver vto = header.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new HeaderHeightMeasurer());

        // to intercept OnItemClick and fix position issues
        super.setOnItemClickListener(new OnItemClickPositionFixer());
        // to intercept OnItemLongClick and fix position issues
        super.setOnItemLongClickListener(new OnItemLongClickPositionFixer());
    }

    /**
     * @see View#View(Context)
     */
    public PullToRefreshListView(Context context) {
        super(context);
    }

    /**
     * @see View#View(Context, AttributeSet)
     */
    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @see View#View(Context, AttributeSet, int)
     */
    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Activate an {@link OnPullToRefreshListener} to get notified on 'pull to
     * refresh' events.
     * 
     * @param listener
     *            The {@link OnPullToRefreshListener} to notify.
     */
    public void setOnPullToRefreshListener(OnPullToRefreshListener listener) {
        this.onRefreshListener = listener;
    }

    /**
     * Explicitly sets the state to 'Refreshing'. This is useful when you want to
     * show the spinner and 'Refreshing' text when the refresh was not triggered by
     * 'pull to refresh', for example on start.
     */
    public void setRefreshing() {
        scrollTo(0, 0);
        setState(State.REFRESHING);
        setHeaderMargin(0);
    }

    /**
     * Returns whether this pull-to-refresh list is in 'Refreshing' state.
     * 
     * @return {@code true} if this list is in 'Refreshing' state.
     */
    public boolean isRefreshing() {
        return state == State.REFRESHING;
    }

    /**
     * Notifies this list that the user class is done refreshing the data.
     */
    public void onRefreshComplete() {
        lastUpdated = System.currentTimeMillis();
        if (!isPullingOnHeader()) {
            pushHeaderBack(true);
        }
    }

    /*
     * SUPER SETTERS INTERCEPTED
     */

    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.userOnItemClickListener = onItemClickListener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.userOnItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean enabled) {
        scrollbarEnabled = enabled;
        if (!scrollbarHidden) {
            super.setVerticalScrollBarEnabled(enabled);
        }
    }

    /*
     * USING CLASS PREFERENCES
     */

    /**
     * Allows the using class to enable/disable the pull-to-refresh functionality.
     * Default is enabled.
     * 
     * @param enabled
     *            if {@code false} the list will behave as a standard
     *            {@link ListView}.
     */
    public void setPullToRefreshEnabled(boolean enabled) {
        ptrEnabled = enabled;
    }

    /**
     * Sets the text to display when the user has not pulled enough yet to trigger
     * the refresh on release.
     * 
     * @param pullToRefreshText
     *            The text to set.
     */
    public void setTextPullToRefresh(String pullToRefreshText) {
        this.pullToRefreshText = pullToRefreshText;
        if (state == State.PULL_TO_REFRESH) {
            text.setText(pullToRefreshText);
        }
    }

    /**
     * Sets the text to display when the user has pulled enough to trigger the
     * refresh on release.
     * 
     * @param releaseToRefreshText
     *            The text to set.
     */
    public void setTextReleaseToRefresh(String releaseToRefreshText) {
        this.releaseToRefreshText = releaseToRefreshText;
        if (state == State.RELEASE_TO_REFRESH) {
            text.setText(releaseToRefreshText);
        }
    }

    /**
     * Sets the text to display when refreshing. This text won't be seen when
     * {@link #setRefreshingHeaderEnabled(boolean)} was called with a {@code false}
     * argument.
     * 
     * @param refreshingText
     *            The text to set.
     */
    public void setTextRefreshing(String refreshingText) {
        this.refreshingText = refreshingText;
        if (state == State.REFRESHING) {
            text.setText(refreshingText);
        }
    }

    /**
     * Default is {@code false}. When lockScrollWhileRefreshing is set to true, the
     * list cannot scroll when in 'refreshing' swipeMode. It's 'locked' on
     * refreshing.
     * 
     * @param lockScrollWhileRefreshing
     */
    public void setLockScrollWhileRefreshing(boolean lockScrollWhileRefreshing) {
        this.lockScrollWhileRefreshing = lockScrollWhileRefreshing;
    }

    /**
     * Default is false. Shows the last-updated date/time in the 'Pull to Refresh'
     * header. See 'setLastUpdatedDateFormat' to set the date/time formatting.
     * 
     * @param show
     */
    public void showLastUpdatedText(boolean show) {
        this.showLastUpdatedText = show;
        if (show) {
            lastUpdatedTextView.setVisibility(View.VISIBLE);
        } else {
            lastUpdatedTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Defines the behavior of the header when it is released and refreshing is
     * triggered.
     * <ul>
     * <li>If {@code true}, the header is pushed back to the top, but it is visible
     * and displays a spinner and the loading text set by
     * {@link #setTextRefreshing(String)}.</li>
     * <li>If {@code false}, the header is pushed back all the way to the top, so
     * that it is hidden even when refreshing.
     * </ul>
     * <p>
     * Setting it to false allows the user class to fully control the refreshing
     * view. The user should still call {@link #onRefreshComplete()} when loading is
     * finished.
     * </p>
     * 
     * @param enabled
     *            Whether to hide or show the header while refreshing.
     */
    public void setRefreshingHeaderEnabled(boolean enabled) {
        this.refreshingHeaderEnabled = enabled;
    }

    /**
     * Sets the space threshold above the header to trigger the refresh event on
     * release. Defaults to 0, which means the state will be "pull to refresh" while
     * the header is not entirely visible, and "release to refresh" as soon as the
     * user sees the whole header (plus potential blank space above).
     * 
     * @param pullThreshold
     *            The minimum space above the pull-to-refresh header needed to
     *            trigger the refresh event, in pixels.
     */
    public void setPullThreshold(int pullThreshold) {
        this.pullThreshold = pullThreshold;
    }

    /**
     * Set the format in which the last-updated date/time is shown. Meaningless if
     * 'showLastUpdatedText == false (default)'. See 'setShowLastUpdatedText'.
     * 
     * @param lastUpdatedDateFormat
     */
    public void setLastUpdatedDateFormat(SimpleDateFormat lastUpdatedDateFormat) {
        this.lastUpdatedDateFormat = lastUpdatedDateFormat;
    }

    /*
     * BUSINESS LOGIC
     */

    /**
     * Updates the internal state and the corresponding UI for the header.
     * 
     * @param state
     *            The {@code State} to switch to.
     */
    private void setState(State state) {
        if (state != this.state) {
            onHeaderPullStateChanged(pullingOnHeader, state);
        }
        this.state = state;
        switch (state) {
        case PULL_TO_REFRESH:
            spinner.setVisibility(View.INVISIBLE);
            image.setVisibility(View.VISIBLE);
            text.setText(pullToRefreshText);
            if (showLastUpdatedText && lastUpdated != -1) {
                lastUpdatedTextView.setVisibility(View.VISIBLE);
                lastUpdatedTextView.setText(String.format(lastUpdatedText,
                        lastUpdatedDateFormat.format(new Date(lastUpdated))));
            }
            break;

        case RELEASE_TO_REFRESH:
            spinner.setVisibility(View.INVISIBLE);
            image.setVisibility(View.VISIBLE);
            text.setText(releaseToRefreshText);
            break;

        case REFRESHING:
            /*
             * this is called before pushing back the header to the top, so we don't
             * change the text if the using class doesn't want to see the refreshing
             * header.
             */
            if (refreshingHeaderEnabled) {
                spinner.setVisibility(View.VISIBLE);
                image.clearAnimation();
                image.setVisibility(View.INVISIBLE);
                text.setText(refreshingText);
            }
            break;
        }
    }

    /**
     * Uses touch events to perform pull-to-refresh behavior.
     * <p>
     * {@link MotionEvent#ACTION_MOVE} is used to reveal the header when the top of
     * the list is reached and the user scrolls farther. This is done by adding a
     * margin at the top of the header.
     * </p>
     * <p>
     * When {@link MotionEvent#ACTION_UP} is received, the header is pushed back to
     * the top, and the refresh event is triggered (depending on current state).
     * </p>
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!ptrEnabled) {
            // pull to refresh disabled
            return super.onTouchEvent(event);
        }
        if (lockScrollWhileRefreshing
                && state == State.REFRESHING) {
            // disable touch/scroll while refreshing the list
            Log.w(LOG_TAG, "touch event ignored while refreshing");
            return true;
        }
        if (getAnimation() != null && !getAnimation().hasEnded()) {
            // disable touch/scroll while animating the list
            Log.w(LOG_TAG, "touch event ignored while animating");
            return true;
        }

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if (isPullingOnHeader()) {
                switch (state) {
                case RELEASE_TO_REFRESH:
                    // pulled enough, refresh!
                    if (onRefreshListener == null) {
                        // no loading to do
                        pushHeaderBack(true);
                    } else {
                        setState(State.REFRESHING);
                        pushHeaderBack(!refreshingHeaderEnabled);
                        onRefreshListener.onPullToRefresh();
                    }
                    break;
                case PULL_TO_REFRESH:
                    // not pulled enough, push header back
                    pushHeaderBack(true);
                    break;
                default:
                    break;
                }
                // not pulling anymore
                setPullingOnHeader(false);
                unhideScrollBar();
                Log.d(LOG_TAG, "Header released");
            }
            break;

        case MotionEvent.ACTION_CANCEL:
            pushHeaderBack(true);
            setPullingOnHeader(false);
            unhideScrollBar();
            Log.d(LOG_TAG, "Header pull canceled");
            break;

        case MotionEvent.ACTION_MOVE:
            if (getFirstVisiblePosition() > HEADER_POSITION) {
                // header not visible
                if (isPullingOnHeader()) {
                    setPullingOnHeader(false);
                    unhideScrollBar();
                }
                break;
            } else if (!isPullingOnHeader()) {
                // user reached the top of the list and keeps pulling
                setPullingOnHeader(true);
                hideScrollBarTemporarily();
                // remember starting position for pull distance
                pullOrigin = event.getY();
                Log.d(LOG_TAG, "Start pulling on header");
            }

            if (isPullingOnHeader()) {
                // retrieve pull distance since pull start
                float absoluteY = event.getY();
                float relativeY = absoluteY - pullOrigin;
                relativeY /= PULL_RESISTANCE;

                int newHeaderMargin = Math.max(Math.round(relativeY) - header.getHeight(),
                        -header.getHeight());

                if (newHeaderMargin != headerTopMargin && state != State.REFRESHING) {
                    // update margin for the pull effect
                    setHeaderMargin(newHeaderMargin);

                    if (state == State.PULL_TO_REFRESH && headerTopMargin > pullThreshold) {
                        // header pulled beyond the threshold
                        Log.d(LOG_TAG, "Pull threshold exceeded");
                        setState(State.RELEASE_TO_REFRESH);
                        image.clearAnimation();
                        image.startAnimation(ccwRotation);
                    } else if (state == State.RELEASE_TO_REFRESH && headerTopMargin < pullThreshold) {
                        // header pushed back below the threshold
                        Log.d(LOG_TAG, "Push back threshold");
                        setState(State.PULL_TO_REFRESH);
                        image.clearAnimation();
                        image.startAnimation(cwRotation);
                    }

                    // hack to disable scrolling while pushing back up
                    setSelection(HEADER_POSITION);
                    // TODO find something cleaner (return true does not work)
                }
                return super.onTouchEvent(event) || true;
            } else {
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Sets the specified margin at the top of the header. To hide the header, use a
     * value of {@code -header.getHeight()}
     * 
     * @param margin
     *            The top margin to set, in pixels.
     */
    private void setHeaderMargin(int margin) {
        headerTopMargin = margin;
        MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
        mlp.setMargins(mlp.leftMargin, Math.round(margin), mlp.rightMargin, mlp.bottomMargin);
        header.setLayoutParams(mlp);
    }

    private void setPullingOnHeader(boolean pulling) {
        pullingOnHeader = pulling;
        onHeaderPullStateChanged(pulling, state);
    }

    private boolean isPullingOnHeader() {
        return pullingOnHeader;
    }

    /**
     * Temporarily overrides the value set via
     * {@link #setVerticalScrollBarEnabled(boolean)} to hide the scroll bar.
     * <p>
     * The method {@link #setVerticalScrollBarEnabled(boolean)} can still be called
     * by the using class, but it will take effect only when
     * {@link #unhideScrollBar()} is called.
     * </p>
     */
    private void hideScrollBarTemporarily() {
        scrollbarHidden = true;
        super.setVerticalScrollBarEnabled(false);
    }

    /**
     * Put back the last value set via {@link #setVerticalScrollBarEnabled(boolean)}
     * to show/hide the scroll bar.
     */
    private void unhideScrollBar() {
        scrollbarHidden = false;
        super.setVerticalScrollBarEnabled(scrollbarEnabled);
    }

    /**
     * Starts an animation to push the header back to the top.
     * 
     * @param allTheWayAndReset
     *            If {@code true}, the header will be pushed all the way to the top
     *            (so that it is hidden) and reset. If {@code false}, only the margin
     *            will be removed and the header will still be visible (and not
     *            reset).
     */
    private void pushHeaderBack(boolean allTheWayAndReset) {
        if (getFirstVisiblePosition() > HEADER_POSITION) {
            // header not visible, no animation needed
            if (allTheWayAndReset) {
                resetHeader();
            }
            return;
        }
        if (getAnimation() != null && !getAnimation().hasEnded()) {
            // animation already in progress
            Log.w(LOG_TAG, "trying to launch two push-back animations at the same time");
            return;
        }
        int yTranslate = allTheWayAndReset ? -headerContainer.getHeight()
                - headerContainer.getTop() + getPaddingTop() : header.getHeight()
                - headerContainer.getHeight();
        TranslateAnimation bounceAnimation = new TranslateAnimation(TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, yTranslate);
        bounceAnimation.setDuration(BOUNCE_ANIMATION_DURATION);
        bounceAnimation.setFillEnabled(true);
        bounceAnimation.setFillAfter(false);
        bounceAnimation.setFillBefore(true);
        bounceAnimation.setInterpolator(new OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION));
        bounceAnimation.setAnimationListener(new ListAnimationListener(yTranslate,
                allTheWayAndReset));
        startAnimation(bounceAnimation);
    }

    /**
     * Resets the header to its idle, invisible state.
     */
    private void resetHeader() {
        setHeaderMargin(-header.getHeight());
        setState(State.PULL_TO_REFRESH);
        image.clearAnimation();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (!hasResetHeader) {
            if (measuredHeaderHeight > 0 && state != State.REFRESHING) {
                setHeaderMargin(-measuredHeaderHeight);
            }
            hasResetHeader = true;
        }
    }

    /**
     * Called when the header is pulled or not pulled anymore.
     * 
     * @param userPullingOnHeader
     *            whether the user is pulling on the header now.
     * @param pullState
     *            the current {@link State} of the header
     */
    protected void onHeaderPullStateChanged(boolean userPullingOnHeader, State pullState) {}

    /**
     * Listens to global list animations. Hides the scrollbar during the animations.
     * Allows to run some code at the end of the animation.
     */
    private class ListAnimationListener implements AnimationListener {

        private int height, translation;
        private State stateAtAnimationStart;
        private boolean resetAfterAnimation;

        public ListAnimationListener(int translation, boolean resetAfterAnimation) {
            this.translation = translation;
            this.resetAfterAnimation = resetAfterAnimation;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            stateAtAnimationStart = state;

            // reduce this ListView's height
            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            height = lp.height;
            lp.height = getHeight() + Math.abs(translation);
            setLayoutParams(lp);

            hideScrollBarTemporarily();
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (resetAfterAnimation) {
                resetHeader();
                setSelection(FIRST_ITEM_POSITION);
            } else {
                // cut the margin that's now hidden
                setHeaderMargin(stateAtAnimationStart == State.REFRESHING && refreshingHeaderEnabled ? 0 : -measuredHeaderHeight - headerContainer.getTop());
                setSelection(HEADER_POSITION);
            }

            // restore this ListView's height
            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = height;
            setLayoutParams(lp);

            unhideScrollBar();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }

    /**
     * At the end of the layout construction, reads the header's height and hide it.
     */
    private class HeaderHeightMeasurer implements OnGlobalLayoutListener {
        @SuppressWarnings("deprecation")
        @Override
        public void onGlobalLayout() {
            int initialHeaderHeight = header.getHeight();
            if (initialHeaderHeight > 0) {
                measuredHeaderHeight = initialHeaderHeight;
                if (state != State.REFRESHING) {
                    setHeaderMargin(-measuredHeaderHeight);
                    requestLayout();
                }
            }
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    /**
     * Intercepts the {@code OnItemClick} event and translates the position indices
     * to ignore the header.
     */
    private class OnItemClickPositionFixer implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            hasResetHeader = false;
            if (userOnItemClickListener != null && position > HEADER_POSITION) {
                userOnItemClickListener.onItemClick(adapterView, view, position - 1, id);
            }
        }
    }

    /**
     * Intercepts the {@code OnItemLongClick} event and translates the position
     * indices to ignore the header.
     */
    private class OnItemLongClickPositionFixer implements OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
            hasResetHeader = false;
            if (userOnItemLongClickListener != null && position > HEADER_POSITION) {
                return userOnItemLongClickListener.onItemLongClick(adapterView, view, position - 1, id);
            }
            return false;
        }
    }
}
