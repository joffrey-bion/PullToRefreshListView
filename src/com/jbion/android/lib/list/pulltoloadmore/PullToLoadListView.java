package com.jbion.android.lib.list.pulltoloadmore;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import com.jbion.android.lib.list.pulltorefresh.PullToRefreshListView;
import com.jbion.android.pulltorefresh.R;

public class PullToLoadListView extends PullToRefreshListView implements OnScrollListener {

    private static final String LOG_TAG = PullToLoadListView.class.getSimpleName();

    /** Whether the pull-up feature is enabled */
    private boolean loadMoreEnabled = true;

    /**
     * Listener to process load more items when the user reaches the end of the list.
     */
    private OnPullToLoadMoreListener onLoadMoreListener;
    /**
     * Holds the {@link OnScrollListener} set by the user class. Since this class
     * uses super's {@link OnScrollListener}, it has to provide a way for the using
     * class to also use a scroll listener. This is done via this field and
     * {@link #setOnScrollListener(OnScrollListener)}.
     */
    private OnScrollListener onScrollListener;

    /** Whether the list is loading more items */
    private boolean mIsLoadingMore = false;

    private View footerContainer;
    private View progressBar;

    {
        // footer initialization
        LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        footerContainer = mInflater.inflate(R.layout.pull_to_load_footer, this, false);
        progressBar = footerContainer.findViewById(R.id.load_more_progressBar);
        addFooterView(footerContainer);
        setFooterDividersEnabled(false);

        super.setOnScrollListener(this);
    }

    /**
     * @see View#View(Context)
     */
    public PullToLoadListView(Context context) {
        super(context);
    }

    /**
     * @see View#View(Context, AttributeSet)
     */
    public PullToLoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @see View#View(Context, AttributeSet, int)
     */
    public PullToLoadListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Register a callback to be invoked when this list reaches the end (last item be
     * visible) and more items should be loaded.
     * 
     * @param onLoadMoreListener
     *            The callback to run.
     */
    public void setOnLoadMoreListener(OnPullToLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    /**
     * Notify the loading more operation has finished
     */
    public void onLoadingComplete() {
        mIsLoadingMore = false;
    }

    /**
     * Allows the using class to dynamically enable/disable the load-more-on-pull-up
     * feature.
     * 
     * @param enabled
     *            if {@code false}, disables the 'load more' feature.
     */
    public void setPullToLoadMoreEnabled(boolean enabled) {
        loadMoreEnabled = enabled;
    }

    /*
     * We keep the using class's listener in this class and use this class as super
     * listener.
     */
    @Override
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    /*
     * BUSINESS LOGIC
     */

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {

        // if need a list to load more items
        if (onLoadMoreListener != null && loadMoreEnabled) {
            Log.d(LOG_TAG, "first=" + firstVisibleItem + " visible=" + visibleItemCount + " total="
                    + totalItemCount);
            if (visibleItemCount == totalItemCount) {
                // nothing to load if the screen is not even full
                progressBar.setVisibility(View.GONE);
            } else {
                if (!mIsLoadingMore && (firstVisibleItem + visibleItemCount >= totalItemCount)) {
                    progressBar.setVisibility(View.VISIBLE);
                    mIsLoadingMore = true;
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onPullToLoadMore();
                    }
                }
            }
        }
        if (onScrollListener != null) {
            onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

}