package com.shencoder.pagergridlayoutmanager;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

/**
 * @author ShenBen
 * @date 2021/01/13 13:43
 * @email 714081644@qq.com
 */
public class PagerGridSnapHelper extends SnapHelper {
    private static final String TAG = "PagerGridSnapHelper";

    private static final int THRESHOLD = 1;
    private RecyclerView mRecyclerView;

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        int[] snapDistance = new int[2];
        return snapDistance;
    }

    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        return manager.findSnapView();
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        Log.i(TAG, "findTargetSnapPosition->velocityX: " + velocityX + ",velocityY: " + velocityY);
        final int itemCount = layoutManager.getItemCount();
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION;
        }

        final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        int targetPosition = RecyclerView.NO_POSITION;
        int minFlingVelocity = manager.getEnd();
        if (manager.canScrollHorizontally()) {
            int absVelocityX = Math.abs(velocityX);

        } else {
            int absVelocityY = Math.abs(velocityY);

        }
        return targetPosition;
    }

    @Override
    public boolean onFling(int velocityX, int velocityY) {
        Log.i(TAG, "onFling->velocityX: " + velocityX + ",velocityY: " + velocityY);
        final RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        if (mRecyclerView.getAdapter() == null) {
            return false;
        }
        PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        int minFlingVelocity = manager.getEnd();
        return (Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity)
                && snapFromFling(layoutManager, velocityX, velocityY);
    }

    private boolean snapFromFling(@NonNull RecyclerView.LayoutManager layoutManager, int velocityX,
                                  int velocityY) {
        if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            return false;
        }
        RecyclerView.SmoothScroller smoothScroller = createScroller(layoutManager);
        if (smoothScroller == null) {
            return false;
        }
        int targetPosition = findTargetSnapPosition(layoutManager, velocityX, velocityY);
        if (targetPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        smoothScroller.setTargetPosition(targetPosition);
        layoutManager.startSmoothScroll(smoothScroller);
        return true;
    }


    @Nullable
    @Override
    protected RecyclerView.SmoothScroller createScroller(RecyclerView.LayoutManager layoutManager) {
        if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            return null;
        }
        if (mRecyclerView != null) {
            return new PagerGridSmoothScroller(mRecyclerView);
        }
        return null;
    }
}