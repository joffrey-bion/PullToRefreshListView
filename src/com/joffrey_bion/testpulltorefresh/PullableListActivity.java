package com.joffrey_bion.testpulltorefresh;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.joffrey_bion.testpulltorefresh.widget.PullToRefreshListView;
import com.joffrey_bion.testpulltorefresh.widget.PullToRefreshListView.OnRefreshListener;
import com.joffrey_bion.testpulltorefresh.widget.PullableListView;
import com.joffrey_bion.testpulltorefresh.widget.PullableListView.OnPulledUpListener;

public class PullableListActivity extends ListActivity {

	private static final String BASE_NAME = "Element ";
	private static final int MAX = 200;
	private static final int MIN = 0;
	private static final int INC = 3;
	private static final int DEC = 3;

	// list with the data to show in the listview
	private LinkedList<String> mListItems;
	/** inclusive */
	private int oldest = 55;
	/** exclusive */
	private int newest = 57;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_layout);

		mListItems = new LinkedList<String>();
		addNames(oldest, newest, true);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mListItems);

		setListAdapter(adapter);
		setRefreshListeners();
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Toast.makeText(PullableListActivity.this,
						"Item " + position + " clicked (id=" + id + ")", Toast.LENGTH_SHORT).show();
			}
		});
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				Toast.makeText(PullableListActivity.this,
						"Item " + position + " long clicked (id=" + id + ")", Toast.LENGTH_SHORT)
						.show();
				return true;
			}
		});
	}

	private void setRefreshListeners() {
		final PullToRefreshListView ptr = (PullToRefreshListView) getListView();
		ptr.setLockScrollWhileRefreshing(false);
		ptr.showLastUpdatedText(true);
		ptr.setLastUpdatedDateFormat(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()));
		ptr.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				new LoadTopDataTask().execute();
				// ptr.onRefreshComplete();
			}
		});

		if (ptr instanceof PullableListView) {
			((PullableListView) ptr).setOnPulledUpListener(new OnPulledUpListener() {
				@Override
				public void onPulledUp() {
					new LoadBottomDataTask().execute();
				}
			});
		}
	}

	private static void timer() {
		// Simulates a background task
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

	private List<String> addNames(int from, int to, boolean reverse) {
		int min = Math.max(from, MIN);
		int max = Math.min(to, MAX);
		for (int i = min; i < max; i++) {
			if (reverse) {
				mListItems.addFirst(BASE_NAME + i);
			} else {
				mListItems.add(BASE_NAME + i);
			}
		}
		return mListItems;
	}

	private class LoadBottomDataTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (isCancelled()) {
				return null;
			}
			timer();
			addNames(oldest - DEC, oldest, false);
			oldest -= DEC;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// We need notify the adapter that the data have been changed
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
			// Call onLoadMoreComplete when the LoadMore task, has finished
			((PullableListView) getListView()).onPulledUpHandled();
			super.onPostExecute(result);
		}

		@Override
		protected void onCancelled() {
			// Notify the loading more operation has finished
			((PullableListView) getListView()).onPulledUpHandled();
		}
	}

	private class LoadTopDataTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (isCancelled()) {
				return null;
			}
			timer();
			addNames(newest, newest + INC, true);
			newest += INC;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
			refreshComplete();
			super.onPostExecute(result);
		}

		@Override
		protected void onCancelled() {
			refreshComplete();
		}

		private void refreshComplete() {
			((PullToRefreshListView) getListView()).onRefreshComplete();
		}
	}
}