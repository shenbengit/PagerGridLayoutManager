package com.shencoder.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author ShenBen
 * @date 2021/09/28 22:58
 * @email 714081644@qq.com
 */
public class MyLinearLayoutManager extends LinearLayoutManager {
    public static final String TAG = "MyLinearLayoutManager";

    public MyLinearLayoutManager(Context context) {
        super(context);
    }

    public MyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public MyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        int scrollOffset = super.computeHorizontalScrollOffset(state);
        Log.i(TAG, "computeHorizontalScrollOffset: " + scrollOffset);
        return scrollOffset;
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        int scrollExtent = super.computeHorizontalScrollExtent(state);
        Log.i(TAG, "computeHorizontalScrollExtent: " + scrollExtent);
        return scrollExtent;
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        int scrollRange = super.computeHorizontalScrollRange(state);
        Log.i(TAG, "computeHorizontalScrollRange: " + scrollRange);
        return scrollRange;
    }
}
