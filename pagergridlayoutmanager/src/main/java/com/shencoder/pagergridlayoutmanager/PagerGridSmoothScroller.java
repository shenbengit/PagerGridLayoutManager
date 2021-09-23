package com.shencoder.pagergridlayoutmanager;

import android.graphics.PointF;
import android.graphics.Rect;
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

    private final RecyclerView mRecyclerView;
    private static final float MILLISECONDS_PER_INCH = 200f;
    private static final int MAX_SCROLL_ON_FLING_DURATION = 500; //ms

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
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof PagerGridLayoutManager) {
            PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
            int targetPosition = manager.getPosition(targetView);
            Rect snapRect = manager.getSnapRect();
            Rect targetRect = new Rect();
            layoutManager.getDecoratedBoundsWithMargins(targetView, targetRect);
            int dx = calculateDx(manager, snapRect, targetRect);
            int dy = calculateDy(manager, snapRect, targetRect);
//            final int time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)));
//            if (time > 0) {
//                Log.i(TAG, "onTargetFound-targetPosition:" + targetPosition + ", dx:" + dx + ",dy:" + dy + ",time:" + time + ",snapRect:" + snapRect + ",targetRect:" + targetRect);
//
//                action.update(dx, dy, time, mDecelerateInterpolator);
//            }
            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
            final int time = calculateTimeForDeceleration(distance);
            if (time > 0) {
                Log.i(TAG, "onTargetFound-targetPosition:" + targetPosition + ", dx:" + dx + ",dy:" + dy + ",time:" + time + ",snapRect:" + snapRect + ",targetRect:" + targetRect);
                action.update(dx, dy, time, mLinearInterpolator);
            }
        } else {
            super.onTargetFound(targetView, state, action);
        }
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return Math.min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx));
    }

    private int calculateDx(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect) {
        if (!manager.canScrollHorizontally()) {
            return 0;
        }
        int snapPreference = getHorizontalSnapPreference();
        switch (snapPreference) {
            case SNAP_TO_START:
            case SNAP_TO_END:
                return targetRect.left - snapRect.left;
            default:
                return 0;
        }
    }

    private int calculateDy(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect) {
        if (!manager.canScrollVertically()) {
            return 0;
        }
        int snapPreference = getVerticalSnapPreference();
        switch (snapPreference) {
            case SNAP_TO_START:
            case SNAP_TO_END:
                return targetRect.top - snapRect.top;
            default:
                return 0;
        }
    }

}
