package com.jbion.android.lib.list.pulltorefresh;

/**
 * Interface to implement when you want to get notified of 'pull to refresh'
 * events. Call setOnRefreshListener(..) to activate an OnPullToRefreshListener.
 */
public interface OnPullToRefreshListener {

    /**
     * Called when a refresh is requested.
     */
    public void onPullToRefresh();
}