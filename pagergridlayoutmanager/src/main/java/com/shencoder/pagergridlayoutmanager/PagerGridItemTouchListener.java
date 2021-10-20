package com.shencoder.pagergridlayoutmanager;

import android.util.Log;
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
    private int mScrollPointerId;
    private int mInitialTouchX;
    private int mInitialTouchY;

    PagerGridItemTouchListener(PagerGridLayoutManager layoutManager, RecyclerView recyclerView) {
        this.layoutManager = layoutManager;
        this.recyclerView = recyclerView;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        final int actionMasked = e.getActionMasked();
        final int actionIndex = e.getActionIndex();
        if (PagerGridLayoutManager.DEBUG) {
            Log.i(TAG, "onInterceptTouchEvent-actionMasked: " + actionMasked + ", actionIndex: " + actionIndex);
        }
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = (int) (e.getY(actionIndex) + 0.5f);

                recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);

                final int dx = x - mInitialTouchX;
                final int dy = y - mInitialTouchY;

                if (layoutManager.canScrollHorizontally()) {
                    recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollHorizontally(-dx));
                }
                if (layoutManager.canScrollVertically()) {
                    recyclerView.getParent().requestDisallowInterceptTouchEvent(recyclerView.canScrollVertically(-dy));
                }
            }
            break;
            case MotionEvent.ACTION_POINTER_UP: {
                onPointerUp(e);
            }
            break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                //do nothing
            }
            break;
        }
        return false;
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }
}
