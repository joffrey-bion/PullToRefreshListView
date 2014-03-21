package com.jbion.android.sample.swipe;

import java.util.LinkedList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.swipe_item_view,
                R.id.item_text, mListItems);

        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.swipe, menu);
        return true;
    }
    
}
