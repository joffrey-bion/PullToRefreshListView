package com.joffrey_bion.testpulltorefresh.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.joffrey_bion.testpulltorefresh.R;

public class PullableListView extends PullToRefreshListView implements OnScrollListener {

	private static final String LOG_TAG = PullableListView.class.getSimpleName();

	/**
	 * Interface definition for a callback to be invoked when list reaches the last
	 * item (the user load more items in the list)
	 */
	public interface OnPulledUpListener {
		/**
		 * Called when the list reaches the last item (the last item is visible to
		 * the user) A call to {@link PullableListView #onPulledUpHandled()} is
		 * expected to indicate that the loadmore has completed.
		 */
		public void onPulledUp();
	}

	// Listener to process load more items when user reaches the end of the list
	private OnPulledUpListener mOnPulledUpListener;
	// To know if the list is loading more items
	private boolean mIsLoadingMore = false;

	// footer
	private RelativeLayout mFooterView;
	// private TextView mLabLoadMore;
	private ProgressBar mProgressBarLoadMore;

	{
		// footer
		LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		mFooterView = (RelativeLayout) mInflater.inflate(R.layout.pull_footer, this, false);
		mProgressBarLoadMore = (ProgressBar) mFooterView.findViewById(R.id.load_more_progressBar);
		addFooterView(mFooterView);
		
		setOnScrollListener(this);
	}

	public PullableListView(Context context) {
		super(context);
	}

	public PullableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Register a callback to be invoked when this list reaches the end (last item be
	 * visible)
	 * 
	 * @param onPulledUpListener
	 *            The callback to run.
	 */
	public void setOnPulledUpListener(OnPulledUpListener onPulledUpListener) {
		mOnPulledUpListener = onPulledUpListener;
	}

	public void onPulledUp() {
		Log.d(LOG_TAG, "onPulledUp()");
		if (mOnPulledUpListener != null) {
			mOnPulledUpListener.onPulledUp();
		}
	}

	/**
	 * Notify the loading more operation has finished
	 */
	public void onPulledUpHandled() {
		Log.d(LOG_TAG, "onPulledUpHandled()");
		mIsLoadingMore = false;
	}
	

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
		
		// if need a list to load more items
		if (mOnPulledUpListener != null) {

			if (visibleItemCount == totalItemCount) {
				mProgressBarLoadMore.setVisibility(View.GONE);
				return;
			}

			boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

			if (!mIsLoadingMore && loadMore) {
				mProgressBarLoadMore.setVisibility(View.VISIBLE);
				mIsLoadingMore = true;
				onPulledUp();
			}

		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}
}