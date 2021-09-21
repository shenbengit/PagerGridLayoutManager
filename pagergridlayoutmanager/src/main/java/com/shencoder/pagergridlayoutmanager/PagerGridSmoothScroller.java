package com.shencoder.pagergridlayoutmanager;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author ShenBen
 * @date 2021/02/18 11:40
 * @email 714081644@qq.com
 */
public class PagerGridSmoothScroller extends LinearSmoothScroller {
    private static final String TAG = "PagerGridSmoothScroller";
    private RecyclerView mRecyclerView;

    public PagerGridSmoothScroller(@NonNull RecyclerView recyclerView) {
        super(recyclerView.getContext());
        mRecyclerView = recyclerView;
    }

    /**
     * 该方法会在targetSnapView被layout出来的时候调用。
     *
     * @param targetView targetSnapView
     * @param state
     * @param action
     */
    @Override
    protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
        Log.i(TAG, "onTargetFound-targetView: ");
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof PagerGridLayoutManager) {
            PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
            int targetPosition = manager.getPosition(targetView);

        } else {
            super.onTargetFound(targetView, state, action);
        }
    }

    /**
     * 该方法是计算滚动速率的，返回值代表滚动速率
     *
     * @param displayMetrics
     * @return
     */
    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return super.calculateSpeedPerPixel(displayMetrics);
    }
}
