package com.jbion.android.lib.list.swipe;

/**
 * Listener to get callback notifications for the SwipeListView
 */
public interface SwipeListViewListener {

    /**
     * Called when open animation finishes
     * @param position list item
     * @param toRight Open to right
     */
    void onOpened(int position, boolean toRight);

    /**
     * Called when close animation finishes
     * @param position list item
     * @param fromRight Close from right
     */
    void onClosed(int position, boolean fromRight);

    /**
     * Called when the list changed
     */
    void onListChanged();

    /**
     * Called when user is moving an item
     * @param position list item
     * @param x Current position X
     */
    void onMove(int position, float x);

    /**
     * Called when user clicks on the front view
     * @param position list item
     */
    void onClickFrontView(int position);

    /**
     * Called when user clicks on the back view
     * @param position list item
     */
    void onClickBackView(int position);

    /**
     * Called when user dismisses items
     * @param reverseSortedPositions Items dismissed
     */
    void onDismiss(int[] reverseSortedPositions);

    /**
     * Called when user choice item
     * @param position position that choice
     * @param selected if item is selected or not
     */
    void onChoiceChanged(int position, boolean selected);

    /**
     * User start choice items
     */
    void onChoiceStarted();

    /**
     * User end choice items
     */
    void onChoiceEnded();

}
