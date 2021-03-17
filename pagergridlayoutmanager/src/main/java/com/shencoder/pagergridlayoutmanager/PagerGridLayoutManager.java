package com.shencoder.pagergridlayoutmanager;

import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分页滑动网格布局LayoutManager
 *
 * @author ShenBen
 * @date 2021/01/10 17:01
 * @email 714081644@qq.com
 */
public class PagerGridLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    private static final String TAG = "PagerGridLayoutManager";
    public static boolean DEBUG = BuildConfig.DEBUG;
    /**
     * 水平滑动
     */
    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    /**
     * 垂直滑动
     */
    public static final int VERTICAL = RecyclerView.VERTICAL;

    private PagerGridSnapHelper mPagerGridSnapHelper;
    /**
     * 当前滑动方向
     */
    @RecyclerView.Orientation
    private int mOrientation = HORIZONTAL;
    /**
     * 行数
     */
    @IntRange(from = 1)
    private int mRows;
    /**
     * 列数
     */
    @IntRange(from = 1)
    private int mColumns;
    /**
     * 一页的数量 {@link #mRows} * {@link #mColumns}
     */
    private int mOnePageSize;
    /**
     * 总页数
     */
    private int mPageCount;
    /**
     * item的宽度
     */
    private int mItemWidth;
    /**
     * item的高度
     */
    private int mItemHeight;
    /**
     * 一个ItemView的所有ItemDecoration占用的宽度(px)
     */
    private int mItemWidthUsed;
    /**
     * 一个ItemView的所有ItemDecoration占用的高度(px)
     */
    private int mItemHeightUsed;

    private int mMaxScrollX;
    private int mMaxScrollY;

    /**
     * 当前页码下标
     * 从0开始
     */
    private int mCurrentPagerIndex;
    /**
     * 当前页左上角锚点位置
     */
    private int mCurrentPagerAnchorPosition;
    /**
     * 需要滑动到的位置
     */
    private int mPendingScrollPosition = RecyclerView.NO_POSITION;
    /**
     * 计算开始滑动到结束滑动之间的滑动距离
     * 用于判断是否滑动到下一页或上一页
     */
    private int mScrollDelta = 0;
    /**
     * 用于保存一些状态
     */
    private LayoutState mLayoutState;
    private final LayoutChunkResult mLayoutChunkResult = new LayoutChunkResult();

    private RecyclerView mRecyclerView;

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns) {
        this(rows, columns, HORIZONTAL);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, @RecyclerView.Orientation int orientation) {
        mRows = rows;
        mColumns = columns;
        setOrientation(orientation);
    }

    /**
     * @return 子布局LayoutParams，默认全部填充
     */
    @Override
    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }


    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
//        mRecyclerView.addOnChildAttachStateChangeListener(enforceChildFillListener());
        mPagerGridSnapHelper = new PagerGridSnapHelper();
        mPagerGridSnapHelper.attachToRecyclerView(view);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (DEBUG) {
            Log.d(TAG, "onLayoutChildren: " + state.toString());
        }
        if (getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }

        ensureLayoutState();
        mLayoutState.mRecycle = false;
        mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mAvailable = getEnd();
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;
        /*
        设置绘制的起始位置
        水平滑动则减去一个View的宽度
        垂直滑动则减去一个View的高度
         */
        mLayoutState.mOffSetX = mOrientation == HORIZONTAL ? getPaddingLeft() - mItemWidth : getPaddingLeft();
        mLayoutState.mOffSetY = mOrientation == HORIZONTAL ? getPaddingTop() : getPaddingLeft() - mItemHeight;

        //回收Views
        detachAndScrapAttachedViews(recycler);

        mOnePageSize = mRows * mColumns;
        //均分宽
        mItemWidth = mColumns > 0 ? getRealWidth() / mColumns : 0;
        //均分高
        mItemHeight = mRows > 0 ? getRealHeight() / mRows : 0;
        mItemWidthUsed = getRealWidth() - mItemWidth;
        mItemHeightUsed = getRealHeight() - mItemHeight;

        //计算总页数
        mPageCount = getItemCount() / mOnePageSize;
        if (getItemCount() % mOnePageSize != 0) {
            ++mPageCount;
        }
        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            mCurrentPagerIndex = getPageIndexByPosition(mPendingScrollPosition);
            mCurrentPagerAnchorPosition = mCurrentPagerIndex * mOnePageSize;
            mLayoutState.mCurrentPosition = mCurrentPagerIndex * mOnePageSize;
        } else {
            mCurrentPagerIndex = 0;
            mCurrentPagerAnchorPosition = 0;
            mLayoutState.mCurrentPosition = 0;
        }
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: getRealWidth:" + getRealWidth() + ",getRealHeight:" + getRealHeight() + ",mItemWidth:" + mItemWidth + ",mItemHeight:" + mItemHeight + ",mPageCount:" + mPageCount);
        }
        fill(recycler, state);

        View childAt = getChildAt(0);
        assert childAt != null;
        System.out.println("getDecoratedMeasuredWidth(childAt):" + getDecoratedMeasuredWidth(childAt));
        System.out.println("getDecoratedMeasuredHeight(childAt):" + getDecoratedMeasuredHeight(childAt));
        System.out.println("getDecoratedLeft(childAt):" + getDecoratedLeft(childAt));
        System.out.println("getDecoratedTop(childAt):" + getDecoratedTop(childAt));
        System.out.println("getDecoratedRight(childAt):" + getDecoratedRight(childAt));
        System.out.println("getDecoratedBottom(childAt):" + getDecoratedBottom(childAt));
//        fill(recycler, state, mCurrentPagerIndex);
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size());
        }
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        mPendingScrollPosition = RecyclerView.NO_POSITION;
    }

    @Nullable
    @Override
    public View findViewByPosition(int position) {
        return super.findViewByPosition(position);
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {

    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == VERTICAL) {
            //垂直滑动不处理
            return 0;
        }
        return scrollBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == HORIZONTAL) {
            //水平滑动不处理
            return 0;
        }
        return scrollBy(dy, recycler, state);

    }

    @Override
    public void onScrollStateChanged(int state) {
        Log.i(TAG, "onScrollStateChanged: " + state);
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE:
                //停止滑动时
                Log.i(TAG, "onScrollStateChanged: 总共滑动的距离: " + mScrollDelta);
                mScrollDelta = 0;
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                //手指拖拽滑动

                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                //自由滑动时

                break;
            default:
                break;
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == RecyclerView.HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == RecyclerView.VERTICAL;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        mRecyclerView = null;
    }

    public void setColumns(@IntRange(from = 1) int columns) {
        if (!isIdle()) {
            return;
        }
        if (mColumns == columns) {
            return;
        }
        mColumns = columns;
        requestLayout();
    }

    @IntRange(from = 1)
    public int getColumns() {
        return mColumns;
    }

    public void setRows(@IntRange(from = 1) int rows) {
        if (!isIdle()) {
            return;
        }
        if (mRows == rows) {
            return;
        }
        mRows = rows;
        requestLayout();
    }

    @IntRange(from = 1)
    public int getRows() {
        return mRows;
    }

    /**
     * 设置滑动方向
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public void setOrientation(@RecyclerView.Orientation int orientation) {
        if (!isIdle()) {
            return;
        }
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation:" + orientation);
        }
        if (orientation != mOrientation) {
            mOrientation = orientation;
            requestLayout();
        }
    }

    /**
     * @param position position
     * @return 获取当前position所在页下标
     */
    public int getPageIndexByPosition(int position) {
        return position / mOnePageSize;
    }


    /**
     * 获取真实宽度
     *
     * @return
     */
    private int getRealWidth() {
        return getWidth() - getPaddingStart() - getPaddingEnd();
    }

    /**
     * 获取真实高度
     *
     * @return
     */
    private int getRealHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private void ensureLayoutState() {
        if (mLayoutState == null) {
            mLayoutState = createLayoutState();
        }
    }

    private LayoutState createLayoutState() {
        return new LayoutState();
    }

    /**
     * 填充布局
     *
     * @param recycler
     * @param state
     * @return 添加的像素数，用于滚动
     */
    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        LayoutState layoutState = mLayoutState;
        int start = layoutState.mAvailable;
        int remainingSpace = layoutState.mAvailable;
        LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
        System.out.println("fill->remainingSpace:" + remainingSpace);
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            layoutState.mAvailable -= layoutChunkResult.mConsumed;
            remainingSpace -= layoutChunkResult.mConsumed;
            System.out.println("fill->remainingSpace:" + remainingSpace);
            //回收View
            if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleViews(recycler);
            }
        }

        //因为最后一列或者一行可能只绘制了收尾的一个，补满
        while (layoutState.hasMore(state)) {
            if (isNeedMoveToNextSpan(layoutState.mCurrentPosition)) {
                //如果需要切换行或列，直接退出
                break;
            }
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            //回收View
            if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleViews(recycler);
            }
        }
        return start - layoutState.mAvailable;
    }

    /**
     * 填充View
     * 直接绘制一行或者一列
     *
     * @param recycler
     * @param state
     * @param layoutState
     * @param layoutChunkResult
     */
    private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState, LayoutChunkResult layoutChunkResult) {
        boolean layoutToEnd = layoutState.mLayoutDirection == LayoutState.LAYOUT_END;
        int position = layoutState.mCurrentPosition;
        View view = layoutState.next(recycler);
        if (layoutToEnd) {
            addView(view);
        } else {
            addView(view, 0);
        }
        measureChildWithMargins(view, mItemWidthUsed, mItemHeightUsed);
        //是否需要移动到下一行或者下一列进行绘制
        boolean isNeedMoveToNextSpan = isNeedMoveToNextSpan(position);
        int left;
        int top;
        int right;
        int bottom;

        if (mOrientation == HORIZONTAL) {
            //水平滑动
            if (layoutToEnd) {
                if (isNeedMoveToNextSpan) {
                    //下一列绘制，从头部开始
                    left = layoutState.mOffSetX + mItemWidth;
                    top = getPaddingTop();
                    layoutChunkResult.mConsumed = mItemWidth;
                } else {
                    //当前列绘制
                    left = layoutState.mOffSetX;
                    top = layoutState.mOffSetY;
                    layoutChunkResult.mConsumed = 0;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
                layoutState.mOffSetY = bottom;
            } else {
                if (isNeedMoveToNextSpan) {
                    //上一列绘制，从底部开始
                    left = layoutState.mOffSetX - mItemWidth;
                    bottom = getPaddingBottom();
                    layoutChunkResult.mConsumed = mItemWidth;
                } else {
                    //当前列绘制
                    left = layoutState.mOffSetX;
                    bottom = layoutState.mOffSetY;
                    layoutChunkResult.mConsumed = 0;
                }
                top = bottom - mItemHeight;
                right = left + mItemWidth;
                layoutState.mOffSetY = top;
            }
            layoutState.mOffSetX = left;
        } else {
            if (layoutToEnd) {
                if (isNeedMoveToNextSpan) {
                    //下一行绘制，从头部开始
                    left = getPaddingLeft();
                    top = layoutState.mOffSetY + mItemHeight;
                    layoutChunkResult.mConsumed = mItemHeight;
                } else {
                    //当前行绘制
                    left = layoutState.mOffSetX;
                    top = layoutState.mOffSetY;
                    layoutChunkResult.mConsumed = 0;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
                layoutState.mOffSetX = right;
            } else {
                if (isNeedMoveToNextSpan) {
                    //上一行绘制，从尾部开始
                    right = getPaddingRight();
                    bottom = layoutState.mOffSetY;
                    left = right - mItemWidth;
                    top = bottom - mItemHeight;
                    layoutChunkResult.mConsumed = mItemHeight;
                } else {
                    //当前行绘制
                    left = layoutState.mOffSetX - mItemWidth;
                    top = layoutState.mOffSetY;
                    right = left + mItemWidth;
                    bottom = top + mItemHeight;
                    layoutChunkResult.mConsumed = 0;
                }
                layoutState.mOffSetX = left;
            }
            layoutState.mOffSetY = top;
        }
        layoutDecoratedWithMargins(view, left, top, right, bottom);
        System.out.println("layoutChunk->mOffSetX: " + layoutState.mOffSetX + ",mOffSetY: " + layoutState.mOffSetY + ",position: " + position + ",mConsumed: " + layoutChunkResult.mConsumed);
    }

    /**
     * @param pagerIndex 页下标
     * @return 根据页下标，找出所在页所有item位置
     */
    private List<Integer> getPagerChildPositionByPagerIndex(int pagerIndex) {
        List<Integer> list = new ArrayList<>(mOnePageSize);
        int itemCount = getItemCount();
        if (itemCount > 0 && pagerIndex < mPageCount) {
            int firstPosition = mOnePageSize * pagerIndex;
            //计算当前页可放置child的数量
            int size = itemCount < firstPosition + mOnePageSize ? itemCount - firstPosition : mOnePageSize;
            for (int i = 0; i < size; i++) {
                list.add(firstPosition + i);
            }
        }
        return list;
    }

    /**
     * @param delta    手指滑动的距离
     * @param recycler
     * @param state
     * @return
     */
    private int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || delta == 0 || mPageCount == 1) {
            return 0;
        }
        ensureLayoutState();
        mLayoutState.mRecycle = true;
        final int layoutDirection = delta > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDelta = Math.abs(delta);
        updateLayoutState(layoutDirection, absDelta, true, state);
        int consumed = mLayoutState.mScrollingOffset + fill(recycler, state);
        if (DEBUG) {
            Log.i(TAG, "scrollBy: childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size());
        }
        if (consumed < 0) {
            return 0;
        }

        int scrolled = delta;
        mScrollDelta += delta;
        //移动
        offsetChildren(-scrolled);
        return scrolled;
    }

    private void updateLayoutState(int layoutDirection, int requiredSpace,
                                   boolean canUseExistingSpace, RecyclerView.State state) {
        mLayoutState.mLayoutDirection = layoutDirection;
        boolean layoutToEnd = layoutDirection == LayoutState.LAYOUT_END;
        mLayoutState.mItemDirection = layoutToEnd ? LayoutState.ITEM_DIRECTION_TAIL : LayoutState.ITEM_DIRECTION_HEAD;
        View child;
        int scrollingOffset;
        if (layoutToEnd) {
            child = getChildClosestToEnd();
            scrollingOffset = getDecoratedEnd(child) - getEndAfterPadding();
            updateLayoutStateOffset(child, mLayoutState, true, mOrientation);
        } else {
            child = getChildClosestToStart();
            scrollingOffset = -getDecoratedStart(child) + getStartAfterPadding();
            updateLayoutStateOffset(child, mLayoutState, false, mOrientation);
        }

        mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;

        mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutState.mAvailable -= scrollingOffset;
        }
//        System.out.println("updateLayoutState-->layoutToEnd:" + layoutToEnd + ",mCurrentPosition:" + mLayoutState.mCurrentPosition + ",mOffSet:" + mLayoutState.mOffSet + ",scrollingOffset:" + scrollingOffset + ",mAvailable:" + mLayoutState.mAvailable + ",requiredSpace:" + requiredSpace);
        mLayoutState.mScrollingOffset = scrollingOffset;
    }

    private View getChildClosestToEnd() {
        return getChildAt(getChildCount() - 1);
    }

    private View getChildClosestToStart() {
        return getChildAt(0);
    }

    private void updateLayoutStateOffset(View child, LayoutState layoutState, boolean layoutToEnd, int orientation) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (orientation == HORIZONTAL) {
            layoutState.mOffSetX = getDecoratedLeft(child) - params.leftMargin;
            if (layoutToEnd) {
                layoutState.mOffSetY = getDecoratedBottom(child) + params.bottomMargin;
            } else {
                layoutState.mOffSetY = getDecoratedTop(child) - params.topMargin;
            }
        } else {
            layoutState.mOffSetY = getDecoratedTop(child) - params.topMargin;
            if (layoutToEnd) {
                layoutState.mOffSetX = getDecoratedRight(child) + params.rightMargin;
            } else {
                layoutState.mOffSetY = getDecoratedLeft(child) - params.leftMargin;
            }
        }
    }

    /**
     * 回收View
     *
     * @param recycler
     */
    private void recycleViews(RecyclerView.Recycler recycler) {
        //是否回收view
        if (!mLayoutState.mRecycle) {
            return;
        }
        int scrollingOffset = mLayoutState.mScrollingOffset;
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            //水平向左或者垂直向上滑动
            recycleViewsFromEnd(recycler);
        } else {
            //水平向右或者垂直向下滑动
            recycleViewsFromStart(recycler);
        }
    }

    private void recycleViewsFromStart(RecyclerView.Recycler recycler) {
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedEnd(childAt);
                if (decorated >= 0) {
                    continue;
                }
                removeAndRecycleView(childAt, recycler);
            }
        }
    }

    private void recycleViewsFromEnd(RecyclerView.Recycler recycler) {
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedStart(childAt);
                if (decorated <= getEnd()) {
                    continue;
                }
                removeAndRecycleView(childAt, recycler);
            }
        }
    }

    private int getDecoratedEnd(View child) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        return mOrientation == HORIZONTAL ? getDecoratedRight(child) + params.rightMargin : getDecoratedBottom(child) + params.bottomMargin;
    }

    private int getDecoratedStart(View child) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        return mOrientation == HORIZONTAL ? getDecoratedLeft(child) - params.leftMargin : getDecoratedTop(child) - params.topMargin;
    }

    private int getEndAfterPadding() {
        return mOrientation == HORIZONTAL ? getWidth() - getPaddingRight() : getHeight() - getPaddingBottom();
    }

    private int getStartAfterPadding() {
        return mOrientation == HORIZONTAL ? getPaddingLeft() : getPaddingTop();
    }

    private int getEnd() {
        return mOrientation == HORIZONTAL ? getRealWidth() : getRealHeight();
    }

    /**
     * 移动Children
     *
     * @param delta 移动偏移量
     */
    private void offsetChildren(int delta) {
        if (mOrientation == HORIZONTAL) {
            offsetChildrenHorizontal(delta);
        } else {
            offsetChildrenVertical(delta);
        }
    }

    /**
     * 根据position 获取在全局中的坐标，注意不是在当前页的位置
     * 水平滑动：
     * |0 4 8 | 12 16
     * |1 5 9 | 13 17        4行*3列
     * |2 6 10| 14 18
     * |3 7 11| 15
     * <p>
     * 垂直滑动：
     * ————————————
     * 0  1  2  3
     * 4  5  6  7             3行*4列
     * 8  9  10 11
     * ————————————
     * 12 13 14
     *
     * @param position position
     * @return int[0]:行坐标位置,从0开始;int[1]:列坐标位置，从0开始
     */
    @NonNull
    private int[] getGlobalCoordinateByPosition(int position) {
        int[] coordinate = new int[2];
        if (getItemCount() == 0 || position >= getItemCount()) {
            return coordinate;
        }
        int rows = mRows;
        int columns = mColumns;
        int rowPosition;
        int columnPosition;
        if (mOrientation == HORIZONTAL) {
            rowPosition = position % rows;
            columnPosition = position / rows;
        } else {
            rowPosition = position / columns;
            columnPosition = position % columns;
        }
        //行位置，从0开始
        coordinate[0] = rowPosition;
        //列位置，从0开始
        coordinate[1] = columnPosition;
        return coordinate;
    }

    /**
     * 根据position在当前页的位置
     * 水平滑动：
     * |0 4 8 | 12 16
     * |1 5 9 | 13 17        4行*3列
     * |2 6 10| 14 18
     * |3 7 11| 15
     * <p>
     * 垂直滑动：
     * ————————————
     * 0  1  2  3
     * 4  5  6  7             3行*4列
     * 8  9  10 11
     * ————————————
     * 12 13 14
     *
     * @param position position
     * @return int[0]:行坐标位置,从0开始;int[1]:列坐标位置，从0开始
     */
    @NonNull
    private int[] getCoordinateByPosition(int position) {
        int[] coordinate = new int[2];
        if (getItemCount() == 0 || position >= getItemCount()) {
            return coordinate;
        }
        int rows = mRows;
        int columns = mColumns;
        int rowPosition;
        int columnPosition;
        if (mOrientation == HORIZONTAL) {
            rowPosition = position % rows;
            columnPosition = position / rows % columns;
        } else {
            rowPosition = position / columns % rows;
            columnPosition = position % columns;
        }
        //行位置，从0开始
        coordinate[0] = rowPosition;
        //列位置，从0开始
        coordinate[1] = columnPosition;
        return coordinate;
    }

    /**
     * @return 找到下一页第一个View的位置
     */
    private int findNextPageFirstPosition() {
        int page = mCurrentPagerIndex;
        ++page;
        if (page >= mPageCount) {
            page = mPageCount - 1;
        }
        return page * mOnePageSize;
    }

    /**
     * @return 找到下一页第一个View的位置
     */
    private int findPrePageFirstPos() {
        int page = mCurrentPagerIndex;
        page--;
        if (page < 0) {
            page = 0;
        }
        return page * mOnePageSize;
    }

    /**
     * @return 当前Recycler是否是静止状态
     */
    private boolean isIdle() {
        return mRecyclerView == null || mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
    }

    /**
     * @param position
     * @return 是否需要换到下一行或列
     */
    private boolean isNeedMoveToNextSpan(int position) {
        return mOrientation == HORIZONTAL ? position % mRows == 0 : position % mColumns == 0;
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getItemCount() == 0) {
            return null;
        }

        return null;
    }

    private RecyclerView.OnChildAttachStateChangeListener enforceChildFillListener() {
        return new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                RecyclerView.LayoutParams layoutParams =
                        (RecyclerView.LayoutParams) view.getLayoutParams();
                if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                        || layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    throw new IllegalStateException(
                            "Pages must fill the whole PagerGridLayoutManager (use match_parent)");
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // nothing
            }
        };
    }

    protected static class LayoutState {

        static final int LAYOUT_START = -1;

        static final int LAYOUT_END = 1;

        static final int ITEM_DIRECTION_HEAD = -1;

        static final int ITEM_DIRECTION_TAIL = 1;

        static final int SCROLLING_OFFSET_NaN = Integer.MIN_VALUE;
        /**
         * 可填充的View空间大小
         */
        private int mAvailable;
        /**
         * 是否需要回收View
         */
        boolean mRecycle;

        int mCurrentPosition;
        /**
         * 遍历Adapter数据的方向
         * 值为 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mItemDirection;
        /**
         * 布局的填充方向
         * 值为 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mLayoutDirection;
        /**
         *
         */
        int mScrollingOffset;

        int mOffSetX;
        int mOffSetY;

        int mLastScrollDelta;


        private View next(RecyclerView.Recycler recycler) {
            View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }
    }

    protected static class LayoutChunkResult {
        public int mConsumed;
        public boolean mFinished;
        public boolean mIgnoreConsumed;
        public boolean mFocusable;

        void resetInternal() {
            mConsumed = 0;
            mFinished = false;
            mIgnoreConsumed = false;
            mFocusable = false;
        }
    }
}
