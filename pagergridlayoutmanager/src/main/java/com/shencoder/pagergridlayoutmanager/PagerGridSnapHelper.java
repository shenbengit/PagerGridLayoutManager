package com.shencoder.pagergridlayoutmanager;

import android.graphics.Rect;
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
class PagerGridSnapHelper extends SnapHelper {
    private static final String TAG = "PagerGridSnapHelper";

    private RecyclerView mRecyclerView;
    /**
     * 存放锚点位置的view，一般数量为1或2个
     */
    private final List<View> snapList = new ArrayList<>(2);

    PagerGridSnapHelper() {
    }

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    protected RecyclerView.SmoothScroller createScroller(@NonNull RecyclerView.LayoutManager layoutManager) {
        if (!(layoutManager instanceof PagerGridLayoutManager)) {
            return null;
        }
        if (mRecyclerView != null) {
            return new PagerGridSmoothScroller(mRecyclerView, (PagerGridLayoutManager) layoutManager);
        }
        return null;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
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
        if (manager.getLayoutState().mLastScrollDelta == 0) {
            //说明无法滑动了，到头或滑动到底
            return RecyclerView.NO_POSITION;
        }
        int[] calculateScrollDistance = calculateScrollDistance(velocityX, velocityY);
        //计算滑动的距离
        int scrollDistance = manager.canScrollHorizontally() ? calculateScrollDistance[0] : calculateScrollDistance[1];

        if (manager.shouldHorizontallyReverseLayout()) {
            //取反
            scrollDistance = -scrollDistance;
        }

        //滑动方向是否向前
        final boolean forwardDirection = isForwardFling(manager, velocityX, velocityY);
        //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
        final int layoutCenter = getLayoutCenter(manager);

        reacquireSnapList(manager);

        //目标位置
        int targetPosition = RecyclerView.NO_POSITION;
        switch (snapList.size()) {
            case 1: {
                View view = snapList.get(0);
                int position = manager.getPosition(view);

                if (forwardDirection) {
                    //方向向前
                    if (scrollDistance >= layoutCenter) {
                        //计算滑动的距离直接超过布局一半值
                        targetPosition = position;
                    } else {
                        if (manager.shouldHorizontallyReverseLayout()) {
                            //水平滑动需要反转的情况
                            int viewDecoratedEnd = getViewDecoratedEnd(manager, view);
                            if (viewDecoratedEnd + scrollDistance >= layoutCenter) {
                                //view的结束线+scrollDistance大于于中间线，
                                //即view在中间线的左边或者上边
                                targetPosition = position;
                            } else {
                                //寻找上一个锚点位置
                                targetPosition = position - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            }
                        } else {
                            int viewDecoratedStart = getViewDecoratedStart(manager, view);
                            if (viewDecoratedStart - scrollDistance <= layoutCenter) {
                                //view的起始线-scrollDistance 小于中间线，
                                //即view在中间线的左边或者上边
                                targetPosition = position;
                            } else {
                                //寻找上一个锚点位置
                                targetPosition = position - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            }
                        }
                    }
                } else {
                    //方向向后
                    if (Math.abs(scrollDistance) >= layoutCenter) {
                        //计算滑动的距离直接超过布局一半值
                        targetPosition = position - 1;
                        if (targetPosition < 0) {
                            targetPosition = RecyclerView.NO_POSITION;
                        }
                    } else {
                        if (manager.shouldHorizontallyReverseLayout()) {
                            //水平滑动需要反转的情况
                            int viewDecoratedStart = getViewDecoratedStart(manager, view);
                            if (viewDecoratedStart - Math.abs(scrollDistance) < layoutCenter) {
                                //寻找上一个锚点位置
                                targetPosition = position - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            } else {
                                targetPosition = position;
                            }
                        } else {
                            int viewDecoratedEnd = getViewDecoratedEnd(manager, view);
                            if (viewDecoratedEnd + Math.abs(scrollDistance) > layoutCenter) {
                                //寻找上一个锚点位置
                                targetPosition = position - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            } else {
                                targetPosition = position;
                            }
                        }
                    }
                }
            }
            break;
            case 2: {
                View view1 = snapList.get(0);
                int position1 = manager.getPosition(view1);
                View view2 = snapList.get(1);
                int position2 = manager.getPosition(view2);

                if (manager.shouldHorizontallyReverseLayout()) {
                    if (forwardDirection) {
                        //方向向前
                        if (scrollDistance >= layoutCenter) {
                            //计算滑动的距离直接超过布局一半值
                            targetPosition = position2;
                        } else {
                            int viewDecoratedEnd2 = getViewDecoratedEnd(manager, view2);
                            if (viewDecoratedEnd2 + scrollDistance >= layoutCenter) {
                                //view的结束线+scrollDistance 大于中间线，
                                //即view在中间线的左边或者上边
                                targetPosition = position2;
                            } else {
                                targetPosition = position2 - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            }
                        }
                    } else {
                        if (Math.abs(scrollDistance) >= layoutCenter) {
                            targetPosition = position2 - 1;
                            if (targetPosition < 0) {
                                targetPosition = RecyclerView.NO_POSITION;
                            }
                        } else {
                            int viewDecoratedStart1 = getViewDecoratedStart(manager, view1);
                            if (viewDecoratedStart1 - Math.abs(scrollDistance) <= layoutCenter) {
                                targetPosition = position2 - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            } else {
                                targetPosition = position2;
                            }
                        }
                    }
                } else {
                    if (forwardDirection) {
                        //方向向前
                        if (scrollDistance >= layoutCenter) {
                            //计算滑动的距离直接超过布局一半值
                            targetPosition = position2;
                        } else {
                            int viewDecoratedStart2 = getViewDecoratedStart(manager, view2);

                            if (viewDecoratedStart2 - scrollDistance <= layoutCenter) {
                                //view的起始线-scrollDistance 小于中间线，
                                //即view在中间线的左边或者上边
                                targetPosition = position2;
                            } else {
                                targetPosition = position2 - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            }
                        }
                    } else {
                        if (Math.abs(scrollDistance) >= layoutCenter) {
                            targetPosition = position2 - 1;
                            if (targetPosition < 0) {
                                targetPosition = RecyclerView.NO_POSITION;
                            }
                        } else {
                            int viewDecoratedEnd1 = getViewDecoratedEnd(manager, view1);
                            if (viewDecoratedEnd1 + Math.abs(scrollDistance) >= layoutCenter) {
                                targetPosition = position2 - 1;
                                if (targetPosition < 0) {
                                    targetPosition = RecyclerView.NO_POSITION;
                                }
                            } else {
                                targetPosition = position2;
                            }
                        }
                    }
                }
            }
            break;
            case 3:
                //1行*1列可能出现的情况
                targetPosition = manager.getPosition(snapList.get(1));
                break;
            default:
                if (PagerGridLayoutManager.DEBUG) {
                    Log.w(TAG, "findTargetSnapPosition-snapList.size: " + snapList.size());
                }
                break;
        }
        if (PagerGridLayoutManager.DEBUG) {
            Log.d(TAG, "findTargetSnapPosition->forwardDirection:" + forwardDirection + ",targetPosition:" + targetPosition + ",velocityX: " + velocityX + ",velocityY: " + velocityY + ",scrollDistance:" + scrollDistance + ",snapList:" + snapList.size());
        }
        snapList.clear();
        return targetPosition;
    }

    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        View snapView = null;
        if (layoutManager instanceof PagerGridLayoutManager) {
            PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
            reacquireSnapList(manager);
            switch (snapList.size()) {
                case 1: {
                    snapView = snapList.get(0);
                }
                break;
                case 2: {
                    //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
                    final int layoutCenter = getLayoutCenter(manager);
                    View view1 = snapList.get(0);
                    View view2 = snapList.get(1);
                    Rect rect = new Rect();
                    manager.getDecoratedBoundsWithMargins(view2, rect);

                    if (manager.shouldHorizontallyReverseLayout()) {
                        int viewDecoratedEnd2 = getViewDecoratedEnd(manager, view2);
                        if (viewDecoratedEnd2 <= layoutCenter) {
                            snapView = view1;
                        } else {
                            snapView = view2;
                        }
                    } else {
                        int viewDecoratedStart2 = getViewDecoratedStart(manager, view2);
                        if (viewDecoratedStart2 <= layoutCenter) {
                            snapView = view2;
                        } else {
                            snapView = view1;
                        }
                    }
                }
                break;
                case 3:
                    //1行*1列可能出现的情况
                    snapView = snapList.get(1);
                    break;
                default:
                    if (PagerGridLayoutManager.DEBUG) {
                        Log.w(TAG, "findSnapView wrong -> snapList.size: " + snapList.size());
                    }
                    break;
            }
            if (PagerGridLayoutManager.DEBUG) {
                Log.i(TAG, "findSnapView: position:" + (snapView != null ? layoutManager.getPosition(snapView) : RecyclerView.NO_POSITION) + ", snapList.size:" + snapList.size());
            }
            snapList.clear();
        }
        return snapView;
    }

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] snapDistance = new int[2];
        int targetPosition = layoutManager.getPosition(targetView);
        if (layoutManager instanceof PagerGridLayoutManager) {
            final PagerGridLayoutManager manager = (PagerGridLayoutManager) layoutManager;
            //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
            final int layoutCenter = getLayoutCenter(manager);
            int dx;
            int dy;
            Rect targetRect = new Rect();
            layoutManager.getDecoratedBoundsWithMargins(targetView, targetRect);
            if (manager.shouldHorizontallyReverseLayout()) {
                int viewDecoratedEnd = getViewDecoratedEnd(manager, targetView);
                if (viewDecoratedEnd >= layoutCenter) {
                    //向前回退
                    Rect snapRect = manager.getStartSnapRect();
                    dx = PagerGridSmoothScroller.calculateDx(manager, snapRect, targetRect);
                    dy = PagerGridSmoothScroller.calculateDy(manager, snapRect, targetRect);
                } else {
                    //向后前进
                    dx = -calculateDxToNextPager(manager, targetRect);
                    dy = -calculateDyToNextPager(manager, targetRect);
                }
            } else {
                int viewDecoratedStart = getViewDecoratedStart(manager, targetView);

                if (viewDecoratedStart <= layoutCenter) {
                    //向前回退
                    Rect snapRect = manager.getStartSnapRect();
                    dx = PagerGridSmoothScroller.calculateDx(manager, snapRect, targetRect);
                    dy = PagerGridSmoothScroller.calculateDy(manager, snapRect, targetRect);
                } else {
                    //向后前进
                    dx = -calculateDxToNextPager(manager, targetRect);
                    dy = -calculateDyToNextPager(manager, targetRect);
                }
            }
            snapDistance[0] = dx;
            snapDistance[1] = dy;

            if (snapDistance[0] == 0 && snapDistance[1] == 0) {
                //说明滑动完成，计算页标
                manager.calculateCurrentPagerIndexByPosition(targetPosition);
            }
            if (PagerGridLayoutManager.DEBUG) {
                Log.i(TAG, "calculateDistanceToFinalSnap-targetView: " + targetPosition + ",snapDistance: " + Arrays.toString(snapDistance));
            }
        }
        return snapDistance;
    }

    private boolean isForwardFling(PagerGridLayoutManager layoutManager, int velocityX, int velocityY) {
        return layoutManager.canScrollHorizontally() ?
                (layoutManager.getShouldReverseLayout() ? velocityX < 0 : velocityX > 0)
                : velocityY > 0;
    }

    /***
     * 获取锚点view
     * @param manager
     */
    private void reacquireSnapList(PagerGridLayoutManager manager) {
        if (!snapList.isEmpty()) {
            snapList.clear();
        }
        int childCount = manager.getChildCount();
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
    }

    static int calculateDxToNextPager(PagerGridLayoutManager manager, Rect targetRect) {
        if (!manager.canScrollHorizontally()) {
            return 0;
        }
        return getLayoutEndAfterPadding(manager) - targetRect.left;
    }

    static int calculateDyToNextPager(PagerGridLayoutManager manager, Rect targetRect) {
        if (!manager.canScrollVertically()) {
            return 0;
        }
        return getLayoutEndAfterPadding(manager) - targetRect.top;
    }

    /**
     * 计算targetView中心位置到布局中心位置的距离
     *
     * @param layoutManager
     * @param targetView
     * @return
     */
    static int distanceToCenter(RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        //布局中心位置，水平滑动为X轴坐标，垂直滑动为Y轴坐标
        final int layoutCenter = getLayoutCenter(layoutManager);
        final int childCenter = getChildViewCenter(layoutManager, targetView);
        return childCenter - layoutCenter;
    }

    static int getLayoutCenter(RecyclerView.LayoutManager layoutManager) {
        return getLayoutStartAfterPadding(layoutManager) + getLayoutTotalSpace(layoutManager) / 2;
    }

    static int getLayoutStartAfterPadding(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.canScrollHorizontally() ? layoutManager.getPaddingStart() : layoutManager.getPaddingTop();
    }

    static int getLayoutEndAfterPadding(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.canScrollHorizontally() ?
                layoutManager.getWidth() - layoutManager.getPaddingEnd()
                : layoutManager.getHeight() - layoutManager.getPaddingBottom();
    }

    static int getLayoutTotalSpace(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.canScrollHorizontally() ?
                layoutManager.getWidth() - layoutManager.getPaddingStart() - layoutManager.getPaddingEnd() :
                layoutManager.getHeight() - layoutManager.getPaddingTop() - layoutManager.getPaddingBottom();
    }

    static int getChildViewCenter(RecyclerView.LayoutManager layoutManager, View targetView) {
        return getViewDecoratedStart(layoutManager, targetView)
                + (getViewDecoratedMeasurement(layoutManager, targetView) / 2);
    }

    static int getViewDecoratedStart(RecyclerView.LayoutManager layoutManager, View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutManager.canScrollHorizontally()) {
            return layoutManager.getDecoratedLeft(view) - params.leftMargin;
        } else {
            return layoutManager.getDecoratedTop(view) - params.topMargin;
        }
    }

    static int getViewDecoratedEnd(RecyclerView.LayoutManager layoutManager, View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutManager.canScrollHorizontally()) {
            return layoutManager.getDecoratedRight(view) - params.rightMargin;
        } else {
            return layoutManager.getDecoratedBottom(view) - params.bottomMargin;
        }
    }

    static int getViewDecoratedMeasurement(RecyclerView.LayoutManager layoutManager, View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutManager.canScrollHorizontally()) {
            return layoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin;
        } else {
            return layoutManager.getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
        }
    }
}
