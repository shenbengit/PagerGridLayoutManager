package com.shencoder.pagergridlayoutmanager;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    protected RecyclerView.SmoothScroller createScroller(@NonNull RecyclerView.LayoutManager layoutManager) {
        Log.i(TAG, "createScroller: ");
        if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            return null;
        }
        if (mRecyclerView != null) {
            return new PagerGridSmoothScroller(mRecyclerView);
        }
        return null;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        Log.d(TAG, "findTargetSnapPosition->velocityX: " + velocityX + ",velocityY: " + velocityY);
        final int itemCount = layoutManager.getItemCount();
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION;
        }
        int childCount = layoutManager.getChildCount();
        if (childCount == 0) {
            return RecyclerView.NO_POSITION;
        }
        if (!(layoutManager instanceof PagerGridLayoutManager)) {
            return RecyclerView.NO_POSITION;
        }
        final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        int[] calculateScrollDistance = calculateScrollDistance(velocityX, velocityY);
        //计算滑动的距离
        int scrollDistance = manager.canScrollHorizontally() ? calculateScrollDistance[0] : calculateScrollDistance[1];

        //存放锚点位置的view，一般数量为1或2个
        List<View> snapList = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View child = manager.getChildAt(i);
            if (child == null) {
                continue;
            }
            //先去寻找符合锚点位置的view
            if (manager.getPosition(child) % manager.getOnePageSize() == 0) {
                snapList.add(child);
            }
        }

        //滑动方向是否向前
        final boolean forwardDirection = isForwardFling(manager, velocityX, velocityY);
        //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
        final int layoutCenter = getLayoutStartAfterPadding(manager) + getLayoutTotalSpace(manager) / 2;
        //目标位置
        int targetPosition = RecyclerView.NO_POSITION;
        switch (snapList.size()) {
            case 1: {
                //数量为1：锚点view肯定是完全显示的，而且view新绘制进来的
                View view = snapList.get(0);
                int distance = distanceToCenter(manager, view);

                if (forwardDirection ? distance >= 0 : distance <= 0) {
                    int position = manager.getPosition(view);
                    //寻找上一个锚点位置
                    targetPosition = position - manager.getOnePageSize();
                    if (targetPosition < 0) {
                        targetPosition = RecyclerView.NO_POSITION;
                    }
                } else {
                    targetPosition = manager.getPosition(view);
                }
                break;
            }
            case 2: {
                //数量为2：锚点view肯定不是完全显示的
                View view1 = snapList.get(0);
                int distance1 = distanceToCenter(manager, view1);
                View view2 = snapList.get(1);
                int distance2 = distanceToCenter(manager, view2);

                //distance1肯定是小于layoutCenter
                //distance2可能是小于、等于、大于layoutCenter
                targetPosition = manager.getPosition(view1);
                if (forwardDirection) {
                    //判断谁离中心位置距离最近且view2的中心线在layoutCenter的前面
                    if (manager.getOnePageSize() == 1) {
                        //这种情况比较特殊，1行x1列的情况
                        if (Math.abs(distance1) > Math.abs(distance2)) {
                            targetPosition = manager.getPosition(view2);
                        }
                    }
                } else {

                    targetPosition = Math.abs(distance2) <= Math.abs(distance1) && distance2 < 0 ?
                            manager.getPosition(view2) : manager.getPosition(view1);
                }
                break;
            }
            default:
                //其他情况基本上不会出现
                break;
        }
        Log.d(TAG, "findTargetSnapPosition->forwardDirection:" + forwardDirection + ",targetPosition:" + targetPosition + ",scrollDistance:" + scrollDistance);

        return targetPosition;
    }

    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        Log.i(TAG, "findSnapView: ");
        if (layoutManager instanceof PagerGridLayoutManager) {
            final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
            return manager.findSnapView();
        }
        return null;
    }

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        Log.i(TAG, "calculateDistanceToFinalSnap-targetView: " + layoutManager.getPosition(targetView));
        int[] snapDistance = new int[2];
        if (layoutManager instanceof PagerGridLayoutManager) {
            final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
        }
        return snapDistance;
    }

    private boolean isForwardFling(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        if (layoutManager.canScrollHorizontally()) {
            return velocityX > 0;
        } else {
            return velocityY > 0;
        }
    }

    /**
     * 计算targetView中心位置到布局中心位置的距离
     *
     * @param layoutManager
     * @param targetView
     * @return
     */
    private int distanceToCenter(RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
        final int layoutCenter = getLayoutStartAfterPadding(layoutManager) + getLayoutTotalSpace(layoutManager) / 2;
        final int childCenter = getViewDecoratedStart(layoutManager, targetView)
                + (getViewDecoratedMeasurement(layoutManager, targetView) / 2);
        return childCenter - layoutCenter;
    }

    private int getLayoutStartAfterPadding(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.canScrollHorizontally() ? layoutManager.getPaddingStart() : layoutManager.getPaddingTop();
    }

    private int getLayoutTotalSpace(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.canScrollHorizontally() ?
                layoutManager.getWidth() - layoutManager.getPaddingStart() - layoutManager.getPaddingEnd() :
                layoutManager.getHeight() - layoutManager.getPaddingTop() - layoutManager.getPaddingBottom();
    }


    private int getViewDecoratedStart(RecyclerView.LayoutManager layoutManager, View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutManager.canScrollHorizontally()) {
            return layoutManager.getDecoratedLeft(view) - params.leftMargin;
        } else {
            return layoutManager.getDecoratedTop(view) - params.topMargin;
        }
    }

    private int getViewDecoratedMeasurement(RecyclerView.LayoutManager layoutManager, View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutManager.canScrollHorizontally()) {
            return layoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin;
        } else {
            return layoutManager.getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
        }
    }


}
