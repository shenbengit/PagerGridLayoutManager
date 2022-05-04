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
class PagerGridSmoothScroller extends LinearSmoothScroller {
    private static final String TAG = "PagerGridSmoothScroller";
    @NonNull
    private final PagerGridLayoutManager mLayoutManager;
    @NonNull
    private final RecyclerView mRecyclerView;
    /**
     * @see #calculateSpeedPerPixel(DisplayMetrics)
     */
    static final float MILLISECONDS_PER_INCH = 100f;
    /**
     * @see #calculateTimeForScrolling(int)
     */
    static final int MAX_SCROLL_ON_FLING_DURATION = 500; //ms

    PagerGridSmoothScroller(@NonNull RecyclerView recyclerView, @NonNull PagerGridLayoutManager layoutManager) {
        super(recyclerView.getContext());
        mRecyclerView = recyclerView;
        mLayoutManager = layoutManager;
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

            PointF pointF = computeScrollVectorForPosition(targetPosition);
            if (pointF == null) {
                //为null，则不处理
                return;
            }

            boolean isLayoutToEnd = pointF.x > 0 || pointF.y > 0;
            if (manager.shouldHorizontallyReverseLayout()) {
                isLayoutToEnd = !isLayoutToEnd;
            }
            Rect snapRect;
            if (isLayoutToEnd) {
                snapRect = manager.getStartSnapRect();
            } else {
                snapRect = manager.getEndSnapRect();
            }
            Rect targetRect = new Rect();
            layoutManager.getDecoratedBoundsWithMargins(targetView, targetRect);
            int dx = calculateDx(manager, snapRect, targetRect, isLayoutToEnd);
            int dy = calculateDy(manager, snapRect, targetRect, isLayoutToEnd);
            final int time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)));
            if (PagerGridLayoutManager.DEBUG) {
                Log.i(TAG, "onTargetFound-targetPosition:" + targetPosition + ", dx:" + dx + ",dy:" + dy + ",time:" + time + ",isLayoutToEnd:" + isLayoutToEnd + ",snapRect:" + snapRect + ",targetRect:" + targetRect);
            }
            if (time > 0) {
                action.update(dx, dy, time, mDecelerateInterpolator);
            } else {
                //说明滑动完成，计算页标
                manager.calculateCurrentPagerIndexByPosition(targetPosition);
            }
        }
    }

    /**
     * 不可过小，不然可能会出现划过再回退的情况
     *
     * @param displayMetrics
     * @return 值越大，滚动速率越慢，反之
     */
    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        float speed = mLayoutManager.getMillisecondPreInch() / displayMetrics.densityDpi;
        if (PagerGridLayoutManager.DEBUG) {
            Log.i(TAG, "calculateSpeedPerPixel-speed: " + speed);
        }
        return speed;
    }

    /**
     * 为避免长时间滚动，设置一个最大滚动时间
     *
     * @param dx 滚动的像素距离
     * @return 值越大，滑动时间越长，滚动速率越慢，反之
     */
    @Override
    protected final int calculateTimeForScrolling(int dx) {
        int time = Math.min(mLayoutManager.getMaxScrollOnFlingDuration(), super.calculateTimeForScrolling(dx));
        Log.i(TAG, "calculateTimeForScrolling-time: " + time);
        return time;
    }

    static int calculateDx(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect) {
        if (!manager.canScrollHorizontally()) {
            return 0;
        }
        return targetRect.left - snapRect.left;
    }

    static int calculateDy(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect) {
        if (!manager.canScrollVertically()) {
            return 0;
        }
        return targetRect.top - snapRect.top;
    }

    static int calculateDx(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect, boolean isLayoutToEnd) {
        if (!manager.canScrollHorizontally()) {
            return 0;
        }
        return isLayoutToEnd ? (targetRect.left - snapRect.left) : (targetRect.right - snapRect.right);
    }

    static int calculateDy(PagerGridLayoutManager manager, Rect snapRect, Rect targetRect, boolean isLayoutToEnd) {
        if (!manager.canScrollVertically()) {
            return 0;
        }
        return isLayoutToEnd ? (targetRect.top - snapRect.top) : (targetRect.bottom - snapRect.bottom);
    }
}
