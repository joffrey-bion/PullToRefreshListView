package com.jbion.android.sample.swipe;

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jbion.android.pulltorefresh.R;

public class SwipeActivity extends ListActivity {

	// list with the data to show in the listview
	private LinkedList<String> mListItems;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_swipe);
		mListItems = new LinkedList<String>();
		for (int i = 1; i < 25; i++) {
			mListItems.add("Email " + i);
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.swipe_item_view, R.id.item_text, mListItems);

		setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.swipe, menu);
		return true;
	}

	public class SwipeListAdapter extends BaseAdapter {

		private Context context;
		private ArrayList<String> list;

		public SwipeListAdapter(Context context, ArrayList<String> data) {
			this.list = data;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = View.inflate(context, R.layout.swipe_item_view, parent);
			} else {
				v = convertView;
			}
			TextView text = (TextView) v.findViewById(R.id.swipe_front_view).findViewById(R.id.item_text);
			text.setText(list.get(position));
			return v;
		}

	}
}
