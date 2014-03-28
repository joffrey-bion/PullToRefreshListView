package com.jbion.android.sample.pulltorefresh;

import java.util.LinkedList;

import android.os.AsyncTask;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.jbion.android.lib.list.pulltoloadmore.PullToLoadListView;
import com.jbion.android.lib.list.pulltorefresh.PullToRefreshListView;

public class LoadableList extends LinkedList<String> {

    private static final String BASE_NAME = "Email ";
    private final int max;
    private final int min;
    private final int stepNew;
    private final int stepOld;
    
    /** inclusive */
    private int oldest;
    /** exclusive */
    private int newest;

    public LoadableList(int initialOldest, int initialNewest, int min, int max, int stepOld, int stepNew) {
        this.min = min;
        this.max = max + 1;
        this.stepOld = stepOld;
        this.stepNew = stepNew;
        oldest = initialOldest;
        newest = initialNewest + 1;
        addNewNames(oldest, newest);
    }

    private static void timer() {
        // Simulates a background task
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private void addNewNames(int from, int to) {
        int limitedFrom = Math.max(from, min);
        int limitedTo = Math.min(to, max);
        for (int i = limitedFrom; i < limitedTo; i++) {
            addFirst(BASE_NAME + i);
        }
    }

    private void addOldNames(int from, int to) {
        int limitedFrom = Math.max(from, min);
        int limitedTo = Math.min(to, max + 1);
        for (int i = limitedTo - 1; i >= limitedFrom; i--) {
            add(BASE_NAME + i);
        }
    }

    public class LoadBottomDataTask extends AsyncTask<Void, Void, Void> {
        private ListView list;
        private BaseAdapter adapter;

        public LoadBottomDataTask(ListView list, BaseAdapter adapter) {
            this.list = list;
            this.adapter = adapter;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isCancelled()) {
                return null;
            }
            timer();
            addOldNames(oldest - stepOld, oldest);
            oldest -= stepOld;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // We need notify the adapter that the data have been changed
            adapter.notifyDataSetChanged();
            // Call onLoadMoreComplete when the LoadMore task, has finished
            ((PullToLoadListView) list).onLoadingComplete();
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            // Notify the loading more operation has finished
            ((PullToLoadListView) list).onLoadingComplete();
        }
    }

    public class LoadTopDataTask extends AsyncTask<Void, Void, Void> {
        private ListView list;
        private BaseAdapter adapter;

        public LoadTopDataTask(ListView list, BaseAdapter adapter) {
            this.list = list;
            this.adapter = adapter;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isCancelled()) {
                return null;
            }
            timer();
            addNewNames(newest, newest + stepNew);
            newest += stepNew;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            adapter.notifyDataSetChanged();
            refreshComplete();
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            refreshComplete();
        }

        private void refreshComplete() {
            ((PullToRefreshListView) list).onRefreshComplete();
        }
    }

}
