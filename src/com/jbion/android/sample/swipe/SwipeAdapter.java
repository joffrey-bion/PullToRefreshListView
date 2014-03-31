package com.jbion.android.sample.swipe;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.jbion.android.lib.list.swipe.SwipeListView;
import com.jbion.android.pulltorefresh.R;

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
        v.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Button 1 clicked", Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Button 2 clicked", Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.button3).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Button 3 clicked", Toast.LENGTH_SHORT).show();;
            }
        });
        return v;
    }
}