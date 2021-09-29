package com.shencoder.pagergridlayoutmanager;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 处理滑动冲突
 *
 * @author ShenBen
 * @date 2021/9/28 09:24
 * @email 714081644@qq.com
 */
class PagerGridItemTouchListener extends RecyclerView.SimpleOnItemTouchListener {
    private static final String TAG = "ItemTouchListener";

    private final PagerGridLayoutManager layoutManager;
    private final RecyclerView recyclerView;
    private final GestureDetector gestureDetector;

    PagerGridItemTouchListener(PagerGridLayoutManager layoutManager, RecyclerView recyclerView) {
        this.layoutManager = layoutManager;
        this.recyclerView = recyclerView;
        this.gestureDetector = new GestureDetector(recyclerView.getContext(), new GestureListener());
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        Log.i(TAG, "onInterceptTouchEvent-action: " + e.getAction());
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        Log.i(TAG, "onRequestDisallowInterceptTouchEvent: ");
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "ItemTouchListener";

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.i(TAG, "onScroll-distanceX: " + distanceX + ",distanceY: " + distanceY);
            int distance = (int) (layoutManager.canScrollHorizontally() ? distanceX : distanceY);
            if (layoutManager.canScrollHorizontally()) {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollHorizontally(distance));
            } else {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollVertically(distance));
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.i(TAG, "onFling-velocityX: " + velocityX + ",velocityY: " + velocityY);
            int distance = (int) (layoutManager.canScrollHorizontally() ? velocityX : velocityY);
            if (layoutManager.canScrollHorizontally()) {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollHorizontally(distance));
            } else {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollVertically(distance));
            }
            return false;
        }
    }
}
