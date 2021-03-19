package com.shencoder.pagergridlayoutmanager;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {
    }

    private PagerGridSnapHelper mPagerGridSnapHelper;
    /**
     * 当前滑动方向
     */
    @Orientation
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

    private final RecyclerView.OnChildAttachStateChangeListener onChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                    || layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                throw new IllegalStateException("item layout must use match_parent");
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(@NonNull View view) {
            // nothing

        }
    };

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns) {
        this(rows, columns, HORIZONTAL);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, @Orientation int orientation) {
        mRows = rows;
        mColumns = columns;
        setOrientation(orientation);
    }

    /**
     * @return 子布局LayoutParams，默认全部填充
     */
    @Override
    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
        mRecyclerView.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
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

        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: getRealWidth:" + getRealWidth() + ",getRealHeight:" + getRealHeight() + ",mItemWidth:" + mItemWidth + ",mItemHeight:" + mItemHeight + ",mPageCount:" + mPageCount);
        }
        ensureLayoutState();

        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            mCurrentPagerIndex = getPageIndexByPosition(mPendingScrollPosition);
            mCurrentPagerAnchorPosition = mCurrentPagerIndex * mOnePageSize;
            mLayoutState.mCurrentPosition = mCurrentPagerIndex * mOnePageSize;
        } else {
            mCurrentPagerIndex = 0;
            mCurrentPagerAnchorPosition = 0;
            mLayoutState.mCurrentPosition = 0;
        }

        mLayoutState.mRecycle = false;
        mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mAvailable = getEnd();
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;
        //计算首个位置的偏移量
        int left;
        int top;
        int right;
        int bottom;
        if (mOrientation == RecyclerView.HORIZONTAL) {
            bottom = getHeight() - getPaddingBottom();
            right = getPaddingLeft();
        } else {
            bottom = getPaddingTop();
            right = getWidth() - getPaddingRight();
        }
        top = bottom - mItemHeight;
        left = right - mItemWidth;

        mLayoutState.setOffsetRect(left, top, right, bottom);

        fill(recycler, state);

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
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        final int firstChild = getPosition(getChildAt(0));
        final int viewPosition = position - firstChild;
        if (viewPosition >= 0 && viewPosition < childCount) {
            final View child = getChildAt(viewPosition);
            if (getPosition(child) == position) {
                return child;
            }
        }
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
//                Log.i(TAG, "onScrollStateChanged: 总共滑动的距离: " + mScrollDelta);
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
        mRecyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
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
    public void setOrientation(@Orientation int orientation) {
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
        }
        boolean layoutToEnd = layoutState.mLayoutDirection == LayoutState.LAYOUT_END;
        //因为最后一列或者一行可能只绘制了收尾的一个，补满
        while (layoutState.hasMore(state)) {
            boolean isNeedMoveSpan = layoutToEnd ? isNeedMoveToNextSpan(layoutState.mCurrentPosition) : isNeedMoveToPreSpan(layoutState.mCurrentPosition);
            if (isNeedMoveSpan) {
                //如果需要切换行或列，直接退出
                break;
            }
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
        }
        //回收View
        if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
            recycleViews(recycler);
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
        //是否需要换行或者换列
        boolean isNeedMoveSpan = layoutToEnd ? isNeedMoveToNextSpan(position) : isNeedMoveToPreSpan(position);

        layoutChunkResult.mConsumed = isNeedMoveSpan ? (mOrientation == HORIZONTAL ? mItemWidth : mItemHeight) : 0;

        //记录的上一个View的位置
        Rect rect = layoutState.mOffsetRect;
        int left;
        int top;
        int right;
        int bottom;
        if (mOrientation == HORIZONTAL) {
            //水平滑动
            if (layoutToEnd) {
                //向后填充，绘制方向：从上到下
                if (isNeedMoveSpan) {
                    //下一列绘制，从头部开始
                    left = rect.left + mItemWidth;
                    top = getPaddingLeft();
                } else {
                    //当前列绘制
                    left = rect.left;
                    top = rect.bottom;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
            } else {
                //向前填充，绘制方向：从下到上
                if (isNeedMoveSpan) {
                    //上一列绘制，从底部开启
                    left = rect.left - mItemWidth;
                    bottom = getHeight() - getPaddingBottom();
                } else {
                    //当前列绘制
                    left = rect.left;
                    bottom = rect.top;
                }
                top = bottom - mItemHeight;
                right = left + mItemWidth;
            }
        } else {
            //垂直滑动
            if (layoutToEnd) {
                //向后填充，绘制方向：从左到右
                if (isNeedMoveSpan) {
                    //下一行绘制，从头部开始
                    left = getPaddingLeft();
                    top = rect.bottom;
                } else {
                    //当前行绘制
                    left = rect.left + mItemWidth;
                    top = rect.top;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
            } else {
                //向前填充，绘制方向：从右到左
                if (isNeedMoveSpan) {
                    //上一行绘制，从尾部开始
                    right = getWidth() - getPaddingRight();
                    left = right - mItemWidth;
                    bottom = rect.top;
                    top = bottom - mItemHeight;
                } else {
                    //当前行绘制
                    left = rect.left - mItemWidth;
                    top = rect.top;
                    right = left + mItemWidth;
                    bottom = top + mItemHeight;
                }
            }
        }

        layoutState.setOffsetRect(left, top, right, bottom);
        layoutDecoratedWithMargins(view, left, top, right, bottom);
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
        } else {
            child = getChildClosestToStart();
            scrollingOffset = -getDecoratedStart(child) + getStartAfterPadding();
        }
        getDecoratedBoundsWithMargins(child, mLayoutState.mOffsetRect);
        mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
        mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutState.mAvailable -= scrollingOffset;
        }
        mLayoutState.mScrollingOffset = scrollingOffset;
    }

    private View getChildClosestToEnd() {
        return getChildAt(getChildCount() - 1);
    }

    private View getChildClosestToStart() {
        return getChildAt(0);
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
        final LayoutParams params = (LayoutParams) child.getLayoutParams();
        return mOrientation == HORIZONTAL ? getDecoratedRight(child) + params.rightMargin : getDecoratedBottom(child) + params.bottomMargin;
    }

    private int getDecoratedStart(View child) {
        final LayoutParams params = (LayoutParams) child.getLayoutParams();
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

    /**
     * @param position
     * @return 是否需要换到下一行或列
     */
    private boolean isNeedMoveToPreSpan(int position) {
        return mOrientation == HORIZONTAL ? position % mRows == mRows - 1 : position % mColumns == mColumns - 1;
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getItemCount() == 0) {
            return null;
        }

        return null;
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

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

        Rect mOffsetRect;

        int mLastScrollDelta;

        protected LayoutState() {
        }

        protected void setOffsetRect(int left, int top, int right, int bottom) {
            if (mOffsetRect == null) {
                mOffsetRect = new Rect();
            }
            mOffsetRect.set(left, top, right, bottom);
        }

        protected View next(RecyclerView.Recycler recycler) {
            View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        protected boolean hasMore(RecyclerView.State state) {
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
