package com.jbion.android.pulltorefresh.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.jbion.android.pulltorefresh.R;

public class EndReachedListener implements OnScrollListener {

	/**
	 * Interface definition for a callback to be invoked when this list reaches the
	 * last item.
	 */
	public interface OnLoadMoreListener {
		/**
		 * Called when the list reaches the last item (the last item is visible to
		 * the user) A call to {@link PullToLoadListView #onLoadingComplete()} is
		 * expected to indicate that the loading has completed.
		 */
		public void onLoadMore();
	}

	/** Whether the pull-up feature is enabled */
	private boolean loadMoreEnabled = true;

	/**
	 * Listener to process load more items when the user reaches the end of the list.
	 */
	private OnLoadMoreListener onLoadMoreListener;
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

	/**
	 * Creates a new {@link EndReachedListener} and attaches it to the specified
	 * list.
	 * 
	 * @param list
	 *            The {@link ListView} to attach this listener to.
	 */
	public EndReachedListener(ListView list) {
		// footer initialization
		LayoutInflater mInflater = (LayoutInflater) list.getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		footerContainer = mInflater.inflate(R.layout.pull_to_load_footer, list, false);
		progressBar = footerContainer.findViewById(R.id.load_more_progressBar);
		list.addFooterView(footerContainer);
		list.setFooterDividersEnabled(false);
		list.setOnScrollListener(this);
	}

	/**
	 * Register a callback to be invoked when this list reaches the end (last item be
	 * visible) and more items should be loaded.
	 * 
	 * @param onLoadMoreListener
	 *            The callback to run.
	 */
	public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
		this.onLoadMoreListener = onLoadMoreListener;
	}

	/**
	 * Notify the loading operation has finished.
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
	public void setLoadMoreEnabled(boolean enabled) {
		loadMoreEnabled = enabled;
	}

	/**
	 * Sets another {@link OnScrollListener} to notify. This allows the user class to
	 * use an {@link OnScrollListener} with a {@link ListView} that uses this class.
	 * 
	 * @param onScrollListener
	 *            The listener to pass the events on to.
	 */
	public void setOnScrollListener(OnScrollListener onScrollListener) {
		this.onScrollListener = onScrollListener;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {

		// if need a list to load more items
		if (onLoadMoreListener != null && loadMoreEnabled) {
			if (visibleItemCount == totalItemCount) {
				progressBar.setVisibility(View.GONE);
			} else {
				if (!mIsLoadingMore && (firstVisibleItem + visibleItemCount >= totalItemCount)) {
					progressBar.setVisibility(View.VISIBLE);
					mIsLoadingMore = true;
					if (onLoadMoreListener != null) {
						onLoadMoreListener.onLoadMore();
					}
				}
			}
		}
		if (onScrollListener != null)
			onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

}