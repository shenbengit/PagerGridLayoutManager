package com.shencoder.pagergridlayoutmanager;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    /**
     * 是否启用Debug
     */
    public static final boolean DEBUG = true;
    /**
     * 水平滑动
     */
    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    /**
     * 垂直滑动
     */
    public static final int VERTICAL = RecyclerView.VERTICAL;
    private static final int NO_ITEM = -1;

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
    private int mPagerCount = NO_ITEM;
    /**
     * 当前页码下标
     * 从0开始
     */
    private int mCurrentPagerIndex = NO_ITEM;
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
     * 用于保存一些状态
     */
    private LayoutState mLayoutState = new LayoutState();

    private final LayoutChunkResult mLayoutChunkResult = new LayoutChunkResult();
    /**
     * 用于计算锚点坐标-左上角第一个view的位置
     */
    private final Rect mStartSnapRect = new Rect();
    /**
     * 用于计算锚点坐标-右下角最后一个view的位置
     */
    private final Rect mEndSnapRect = new Rect();

    private RecyclerView mRecyclerView;

    @Nullable
    private PagerChangedListener mPagerChangedListener;

    private final RecyclerView.OnChildAttachStateChangeListener onChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            //判断ItemLayout的宽高是否是match_parent
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

    private final RecyclerView.OnItemTouchListener onItemTouchListener = new RecyclerView.OnItemTouchListener() {
        private int mLastTouchX;
        private int mLastTouchY;

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            int action = e.getAction();
//            switch (action) {
//                case MotionEvent.ACTION_DOWN:
//                    rv.getParent().requestDisallowInterceptTouchEvent(true);
//                    break;
//                case MotionEvent.ACTION_UP:
//                    rv.getParent().requestDisallowInterceptTouchEvent(false);
//
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    break;
//            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    };

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns) {
        this(rows, columns, HORIZONTAL);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, @Orientation int orientation) {
        mRows = Math.max(rows, 1);
        mColumns = Math.max(columns, 1);
        setOrientation(orientation);
    }

    /**
     * @return 子布局LayoutParams，默认全部填充，子布局会根据{@link #mRows}和{@link #mColumns} 均分RecyclerView
     */
    @Override
    public final RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof RecyclerView.LayoutParams) {
            return new LayoutParams((RecyclerView.LayoutParams) lp);
        } else if (lp instanceof ViewGroup.MarginLayoutParams) {
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
        view.addOnItemTouchListener(onItemTouchListener);
        view.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
        mPagerGridSnapHelper = new PagerGridSnapHelper();
        mPagerGridSnapHelper.attachToRecyclerView(view);
        mRecyclerView = view;
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec) {
        int widthMode = View.MeasureSpec.getMode(widthSpec);
        int heightMode = View.MeasureSpec.getMode(heightSpec);
        //判断RecyclerView的宽度和高度是不是精确值
        if (widthMode != View.MeasureSpec.EXACTLY || heightMode != View.MeasureSpec.EXACTLY) {
            throw new IllegalStateException("RecyclerView's width and height must be exactly");
        }
        int widthSize = View.MeasureSpec.getSize(widthSpec);
        int heightSize = View.MeasureSpec.getSize(heightSpec);

        int realWidth = widthSize - getPaddingStart() - getPaddingEnd();
        int realHeight = heightSize - getPaddingTop() - getPaddingBottom();
        //均分宽
        mItemWidth = mColumns > 0 ? realWidth / mColumns : 0;
        //均分高
        mItemHeight = mRows > 0 ? realHeight / mRows : 0;

        mItemWidthUsed = realWidth - mItemWidth;
        mItemHeightUsed = realHeight - mItemHeight;

        int diffWidth = widthSize - mItemWidth * mColumns;
        int diffHeight = heightSize - mItemHeight * mRows;

        if (DEBUG) {
            Log.d(TAG, "onMeasure- originalWidthSize: " + widthSize + ",originalHeightSize: " + heightSize + ",diffWidth: " + diffWidth + ",diffHeight: " + diffHeight + ",mItemWidth: " + mItemWidth + ",mItemHeight: " + mItemHeight);
        }

        //重置下宽高，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
        //不减去的话，会导致翻页计算不触发
        super.onMeasure(
                recycler,
                state,
                View.MeasureSpec.makeMeasureSpec(widthSize - diffWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightSize - diffHeight, View.MeasureSpec.EXACTLY)
        );
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (DEBUG) {
            Log.d(TAG, "onLayoutChildren: " + state.toString());
        }
        int itemCount = getItemCount();
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler);
            setPagerCount(0);
            setCurrentPagerIndex(NO_ITEM);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        mOnePageSize = mRows * mColumns;

        //计算总页数
        int pagerCount = itemCount / mOnePageSize;
        if (itemCount % mOnePageSize != 0) {
            ++pagerCount;
        }
        if (DEBUG) {
            Log.i(TAG, "pagerCount:" + pagerCount);
        }

        //计算需要补充空间
        mLayoutState.replenishDelta = 0;
        if (pagerCount > 1) {
            //超过一页，计算补充空间距离
            int remain = itemCount % mOnePageSize;
            int replenish = 0;
            if (remain != 0) {
                if (mOrientation == HORIZONTAL) {
                    int i = remain / mRows;
                    int k = remain % mRows;
                    if (k != 0) {
                        ++i;
                    }
                    replenish = (mColumns - i) * mItemWidth;
                } else {
                    int i = remain / mColumns;
                    int k = remain % mColumns;
                    if (k != 0) {
                        ++i;
                    }
                    replenish = (mRows - i) * mItemHeight;
                }
            }
            mLayoutState.replenishDelta = replenish;
        }
        int pagerIndex = mCurrentPagerIndex;
        if (pagerIndex == NO_ITEM) {
            pagerIndex = 0;
        }
        int maxPagerIndex = getMaxPagerIndex();
        if (pagerIndex > maxPagerIndex) {
            //如果之前的PagerIndex大于最大的PagerSize
            pagerIndex = maxPagerIndex;
        }

        mLayoutState.mCurrentPosition = pagerIndex * mOnePageSize;

        mLayoutState.mRecycle = false;
        mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mAvailable = getEnd();
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;
        //计算首个位置的偏移量，主要是为了方便child layout
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

        mStartSnapRect.set(getPaddingStart(), getPaddingTop(), getPaddingStart() + mItemWidth, getPaddingTop() + mItemHeight);
        mEndSnapRect.set(getWidth() - getPaddingEnd() - mItemWidth, getHeight() - getPaddingBottom() - mItemHeight, getWidth() - getPaddingEnd(), getHeight() - getPaddingBottom());

        //回收views
        detachAndScrapAttachedViews(recycler);
        //填充views
        fill(recycler, state);
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size() + ",mLayoutState.replenishDelta:" + mLayoutState.replenishDelta);
        }
        setPagerCount(pagerCount);
        setCurrentPagerIndex(pagerIndex);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {

    }

//    @Nullable
//    @Override
//    public View findViewByPosition(int position) {
//        final int childCount = getChildCount();
//        if (childCount == 0) {
//            return null;
//        }
//        final int firstChild = getPosition(getChildAt(0));
//        final int viewPosition = position - firstChild;
//        if (viewPosition >= 0 && viewPosition < childCount) {
//            final View child = getChildAt(viewPosition);
//            if (getPosition(child) == position) {
//                return child;
//            }
//        }
//        return super.findViewByPosition(position);
//    }


    @Override
    public int computeHorizontalScrollRange(@NonNull RecyclerView.State state) {
        return super.computeHorizontalScrollRange(state);
    }

    @Override
    public int computeHorizontalScrollExtent(@NonNull RecyclerView.State state) {
        return super.computeHorizontalScrollExtent(state);
    }

    @Override
    public int computeHorizontalScrollOffset(@NonNull RecyclerView.State state) {
        return super.computeHorizontalScrollOffset(state);
    }


    @Override
    public int computeVerticalScrollRange(@NonNull RecyclerView.State state) {
        return super.computeVerticalScrollRange(state);
    }

    @Override
    public int computeVerticalScrollExtent(@NonNull RecyclerView.State state) {
        return super.computeVerticalScrollExtent(state);
    }

    @Override
    public int computeVerticalScrollOffset(@NonNull RecyclerView.State state) {
        return super.computeVerticalScrollOffset(state);
    }

    @Override
    public void scrollToPosition(int position) {
        if (!isIdle()) {
            return;
        }
        //先找到目标position所在第几页
        int pagerIndex = getPagerIndexByPosition(position);
        scrollToPagerIndex(pagerIndex);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (!isIdle()) {
            return;
        }
        //先找到目标position所在第几页
        int pagerIndex = getPagerIndexByPosition(position);
        smoothScrollToPagerIndex(pagerIndex);
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
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE://静止状态

                break;
            case RecyclerView.SCROLL_STATE_DRAGGING://手指拖拽

                break;
            case RecyclerView.SCROLL_STATE_SETTLING://自由滚动

                break;
        }
    }

    @Override
    public final boolean canScrollHorizontally() {
        return mOrientation == RecyclerView.HORIZONTAL;
    }

    @Override
    public final boolean canScrollVertically() {
        return mOrientation == RecyclerView.VERTICAL;
    }

    @Override
    @CallSuper
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (mRecyclerView != null) {
            mRecyclerView.removeOnItemTouchListener(onItemTouchListener);
            mRecyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
        }
        mPagerGridSnapHelper.attachToRecyclerView(null);
        mRecyclerView = null;
        mPagerChangedListener = null;
    }

    public void setPagerChangedListener(@Nullable PagerChangedListener listener) {
        mPagerChangedListener = listener;
    }

    /**
     * 一页的数量
     *
     * @return
     */
    public final int getOnePageSize() {
        return mOnePageSize;
    }

    public void setColumns(@IntRange(from = 1) int columns) {
        if (!isIdle()) {
            return;
        }
        if (mColumns == columns) {
            return;
        }
        mPagerCount = NO_ITEM;
        mCurrentPagerIndex = NO_ITEM;
        mColumns = columns;
        requestLayout();
    }

    /**
     * @return 列数
     */
    @IntRange(from = 1)
    public final int getColumns() {
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
        mPagerCount = NO_ITEM;
        mCurrentPagerIndex = NO_ITEM;
        requestLayout();
    }

    /**
     * @return 行数
     */
    @IntRange(from = 1)
    public final int getRows() {
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
            mPagerCount = NO_ITEM;
            mCurrentPagerIndex = NO_ITEM;

            requestLayout();
        }
    }

    /**
     * @param position position
     * @return 获取当前position所在页下标
     */
    public final int getPagerIndexByPosition(int position) {
        return position / mOnePageSize;
    }

    /**
     * 直接滚到第几页
     *
     * @param pagerIndex 第几页
     */
    public void scrollToPagerIndex(@IntRange(from = 0) int pagerIndex) {
        if (!isIdle()) {
            return;
        }
        //先找到目标position所在第几页
        pagerIndex = Math.min(pagerIndex, getMaxPagerIndex());
        if (pagerIndex == mCurrentPagerIndex) {
            //同一页直接return
            return;
        }
        setCurrentPagerIndex(pagerIndex);
        requestLayout();
    }

    public void scrollToPrePager() {

    }

    public void scrollToNextPager() {

    }

    /**
     * 平滑滚到第几页，为避免长时间滚动，会预先跳转到就近位置，默认3页
     *
     * @param pagerIndex 第几页，下标从0开始
     */
    public void smoothScrollToPagerIndex(@IntRange(from = 0) int pagerIndex) {
        if (!isIdle()) {
            return;
        }
        pagerIndex = Math.min(Math.max(pagerIndex, 0), getMaxPagerIndex());
        int previousIndex = mCurrentPagerIndex;
        if (pagerIndex == previousIndex) {
            //同一页直接return
            return;
        }
        boolean isLayoutToEnd = pagerIndex > previousIndex;

        if (Math.abs(pagerIndex - previousIndex) > 3) {
            //先就近直接跳转
            int transitionIndex = pagerIndex > previousIndex ? pagerIndex - 3 : pagerIndex + 3;
            scrollToPagerIndex(transitionIndex);

            if (mRecyclerView != null) {
                mRecyclerView.post(new SmoothScrollToPosition(getPositionByPagerIndex(pagerIndex, isLayoutToEnd), this, mRecyclerView));
            }
        } else {
            PagerGridSmoothScroller smoothScroller = new PagerGridSmoothScroller(mRecyclerView);
            smoothScroller.setTargetPosition(getPositionByPagerIndex(pagerIndex, isLayoutToEnd));
            startSmoothScroll(smoothScroller);
        }
    }

    /**
     * 平滑到上一页
     */
    public void smoothScrollToPrePager() {
        if (!isIdle()) {
            return;
        }
        smoothScrollToPagerIndex(mCurrentPagerIndex - 1);
    }

    /**
     * 平滑到下一页
     */
    public void smoothScrollToNextPager() {
        if (!isIdle()) {
            return;
        }
        smoothScrollToPagerIndex(mCurrentPagerIndex + 1);
    }

    /**
     * 设置总页数
     *
     * @param pagerCount
     */
    private void setPagerCount(int pagerCount) {
        if (mPagerCount == pagerCount) {
            return;
        }
        mPagerCount = pagerCount;
        if (mPagerChangedListener != null) {
            mPagerChangedListener.onPagerCountChanged(pagerCount);
        }
    }

    /**
     * 返回总页数
     *
     * @return 0：{@link #getItemCount()} is 0
     */
    @IntRange(from = 0)
    public final int getPagerCount() {
        return Math.max(mPagerCount, 0);
    }

    /**
     * 设置当前页码
     *
     * @param pagerIndex 页码
     */
    private void setCurrentPagerIndex(int pagerIndex) {
        if (mCurrentPagerIndex == pagerIndex) {
            return;
        }
        int prePagerIndex = mCurrentPagerIndex;
        mCurrentPagerIndex = pagerIndex;
        if (mPagerChangedListener != null) {
            mPagerChangedListener.onPagerIndexSelected(prePagerIndex, pagerIndex);
        }
    }

    /**
     * 获取当前的页码
     *
     * @return -1：{@link #getItemCount()} is 0
     * @since {{@link #NO_ITEM}}
     */
    @IntRange(from = -1)
    public final int getCurrentPagerIndex() {
        return mCurrentPagerIndex;
    }

    /**
     * 根据页码下标获取position
     *
     * @param pagerIndex    页码
     * @param isLayoutToEnd true:页的第一个位置，false:页的最后一个位置
     * @return
     */
    private int getPositionByPagerIndex(int pagerIndex, boolean isLayoutToEnd) {
        return isLayoutToEnd ? pagerIndex * mOnePageSize : pagerIndex * mOnePageSize + mOnePageSize - 1;
    }

    /**
     * @return 获取最大页数
     */
    private int getMaxPagerIndex() {
        return getPagerIndexByPosition(getItemCount() - 1);
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
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            layoutState.mAvailable -= layoutChunkResult.mConsumed;
            remainingSpace -= layoutChunkResult.mConsumed;
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
        recycleViews(recycler);
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
     * @param delta    手指滑动的距离
     * @param recycler
     * @param state
     * @return
     */
    private int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || delta == 0 || mPagerCount == 1) {
            return 0;
        }
        mLayoutState.mRecycle = true;
        final int layoutDirection = delta > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        mLayoutState.mLayoutDirection = layoutDirection;
        boolean layoutToEnd = layoutDirection == LayoutState.LAYOUT_END;
        final int absDelta = Math.abs(delta);
        updateLayoutState(layoutToEnd, absDelta, true, state);
        int consumed = mLayoutState.mScrollingOffset + fill(recycler, state);
        if (layoutToEnd) {
            //向后滑动，添加补充距离
            consumed += mLayoutState.replenishDelta;
        }
        if (consumed < 0) {
            return 0;
        }
        //是否已经完全填充到头部或者尾部，滑动的像素>消费的像素
        boolean isOver = absDelta > consumed;
        //计算实际可移动值
        int scrolled = isOver ? layoutDirection * consumed : delta;
        //移动
        offsetChildren(-scrolled);
        mLayoutState.mLastScrollDelta = scrolled;
        if (DEBUG) {
            Log.i(TAG, "scrollBy: childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size() + ",delta:" + delta + ",scrolled:" + scrolled);
        }
        return scrolled;
    }

    private void updateLayoutState(boolean layoutToEnd, int requiredSpace,
                                   boolean canUseExistingSpace, RecyclerView.State state) {
        mLayoutState.mItemDirection = layoutToEnd ? LayoutState.ITEM_DIRECTION_TAIL : LayoutState.ITEM_DIRECTION_HEAD;
        View child;
        //计算在不添加新view的情况下可以滚动多少（与布局无关）
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
        int start = getPaddingStart();
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedEnd(childAt);
                if (decorated > start) {
                    continue;
                }
                removeAndRecycleView(childAt, recycler);
            }
        }
    }

    private void recycleViewsFromEnd(RecyclerView.Recycler recycler) {
        int end = getEndAfterPadding();
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedStart(childAt);
                if (decorated < end) {
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

    /**
     * @return 左上角第一个view的位置
     */
    final Rect getStartSnapRect() {
        return mStartSnapRect;
    }

    /**
     * @return 右下角最后一个view的位置
     */
    final Rect getEndSnapRect() {
        return mEndSnapRect;
    }

    /**
     * 根据下标计算页码
     *
     * @param position
     */
    final void calculateCurrentPagerIndexByPosition(int position) {
        setCurrentPagerIndex(getPagerIndexByPosition(position));
    }

    final LayoutState getLayoutState() {
        return mLayoutState;
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        int firstSnapPosition = RecyclerView.NO_POSITION;
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int position = getPosition(childAt);
                if (position % getOnePageSize() == 0) {
                    firstSnapPosition = position;
                    break;
                }
            }
        }
        if (firstSnapPosition == RecyclerView.NO_POSITION) {
            return null;
        }
        float direction = targetPosition < firstSnapPosition ? -1f : 1f;
        if (mOrientation == HORIZONTAL) {
            return new PointF(direction, 0f);
        } else {
            return new PointF(0f, direction);
        }
    }

    /**
     * 自定义LayoutParams
     */
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

    private static class SmoothScrollToPosition implements Runnable {
        private final int mPosition;
        @NonNull
        private final PagerGridLayoutManager mLayoutManager;
        @NonNull
        private final RecyclerView mRecyclerView;

        SmoothScrollToPosition(int position, @NonNull PagerGridLayoutManager layoutManager, @NonNull RecyclerView recyclerView) {
            mPosition = position;
            mLayoutManager = layoutManager;
            mRecyclerView = recyclerView;
        }

        @Override
        public void run() {
            PagerGridSmoothScroller smoothScroller = new PagerGridSmoothScroller(mRecyclerView);
            smoothScroller.setTargetPosition(mPosition);
            mLayoutManager.startSmoothScroll(smoothScroller);
        }
    }

    protected static class LayoutState {

        protected static final int LAYOUT_START = -1;

        protected static final int LAYOUT_END = 1;

        protected static final int ITEM_DIRECTION_HEAD = -1;

        protected static final int ITEM_DIRECTION_TAIL = 1;

        protected static final int SCROLLING_OFFSET_NaN = Integer.MIN_VALUE;

        /**
         * 可填充的View空间大小
         */
        protected int mAvailable;
        /**
         * 是否需要回收View
         */
        protected boolean mRecycle;

        protected int mCurrentPosition;
        /**
         * 遍历Adapter数据的方向
         * 值为 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        protected int mItemDirection;
        /**
         * 布局的填充方向
         * 值为 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        protected int mLayoutDirection;
        /**
         * 在滚动状态下构造布局状态时使用。
         * 它应该设置我们可以在不创建新视图的情况下进行滚动量。
         * 有效的视图回收需要设置
         */
        protected int mScrollingOffset;
        /**
         * 开始绘制的坐标位置
         */
        protected Rect mOffsetRect;
        /**
         * 最近一次的滑动数量
         */
        protected int mLastScrollDelta;
        /**
         * 需要补充滑动的距离
         */
        protected int replenishDelta;

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
        protected int mConsumed;
        protected boolean mFinished;
        protected boolean mIgnoreConsumed;
        protected boolean mFocusable;

        protected void resetInternal() {
            mConsumed = 0;
            mFinished = false;
            mIgnoreConsumed = false;
            mFocusable = false;
        }
    }

    public interface PagerChangedListener {
        /**
         * 页面总数量变化
         *
         * @param pagerCount 页面总数
         */
        void onPagerCountChanged(@IntRange(from = 0) int pagerCount);

        /**
         * 选中的页面下标
         *
         * @param prePagerIndex     上次的页码，当{{@link #getItemCount()}}为0时，为-1，{{@link #NO_ITEM}}
         * @param currentPagerIndex 当前的页码，当{{@link #getItemCount()}}为0时，为-1，{{@link #NO_ITEM}}
         */
        void onPagerIndexSelected(@IntRange(from = -1) int prePagerIndex, @IntRange(from = -1) int currentPagerIndex);
    }
}
