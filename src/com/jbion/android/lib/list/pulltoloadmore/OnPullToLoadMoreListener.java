package com.jbion.android.lib.list.pulltoloadmore;

/**
 * Interface definition for a callback to be invoked when this list reaches the
 * last item.
 */
public interface OnPullToLoadMoreListener {
	/**
	 * Called when the list reaches the last item (the last item is visible to
	 * the user) A call to {@link PullToLoadListView #onLoadingComplete()} is
	 * expected to indicate that the loading has completed.
	 */
	public void onPullToLoadMore();
}