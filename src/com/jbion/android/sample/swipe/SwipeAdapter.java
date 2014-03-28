package com.jbion.android.sample.swipe;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.jbion.android.lib.list.swipe.SwipeListView;

public class SwipeAdapter extends ArrayAdapter<String> {

    public SwipeAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).initSwipeState(v, position);
        }
        return v;
    }
}