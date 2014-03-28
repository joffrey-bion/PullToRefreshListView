package com.jbion.android.sample.swipe;

import java.util.LinkedList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jbion.android.lib.list.pulltorefresh.PullToRefreshListView;
import com.jbion.android.lib.list.swipe.SwipeListView;
import com.jbion.android.pulltorefresh.R;

public class SwipeActivity extends ListActivity {

    // list with the data to show in the listview
    private LinkedList<String> mListItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);
        mListItems = new LinkedList<String>();
        for (int i = 0; i < 25; i++) {
            mListItems.add("Email " + i);
        }

        ArrayAdapter<String> adapter = new MyAdapter(this, R.layout.swipe_item_view,
                R.id.item_text, mListItems);

        ListView list = getListView();
        if (list instanceof PullToRefreshListView) {
            ((PullToRefreshListView) list).setRefreshingHeaderEnabled(false);
        }
        setListAdapter(adapter);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.swipe, menu);
        return true;
    }

    public class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            ListView list = getListView();
            if (list instanceof SwipeListView) {
                ((SwipeListView) list).initSwipeState(v, position);
            }
            return v;
        }
    }
}
