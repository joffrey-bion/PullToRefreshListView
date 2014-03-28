package com.jbion.android.sample;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jbion.android.lib.list.pulltoloadmore.OnPullToLoadMoreListener;
import com.jbion.android.lib.list.pulltoloadmore.PullToLoadListView;
import com.jbion.android.lib.list.pulltorefresh.OnPullToRefreshListener;
import com.jbion.android.lib.list.pulltorefresh.PullToRefreshListView;
import com.jbion.android.lib.list.swipe.SwipeListView;
import com.jbion.android.pulltorefresh.R;
import com.jbion.android.sample.pulltorefresh.LoadableList;
import com.jbion.android.sample.swipe.SwipeAdapter;

public class SampleActivity extends ListActivity {

    public static final boolean SWIPE = true;
    public static final boolean PULL_TO_REFRESH = true;
    public static final boolean PULL_TO_LOAD = true;

    private static final int MAX = 120;
    private static final int MIN = 0;
    private static final int STEP = 3;

    // list with the data to show in the listview
    private LoadableList mListItems;
    private BaseAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);
        mListItems = new LoadableList(55, 57, MIN, MAX, STEP, STEP);
        mAdapter = createAdapter(mListItems);
        setListAdapter(mAdapter);
        setRefreshListeners();
        setOnClickListeners();
    }

    private ArrayAdapter<String> createAdapter(LoadableList list) {
        if (getListView() instanceof SwipeListView) {
            return new SwipeAdapter(this, R.layout.swipe_item_view, R.id.item_text, list);
        } else {
            return new ArrayAdapter<String>(this, R.layout.swipe_item_view, R.id.item_text, list);
        }
    }

    private void setRefreshListeners() {
        final ListView list = getListView();

        if (list instanceof SwipeListView) {
            SwipeListView sl = (SwipeListView) list;
            sl.setSwipeEnabled(SWIPE);
        }

        if (list instanceof PullToRefreshListView) {
            final PullToRefreshListView ptr = (PullToRefreshListView) list;

            ptr.setPullToRefreshEnabled(PULL_TO_REFRESH);
            ptr.setRefreshingHeaderEnabled(false);
            ptr.setLockScrollWhileRefreshing(false);
            ptr.showLastUpdatedText(true);
            ptr.setLastUpdatedDateFormat(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale
                    .getDefault()));

            ptr.setOnPullToRefreshListener(new OnPullToRefreshListener() {
                @Override
                public void onPullToRefresh() {
                    mListItems.new LoadTopDataTask(ptr, mAdapter).execute();
                }
            });
        }

        if (list instanceof PullToLoadListView) {
            final PullToLoadListView ptl = (PullToLoadListView) list;

            ptl.setPullToLoadMoreEnabled(PULL_TO_LOAD);

            ptl.setOnLoadMoreListener(new OnPullToLoadMoreListener() {
                @Override
                public void onPullToLoadMore() {
                    mListItems.new LoadBottomDataTask(ptl, mAdapter).execute();
                }
            });
        }
    }

    private void setOnClickListeners() {
        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Toast.makeText(SampleActivity.this,
                        "Item " + position + " clicked (id=" + id + ")", Toast.LENGTH_SHORT).show();
            }
        });

        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
                    long id) {
                Toast.makeText(SampleActivity.this,
                        "Item " + position + " long clicked (id=" + id + ")", Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.swipe, menu);
        return true;
    }
}
