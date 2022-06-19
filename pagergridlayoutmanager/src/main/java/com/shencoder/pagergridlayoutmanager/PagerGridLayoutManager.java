package com.shencoder.pagergridlayoutmanager;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.CallSuper;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
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
    static boolean DEBUG = BuildConfig.DEBUG;
    /**
     * 水平滑动
     */
    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    /**
     * 垂直滑动
     */
    public static final int VERTICAL = RecyclerView.VERTICAL;
    /**
     * @see #mCurrentPagerIndex
     */
    public static final int NO_ITEM = -1;
    public static final int NO_PAGER_COUNT = 0;

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
    private int mPagerCount = NO_PAGER_COUNT;
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
    protected final LayoutState mLayoutState;

    protected final LayoutChunkResult mLayoutChunkResult;
    /**
     * 用于计算锚点坐标
     * {@link #mShouldReverseLayout} 为false：左上角第一个view的位置
     * {@link #mShouldReverseLayout} 为true：右上角第一个view的位置
     */
    private final Rect mStartSnapRect = new Rect();
    /**
     * 用于计算锚点坐标
     * {@link #mShouldReverseLayout} 为false：右下角最后一个view的位置
     * {@link #mShouldReverseLayout} 为true：左上角最后一个view的位置
     */
    private final Rect mEndSnapRect = new Rect();

    private RecyclerView mRecyclerView;
    /**
     * 定义是否应从头到尾计算布局
     *
     * @see #mShouldReverseLayout
     */
    private boolean mReverseLayout = false;
    /**
     * 这保留了 PagerGridLayoutManager 应该如何开始布局视图的最终值。
     * 它是通过检查 {@link #getReverseLayout()} 和 View 的布局方向来计算的。
     */
    protected boolean mShouldReverseLayout = false;

    @Nullable
    private PagerChangedListener mPagerChangedListener;
    /**
     * 计算多出来的宽度，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
     * 不减去的话，会导致翻页计算不触发
     *
     * @see #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)
     */
    private int diffWidth = 0;
    /**
     * 计算多出来的高度，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
     * 不减去的话，会导致翻页计算不触发
     *
     * @see #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)
     */
    private int diffHeight = 0;
    /**
     * 是否启用处理滑动冲突滑动冲突，默认开启
     * 只会在{@link RecyclerView} 在可滑动布局{@link #isInScrollingContainer(View)}中起作用
     */
    private boolean isHandlingSlidingConflictsEnabled = true;
    private float mMillisecondPreInch = PagerGridSmoothScroller.MILLISECONDS_PER_INCH;
    private int mMaxScrollOnFlingDuration = PagerGridSmoothScroller.MAX_SCROLL_ON_FLING_DURATION;

    private final RecyclerView.OnChildAttachStateChangeListener onChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            //判断ItemLayout的宽高是否是match_parent
            if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                    || layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                throw new IllegalStateException("Item layout  must fill the whole PagerGridLayoutManager (use match_parent)");
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(@NonNull View view) {
            // nothing
        }
    };

    private RecyclerView.OnItemTouchListener onItemTouchListener;

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns) {
        this(rows, columns, HORIZONTAL);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, boolean reverseLayout) {
        this(rows, columns, HORIZONTAL, reverseLayout);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, @Orientation int orientation) {
        this(rows, columns, orientation, false);
    }

    public PagerGridLayoutManager(@IntRange(from = 1) int rows, @IntRange(from = 1) int columns, @Orientation int orientation, boolean reverseLayout) {
        mLayoutState = createLayoutState();
        mLayoutChunkResult = createLayoutChunkResult();
        setRows(rows);
        setColumns(columns);
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
    }

    /**
     * print logcat
     *
     * @param debug is debug
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
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
        if (DEBUG) {
            Log.d(TAG, "onAttachedToWindow: ");
        }
        //默认先这么设置
        view.setHasFixedSize(true);
        if (isInScrollingContainer(view)) {
            //在一个可滑动的布局中
            if (isHandlingSlidingConflictsEnabled) {
                onItemTouchListener = new PagerGridItemTouchListener(this, view);
                view.addOnItemTouchListener(onItemTouchListener);
            } else {
                //不启用的话可以自行解决
                if (DEBUG) {
                    Log.w(TAG, "isHandlingSlidingConflictsEnabled: false.");
                }
            }
        }
        view.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
        mPagerGridSnapHelper = new PagerGridSnapHelper();
        mPagerGridSnapHelper.attachToRecyclerView(view);
        mRecyclerView = view;
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

        //重置下宽高，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
        //不减去的话，会导致翻页计算不触发
        diffWidth = realWidth - mItemWidth * mColumns;
        diffHeight = realHeight - mItemHeight * mRows;

        mItemWidthUsed = realWidth - diffWidth - mItemWidth;
        mItemHeightUsed = realHeight - diffHeight - mItemHeight;

        if (DEBUG) {
            Log.d(TAG, "onMeasure-originalWidthSize: " + widthSize + ",originalHeightSize: " + heightSize + ",diffWidth: " + diffWidth + ",diffHeight: " + diffHeight + ",mItemWidth: " + mItemWidth + ",mItemHeight: " + mItemHeight + ",mStartSnapRect:" + mStartSnapRect + ",mEndSnapRect:" + mEndSnapRect);
        }
        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (DEBUG) {
            Log.d(TAG, "onLayoutChildren: " + state.toString());
        }
        int itemCount = getItemCount();
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler);
            setPagerCount(NO_PAGER_COUNT);
            setCurrentPagerIndex(NO_ITEM);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }

        // resolve layout direction
        resolveShouldLayoutReverse();

        //计算锚点的坐标
        if (mShouldReverseLayout) {
            //右上角第一个view的位置
            mStartSnapRect.set(getWidth() - getPaddingEnd() - mItemWidth, getPaddingTop(), getWidth() - getPaddingEnd(), getPaddingTop() + mItemHeight);
            //左下角最后一个view的位置
            mEndSnapRect.set(getPaddingStart(), getHeight() - getPaddingBottom() - mItemHeight, getPaddingStart() + mItemWidth, getHeight() - getPaddingBottom());
        } else {
            //左上角第一个view的位置
            mStartSnapRect.set(getPaddingStart(), getPaddingTop(), getPaddingStart() + mItemWidth, getPaddingTop() + mItemHeight);
            //右下角最后一个view的位置
            mEndSnapRect.set(getWidth() - getPaddingEnd() - mItemWidth, getHeight() - getPaddingBottom() - mItemHeight, getWidth() - getPaddingEnd(), getHeight() - getPaddingBottom());
        }

        //计算总页数
        int pagerCount = itemCount / mOnePageSize;
        if (itemCount % mOnePageSize != 0) {
            ++pagerCount;
        }

        //计算需要补充空间
        mLayoutState.replenishDelta = 0;
        if (pagerCount > 1) {
            //超过一页，计算补充空间距离
            int remain = itemCount % mOnePageSize;
            int replenish = 0;
            if (remain != 0) {
                int i = remain / mColumns;
                int k = remain % mColumns;
                if (mOrientation == HORIZONTAL) {
                    replenish = (i == 0) ? (mColumns - k) * mItemWidth : 0;
                } else {
                    if (k > 0) {
                        ++i;
                    }
                    replenish = (mRows - i) * mItemHeight;
                }
            }
            mLayoutState.replenishDelta = replenish;
        }

        mLayoutState.mRecycle = false;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mAvailable = getEnd();
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;

        int pagerIndex = mCurrentPagerIndex;
        if (pagerIndex == NO_ITEM) {
            pagerIndex = 0;
        } else {
            //取上次PagerIndex和最大MaxPagerIndex中最小值。
            pagerIndex = Math.min(pagerIndex, getMaxPagerIndex());
        }

        View firstView;
        if (!isIdle() && getChildCount() != 0) {
            //滑动中的更新状态
            firstView = getChildClosestToStart();
        } else {
            //没有子view或者不在滑动状态
            firstView = null;
        }

        //计算首个位置的偏移量，主要是为了方便child layout，计算出目标位置的上一个位置的坐标
        int left;
        int top;
        int right;
        int bottom;
        if (mShouldReverseLayout) {
            if (firstView == null) {
                //按页且从右上角开始布局
                mLayoutState.mCurrentPosition = pagerIndex * mOnePageSize;

                int calculateClipOffset = calculateClipOffset(true, mLayoutState.mCurrentPosition);

                if (mOrientation == RecyclerView.HORIZONTAL) {
                    bottom = getHeight() - getPaddingBottom();
                    left = getWidth() - getPaddingEnd() + calculateClipOffset;
                } else {
                    bottom = getPaddingTop() - calculateClipOffset;
                    left = getPaddingStart();
                }
            } else {
                //计算布局偏移量
                int position = getPosition(firstView);
                mLayoutState.mCurrentPosition = position;
                Rect rect = mLayoutState.mOffsetRect;

                int calculateClipOffset = calculateClipOffset(true, mLayoutState.mCurrentPosition);

                getDecoratedBoundsWithMargins(firstView, rect);
                if (mOrientation == RecyclerView.HORIZONTAL) {
                    if (isNeedMoveToNextSpan(position)) {
                        //为了方便计算
                        bottom = getHeight() - getPaddingBottom();
                        left = rect.right + calculateClipOffset;
                    } else {
                        bottom = rect.top;
                        left = rect.left;
                    }
                } else {
                    if (isNeedMoveToNextSpan(position)) {
                        //为了方便计算
                        bottom = rect.top - calculateClipOffset;
                        left = getPaddingStart();
                    } else {
                        bottom = rect.bottom;
                        left = rect.right;
                    }
                }
                //追加额外的滑动空间
                int scrollingOffset;
                if (mOrientation == HORIZONTAL) {
                    scrollingOffset = getDecoratedStart(firstView) - getEndAfterPadding();
                } else {
                    scrollingOffset = getDecoratedStart(firstView);
                }
                mLayoutState.mAvailable -= scrollingOffset;
            }

            top = bottom - mItemHeight;
            right = left + mItemWidth;
        } else {
            if (firstView == null) {
                //按页且从左上角开始布局
                mLayoutState.mCurrentPosition = pagerIndex * mOnePageSize;

                int calculateClipOffset = calculateClipOffset(true, mLayoutState.mCurrentPosition);

                if (mOrientation == RecyclerView.HORIZONTAL) {
                    bottom = getHeight() - getPaddingBottom();
                    right = getPaddingStart() - calculateClipOffset;
                } else {
                    bottom = getPaddingTop() - calculateClipOffset;
                    right = getWidth() - getPaddingEnd();
                }
            } else {
                //计算布局偏移量
                int position = getPosition(firstView);
                mLayoutState.mCurrentPosition = position;
                Rect rect = mLayoutState.mOffsetRect;

                int calculateClipOffset = calculateClipOffset(true, mLayoutState.mCurrentPosition);

                getDecoratedBoundsWithMargins(firstView, rect);
                if (mOrientation == RecyclerView.HORIZONTAL) {
                    if (isNeedMoveToNextSpan(position)) {
                        //为了方便计算
                        bottom = getHeight() - getPaddingBottom();
                        right = rect.left - calculateClipOffset;
                    } else {
                        bottom = rect.top;
                        right = rect.right;
                    }
                } else {
                    if (isNeedMoveToNextSpan(position)) {
                        //为了方便计算
                        bottom = rect.top - calculateClipOffset;
                        right = getWidth() - getPaddingEnd();
                    } else {
                        bottom = rect.bottom;
                        right = rect.left;
                    }
                }
                //追加额外的滑动空间
                int scrollingOffset = getDecoratedStart(firstView);
                mLayoutState.mAvailable -= scrollingOffset;
            }
            top = bottom - mItemHeight;
            left = right - mItemWidth;
        }
        mLayoutState.setOffsetRect(left, top, right, bottom);

        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren-pagerCount:" + pagerCount + ",mLayoutState.mAvailable: " + mLayoutState.mAvailable);
        }

        //回收views
        detachAndScrapAttachedViews(recycler);
        //填充views
        fill(recycler, state);
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size() + ",mLayoutState.replenishDelta:" + mLayoutState.replenishDelta);
        }

        if (firstView == null) {
            //移动状态不更新页数和页码
            setPagerCount(pagerCount);
            setCurrentPagerIndex(pagerIndex);
        }
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {

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
    public int computeHorizontalScrollOffset(@NonNull RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeVerticalScrollOffset(@NonNull RecyclerView.State state) {
        return computeScrollOffset(state);
    }

    @Override
    public int computeHorizontalScrollExtent(@NonNull RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    @Override
    public int computeVerticalScrollExtent(@NonNull RecyclerView.State state) {
        return computeScrollExtent(state);
    }

    @Override
    public int computeVerticalScrollRange(@NonNull RecyclerView.State state) {
        return computeScrollRange(state);
    }

    @Override
    public int computeHorizontalScrollRange(@NonNull RecyclerView.State state) {
        return computeScrollRange(state);
    }

    @Nullable
    @Override
    public Parcelable onSaveInstanceState() {
        if (DEBUG) {
            Log.d(TAG, "onSaveInstanceState: ");
        }
        SavedState state = new SavedState();
        state.mOrientation = mOrientation;
        state.mRows = mRows;
        state.mColumns = mColumns;
        state.mCurrentPagerIndex = mCurrentPagerIndex;
        state.mReverseLayout = mReverseLayout;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mOrientation = savedState.mOrientation;
            mRows = savedState.mRows;
            mColumns = savedState.mColumns;
            calculateOnePageSize();
            setCurrentPagerIndex(savedState.mCurrentPagerIndex);
            mReverseLayout = savedState.mReverseLayout;
            requestLayout();
            if (DEBUG) {
                Log.d(TAG, "onRestoreInstanceState: loaded saved state");
            }
        }
    }

    @Override
    public void scrollToPosition(int position) {
        assertNotInLayoutOrScroll(null);

        //先找到目标position所在第几页
        int pagerIndex = getPagerIndexByPosition(position);
        scrollToPagerIndex(pagerIndex);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        assertNotInLayoutOrScroll(null);

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
    public final int getWidth() {
        return super.getWidth() - getDiffWidth();
    }

    @Override
    public final int getHeight() {
        return super.getHeight() - getDiffHeight();
    }

    @Override
    @CallSuper
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (DEBUG) {
            Log.w(TAG, "onDetachedFromWindow: ");
        }
        if (mRecyclerView != null) {
            if (onItemTouchListener != null) {
                mRecyclerView.removeOnItemTouchListener(onItemTouchListener);
            }
            mRecyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
            mRecyclerView = null;
        }
        mPagerGridSnapHelper.attachToRecyclerView(null);
        mPagerGridSnapHelper = null;
        //这里不能置为null，因为在ViewPager2嵌套Fragment使用，
        //部分情况下Fragment不回调onDestroyView，但会导致onDetachedFromWindow触发。
        //所以如果想置null，请调用{@link #setPagerChangedListener(null)}
//        mPagerChangedListener = null;
    }

    /**
     * 设置监听回调
     *
     * @param listener
     */
    public void setPagerChangedListener(@Nullable PagerChangedListener listener) {
        mPagerChangedListener = listener;
    }

    /**
     * 是否启用处理滑动冲突滑动冲突，默认true
     * 这个方法必须要在{@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)} 之前调用，否则无效
     * you must call this method before {@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)}
     *
     * @param enabled 是否启用
     * @see #isInScrollingContainer(View)
     * @see #onAttachedToWindow(RecyclerView)
     */
    public final void setHandlingSlidingConflictsEnabled(boolean enabled) {
        isHandlingSlidingConflictsEnabled = enabled;
    }

    public final boolean isHandlingSlidingConflictsEnabled() {
        return isHandlingSlidingConflictsEnabled;
    }

    /**
     * 设置滑动每像素需要花费的时间，不可过小，不然可能会出现划过再回退的情况
     * 默认值：{@link PagerGridSmoothScroller#MILLISECONDS_PER_INCH}
     * <p>
     * set millisecond pre inch. not too small.
     * default value: {@link PagerGridSmoothScroller#MILLISECONDS_PER_INCH}
     *
     * @param millisecondPreInch 值越大，滚动速率越慢，反之
     * @see PagerGridSmoothScroller#calculateSpeedPerPixel(DisplayMetrics)
     */
    public final void setMillisecondPreInch(@FloatRange(from = 1) float millisecondPreInch) {
        mMillisecondPreInch = Math.max(1f, millisecondPreInch);
    }

    /**
     * @return 滑动每像素需要花费的时间
     * @see PagerGridSmoothScroller#calculateSpeedPerPixel(DisplayMetrics)
     */
    public final float getMillisecondPreInch() {
        return mMillisecondPreInch;
    }

    /**
     * 设置最大滚动时间，如果您想此值无效，请使用{@link Integer#MAX_VALUE}
     * 默认值：{@link PagerGridSmoothScroller#MAX_SCROLL_ON_FLING_DURATION}，单位：毫秒
     * <p>
     * set max scroll on fling duration.If you want this value to expire, use {@link Integer#MAX_VALUE}
     * default value: {@link PagerGridSmoothScroller#MAX_SCROLL_ON_FLING_DURATION},Unit: ms
     *
     * @param maxScrollOnFlingDuration 值越大，滑动时间越长，滚动速率越慢，反之
     * @see PagerGridSmoothScroller#calculateTimeForScrolling(int)
     */
    public final void setMaxScrollOnFlingDuration(@IntRange(from = 1) int maxScrollOnFlingDuration) {
        mMaxScrollOnFlingDuration = Math.max(1, maxScrollOnFlingDuration);
    }

    /**
     * @return 最大滚动时间
     * @see PagerGridSmoothScroller#calculateTimeForScrolling(int)
     */
    public final int getMaxScrollOnFlingDuration() {
        return mMaxScrollOnFlingDuration;
    }

    public final int getItemWidth() {
        return mItemWidth;
    }

    public final int getItemHeight() {
        return mItemHeight;
    }

    /**
     * 计算一页的数量
     */
    private void calculateOnePageSize() {
        mOnePageSize = mRows * mColumns;
    }

    /**
     * @return 一页的数量
     */
    @IntRange(from = 1)
    public final int getOnePageSize() {
        return mOnePageSize;
    }

    public void setColumns(@IntRange(from = 1) int columns) {
        assertNotInLayoutOrScroll(null);

        if (mColumns == columns) {
            return;
        }
        mColumns = Math.max(columns, 1);
        mPagerCount = NO_PAGER_COUNT;
        mCurrentPagerIndex = NO_ITEM;
        calculateOnePageSize();
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
        assertNotInLayoutOrScroll(null);

        if (mRows == rows) {
            return;
        }
        mRows = Math.max(rows, 1);
        mPagerCount = NO_PAGER_COUNT;
        mCurrentPagerIndex = NO_ITEM;
        calculateOnePageSize();
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
        assertNotInLayoutOrScroll(null);

        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation:" + orientation);
        }
        if (orientation != mOrientation) {
            mOrientation = orientation;

            requestLayout();
        }
    }

    @Orientation
    public int getOrientation() {
        return mOrientation;
    }

    public void setReverseLayout(boolean reverseLayout) {
        assertNotInLayoutOrScroll(null);

        if (reverseLayout == mReverseLayout) {
            return;
        }
        mReverseLayout = reverseLayout;
        requestLayout();
    }

    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    /**
     * @param position position
     * @return 获取当前position所在页下标
     */
    public final int getPagerIndexByPosition(int position) {
        return position / mOnePageSize;
    }

    /**
     * @return 获取最大页数
     */
    public final int getMaxPagerIndex() {
        return getPagerIndexByPosition(getItemCount() - 1);
    }

    /**
     * 直接滚到第几页
     *
     * @param pagerIndex 第几页
     */
    public void scrollToPagerIndex(@IntRange(from = 0) int pagerIndex) {
        assertNotInLayoutOrScroll(null);

        //先找到目标position所在第几页
        pagerIndex = Math.min(Math.max(pagerIndex, 0), getMaxPagerIndex());
        if (pagerIndex == mCurrentPagerIndex) {
            //同一页直接return
            return;
        }
        setCurrentPagerIndex(pagerIndex);
        requestLayout();
    }

    /**
     * 直接滚动到上一页
     */
    public void scrollToPrePager() {
        assertNotInLayoutOrScroll(null);

        scrollToPagerIndex(mCurrentPagerIndex - 1);
    }

    /**
     * 直接滚动到下一页
     */
    public void scrollToNextPager() {
        assertNotInLayoutOrScroll(null);

        scrollToPagerIndex(mCurrentPagerIndex + 1);
    }

    /**
     * 平滑滚到第几页，为避免长时间滚动，会预先跳转到就近位置，默认3页
     *
     * @param pagerIndex 第几页，下标从0开始
     */
    public void smoothScrollToPagerIndex(@IntRange(from = 0) int pagerIndex) {
        assertNotInLayoutOrScroll(null);

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
            PagerGridSmoothScroller smoothScroller = new PagerGridSmoothScroller(mRecyclerView, this);
            smoothScroller.setTargetPosition(getPositionByPagerIndex(pagerIndex, isLayoutToEnd));
            startSmoothScroll(smoothScroller);
        }
    }

    /**
     * 平滑到上一页
     */
    public void smoothScrollToPrePager() {
        assertNotInLayoutOrScroll(null);

        smoothScrollToPagerIndex(mCurrentPagerIndex - 1);
    }

    /**
     * 平滑到下一页
     */
    public void smoothScrollToNextPager() {
        assertNotInLayoutOrScroll(null);

        smoothScrollToPagerIndex(mCurrentPagerIndex + 1);
    }

    protected LayoutState createLayoutState() {
        return new LayoutState();
    }

    protected LayoutChunkResult createLayoutChunkResult() {
        return new LayoutChunkResult();
    }

    protected boolean isLayoutRTL() {
        return getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
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
     * @return -1：{@link #getItemCount()} is 0,{@link #NO_ITEM} . else {@link #mCurrentPagerIndex}
     */
    @IntRange(from = -1)
    public final int getCurrentPagerIndex() {
        return mCurrentPagerIndex;
    }

    /**
     * 由于View类中这个方法无法使用，直接copy处理
     *
     * @param view
     * @return 判断view是不是处在一个可滑动的布局中
     * @see ViewGroup#shouldDelayChildPressedState()
     */
    private boolean isInScrollingContainer(View view) {
        ViewParent p = view.getParent();
        while (p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
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

    public final int getDiffWidth() {
        return Math.max(diffWidth, 0);
    }

    public final int getDiffHeight() {
        return Math.max(diffHeight, 0);
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
            if (mShouldReverseLayout) {
                reverseLayoutChunk(recycler, state, layoutState, layoutChunkResult);
            } else {
                layoutChunk(recycler, state, layoutState, layoutChunkResult);
            }
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
            if (mShouldReverseLayout) {
                reverseLayoutChunk(recycler, state, layoutState, layoutChunkResult);
            } else {
                layoutChunk(recycler, state, layoutState, layoutChunkResult);
            }
        }
        //回收View
        recycleViews(recycler);
        return start - layoutState.mAvailable;
    }

    /**
     * 正项布局
     *
     * @param recycler
     * @param state
     * @param layoutState
     * @param layoutChunkResult
     * @see #layoutChunk(RecyclerView.Recycler, RecyclerView.State, LayoutState, LayoutChunkResult)
     * @see #mShouldReverseLayout
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
        layoutState.mCurrentPosition = layoutToEnd ? layoutState.getNextPosition(position, mOrientation, mRows, mColumns, state) :
                layoutState.getPrePosition(position, mOrientation, mRows, mColumns, state);
        measureChildWithMargins(view, mItemWidthUsed, mItemHeightUsed);
        //是否需要换行或者换列
        boolean isNeedMoveSpan = layoutToEnd ? isNeedMoveToNextSpan(position) : isNeedMoveToPreSpan(position);
        layoutChunkResult.mConsumed = isNeedMoveSpan ? mOrientation == HORIZONTAL ? mItemWidth : mItemHeight : 0;

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
                    left = rect.left + mItemWidth + calculateClipOffset(true, position);
                    top = getPaddingTop();
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
                    left = rect.left - mItemWidth - calculateClipOffset(false, position);
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
            if (layoutToEnd) {
                //向下填充，绘制方向：从左到右
                if (isNeedMoveSpan) {
                    //下一行绘制，从头部开始
                    left = getPaddingStart();
                    top = rect.bottom + calculateClipOffset(true, position);
                } else {
                    //当前行绘制
                    left = rect.left + mItemWidth;
                    top = rect.top;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
            } else {
                //向上填充，绘制方向：从右到左
                if (isNeedMoveSpan) {
                    //上一行绘制，从尾部开始
                    right = getWidth() - getPaddingEnd();
                    left = right - mItemWidth;
                    bottom = rect.top - calculateClipOffset(false, position);
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
     * 反向布局
     *
     * @param recycler
     * @param state
     * @param layoutState
     * @param layoutChunkResult
     * @see #layoutChunk(RecyclerView.Recycler, RecyclerView.State, LayoutState, LayoutChunkResult)
     * @see #mShouldReverseLayout
     */
    private void reverseLayoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState, LayoutChunkResult layoutChunkResult) {
        //仅处理水平反向滑动，垂直仅改变排列顺序
        boolean layoutToEnd = layoutState.mLayoutDirection == LayoutState.LAYOUT_END;

        int position = layoutState.mCurrentPosition;
        View view = layoutState.next(recycler);
        if (layoutToEnd) {
            addView(view);
        } else {
            addView(view, 0);
        }
        layoutState.mCurrentPosition = layoutToEnd ? layoutState.getNextPosition(position, mOrientation, mRows, mColumns, state) :
                layoutState.getPrePosition(position, mOrientation, mRows, mColumns, state);
        measureChildWithMargins(view, mItemWidthUsed, mItemHeightUsed);
        //是否需要换行或者换列
        boolean isNeedMoveSpan = layoutToEnd ? isNeedMoveToNextSpan(position) : isNeedMoveToPreSpan(position);
        layoutChunkResult.mConsumed = isNeedMoveSpan ? mOrientation == HORIZONTAL ? mItemWidth : mItemHeight : 0;

        //记录的上一个View的位置
        Rect rect = layoutState.mOffsetRect;
        int left;
        int top;
        int right;
        int bottom;
        if (mOrientation == HORIZONTAL) {
            //水平滑动
            if (layoutToEnd) {
                //向前填充，绘制方向：从上到下
                if (isNeedMoveSpan) {
                    //上一列绘制，从头部开始
                    left = rect.left - mItemWidth - calculateClipOffset(true, position);
                    top = getPaddingTop();
                } else {
                    //当前列绘制
                    left = rect.left;
                    top = rect.bottom;
                }
                right = left + mItemWidth;
                bottom = top + mItemHeight;
            } else {
                //向后填充，绘制方向：从下到上
                if (isNeedMoveSpan) {
                    //下一列绘制，从底部开启
                    left = rect.left + mItemWidth + calculateClipOffset(false, position);
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
            if (layoutToEnd) {
                //向下填充，绘制方向：从右到左
                if (isNeedMoveSpan) {
                    //下一行绘制，从尾部开始
                    right = getWidth() - getPaddingEnd();
                    top = rect.bottom + calculateClipOffset(true, position);
                } else {
                    //当前行绘制，向前布局
                    right = rect.left;
                    top = rect.top;
                }
                left = right - mItemWidth;
                bottom = top + mItemHeight;
            } else {
                //向上填充，绘制方向：从左到右
                if (isNeedMoveSpan) {
                    //上一行绘制，从头部开始
                    left = getPaddingStart();
                    right = left + mItemWidth;
                    bottom = rect.top - calculateClipOffset(false, position);
                    top = bottom - mItemHeight;
                } else {
                    //当前行绘制，向后布局
                    left = rect.right;
                    right = left + mItemWidth;
                    top = rect.top;
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
        final int layoutDirection;
        if (shouldHorizontallyReverseLayout()) {
            layoutDirection = delta > 0 ? LayoutState.LAYOUT_START : LayoutState.LAYOUT_END;
        } else {
            layoutDirection = delta > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        }
        mLayoutState.mLayoutDirection = layoutDirection;
        boolean layoutToEnd = layoutDirection == LayoutState.LAYOUT_END;
        final int absDelta = Math.abs(delta);
        if (DEBUG) {
            Log.i(TAG, "scrollBy -> before : childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size() + ",delta:" + delta);
        }
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

        //回收view，此步骤在移动之后
        recycleViews(recycler);
        if (DEBUG) {
            Log.i(TAG, "scrollBy -> end : childCount:" + getChildCount() + ",recycler.scrapList.size:" + recycler.getScrapList().size() + ",delta:" + delta + ",scrolled:" + scrolled);
        }
        return scrolled;
    }

    private void updateLayoutState(boolean layoutToEnd, int requiredSpace,
                                   boolean canUseExistingSpace, RecyclerView.State state) {
        View child;
        //计算在不添加新view的情况下可以滚动多少（与布局无关）
        int scrollingOffset;
        if (layoutToEnd) {
            child = getChildClosestToEnd();
            if (shouldHorizontallyReverseLayout()) {
                scrollingOffset = -getDecoratedStart(child) + getStartAfterPadding();
            } else {
                scrollingOffset = getDecoratedEnd(child) - getEndAfterPadding();
            }
        } else {
            child = getChildClosestToStart();
            if (shouldHorizontallyReverseLayout()) {
                scrollingOffset = getDecoratedEnd(child) - getEndAfterPadding();
            } else {
                scrollingOffset = -getDecoratedStart(child) + getStartAfterPadding();
            }
        }
        getDecoratedBoundsWithMargins(child, mLayoutState.mOffsetRect);

        mLayoutState.mCurrentPosition = layoutToEnd ? mLayoutState.getNextPosition(getPosition(child), mOrientation, mRows, mColumns, state) :
                mLayoutState.getPrePosition(getPosition(child), mOrientation, mRows, mColumns, state);

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
        if (shouldHorizontallyReverseLayout()) {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                //水平向右或者垂直向下滑动
                recycleViewsFromStart(recycler);
            } else {
                //水平向左或者垂直向上滑动
                recycleViewsFromEnd(recycler);
            }
        } else {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                //水平向左或者垂直向上滑动
                recycleViewsFromEnd(recycler);
            } else {
                //水平向右或者垂直向下滑动
                recycleViewsFromStart(recycler);
            }
        }
    }

    private void recycleViewsFromStart(RecyclerView.Recycler recycler) {
        //如果clipToPadding==false，则不计算padding
        boolean clipToPadding = getClipToPadding();
        int start = clipToPadding ? getStartAfterPadding() : 0;
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedEnd(childAt);
                if (decorated >= start) {
                    continue;
                }
                if (DEBUG) {
                    Log.w(TAG, "recycleViewsFromStart-removeAndRecycleViewAt: " + i + ", position: " + getPosition(childAt));
                }
                removeAndRecycleViewAt(i, recycler);
//                removeAndRecycleView(childAt, recycler);
            }
        }
    }

    private void recycleViewsFromEnd(RecyclerView.Recycler recycler) {
        //如果clipToPadding==false，则不计算padding
        boolean clipToPadding = getClipToPadding();
        int end = clipToPadding ? getEndAfterPadding() : (mOrientation == HORIZONTAL ? getWidth() : getHeight());
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                int decorated = getDecoratedStart(childAt);
                if (decorated <= end) {
                    continue;
                }
                if (DEBUG) {
                    Log.w(TAG, "recycleViewsFromEnd-removeAndRecycleViewAt: " + i + ", position: " + getPosition(childAt));
                }
                removeAndRecycleViewAt(i, recycler);
//                removeAndRecycleView(childAt, recycler);
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
        return mOrientation == HORIZONTAL ? getWidth() - getPaddingEnd() : getHeight() - getPaddingBottom();
    }

    private int getStartAfterPadding() {
        return mOrientation == HORIZONTAL ? getPaddingStart() : getPaddingTop();
    }

    private int getClipToPaddingSize() {
        return mOrientation == HORIZONTAL ? getPaddingStart() + getPaddingEnd() : getPaddingTop() + getPaddingBottom();
    }

    /**
     * 计算{@link #getClipToPadding()}==false时偏移量
     *
     * @param layoutToEnd 是否是向后布局
     * @param position    position
     * @return offset
     */
    private int calculateClipOffset(boolean layoutToEnd, int position) {
        boolean clipToPadding = getClipToPadding();
        return !clipToPadding && (position % mOnePageSize == (layoutToEnd ? 0 : mOnePageSize - 1)) ? getClipToPaddingSize() : 0;
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
        if (mOrientation == HORIZONTAL) {
            int surplus = position % mOnePageSize;
            int rowIndex = surplus / mColumns;
            //是否在最后一行
            return rowIndex == 0;
        } else {
            return position % mColumns == 0;
        }
    }

    /**
     * @param position
     * @return 是否需要换到上一行或列
     */
    private boolean isNeedMoveToPreSpan(int position) {
        if (mOrientation == HORIZONTAL) {
            int surplus = position % mOnePageSize;
            //在第几行
            int rowIndex = surplus / mColumns;
            //是否在第一行
            return rowIndex == mRows - 1;
        } else {
            return position % mColumns == mColumns - 1;
        }
    }

    private int computeScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }
        View firstView = getChildAt(0);
        if (firstView == null) {
            return 0;
        }
        int position = getPosition(firstView);
        final float avgSize = (float) getEnd() / (mOrientation == HORIZONTAL ? mColumns : mRows);
        int index;
        if (mOrientation == HORIZONTAL) {
            //所在第几列
            int pagerIndex = getPagerIndexByPosition(position);
            index = pagerIndex * mColumns + position % mColumns;
        } else {
            //所在第几行
            index = position / mColumns;
        }
        int scrollOffset;
        if (shouldHorizontallyReverseLayout()) {
            int scrollRange = computeScrollRange(state) - computeScrollExtent(state);
            scrollOffset = scrollRange - Math.round(index * avgSize + (getDecoratedEnd(firstView) - getEndAfterPadding()));
        } else {
            scrollOffset = Math.round(index * avgSize + (getStartAfterPadding() - getDecoratedStart(firstView)));
        }
        if (DEBUG) {
            Log.i(TAG, "computeScrollOffset: " + scrollOffset);
        }
        return scrollOffset;
    }

    private int computeScrollExtent(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }
        int scrollExtent = getEnd();
        if (DEBUG) {
            Log.i(TAG, "computeScrollExtent: " + scrollExtent);
        }
        return scrollExtent;
    }

    private int computeScrollRange(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }
        int scrollRange = Math.max(mPagerCount, 0) * getEnd();
        if (DEBUG) {
            Log.i(TAG, "computeScrollRange: " + scrollRange);
        }
        return scrollRange;
    }

    private void resolveShouldLayoutReverse() {
        if (mOrientation == VERTICAL || !isLayoutRTL()) {
            mShouldReverseLayout = mReverseLayout;
        } else {
            //水平滑动且是RTL
            mShouldReverseLayout = !mReverseLayout;
        }
    }

    boolean getShouldReverseLayout() {
        return mShouldReverseLayout;
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

    /**
     * @return 是否水平方向反转布局
     */
    boolean shouldHorizontallyReverseLayout() {
        return mShouldReverseLayout && mOrientation == HORIZONTAL;
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
                if (position % mOnePageSize == 0) {
                    firstSnapPosition = position;
                    break;
                }
            }
        }
        if (firstSnapPosition == RecyclerView.NO_POSITION) {
            return null;
        }
        float direction = targetPosition < firstSnapPosition ? -1f : 1f;
        if (shouldHorizontallyReverseLayout()) {
            direction = -direction;
        }
        if (DEBUG) {
            Log.w(TAG, "computeScrollVectorForPosition-firstSnapPosition: " + firstSnapPosition + ", targetPosition:" + targetPosition + ",mOrientation :" + mOrientation + ", direction:" + direction);
        }
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
            PagerGridSmoothScroller smoothScroller = new PagerGridSmoothScroller(mRecyclerView, mLayoutManager);
            smoothScroller.setTargetPosition(mPosition);
            mLayoutManager.startSmoothScroll(smoothScroller);
        }
    }

    protected static class LayoutState {

        protected static final int LAYOUT_START = -1;

        protected static final int LAYOUT_END = 1;


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
        protected final Rect mOffsetRect = new Rect();
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
            mOffsetRect.set(left, top, right, bottom);
        }

        protected View next(RecyclerView.Recycler recycler) {
            return recycler.getViewForPosition(mCurrentPosition);
        }

        protected boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * @param currentPosition 当前的位置
         * @param orientation     方向
         * @param rows            行数
         * @param columns         列数
         * @param state           状态
         * @return 下一个位置
         */
        protected int getNextPosition(int currentPosition, int orientation, int rows, int columns, RecyclerView.State state) {
            int position;
            int onePageSize = rows * columns;
            if (orientation == HORIZONTAL) {
                int surplus = currentPosition % onePageSize;
                //水平滑动
                //向后追加item
                if (surplus == onePageSize - 1) {
                    //一页的最后一个位置
                    position = currentPosition + 1;
                } else {
                    //在第几列
                    int columnsIndex = currentPosition % columns;
                    //在第几行
                    int rowIndex = surplus / columns;
                    //是否在最后一行
                    boolean isLastRow = rowIndex == rows - 1;
                    if (isLastRow) {
                        position = currentPosition - rowIndex * columns + 1;
                    } else {
                        position = currentPosition + columns;
                        if (position >= state.getItemCount()) {
                            //越界了
                            if (columnsIndex != columns - 1) {
                                //如果不是最后一列，计算换行位置
                                position = currentPosition - rowIndex * columns + 1;
                            }
                        }
                    }
                }
            } else {
                //垂直滑动
                position = currentPosition + 1;
            }
            return position;
        }

        /**
         * @param currentPosition 当前的位置
         * @param orientation     方向
         * @param rows            行数
         * @param columns         列数
         * @param state           状态
         * @return 上一个位置
         */
        protected int getPrePosition(int currentPosition, int orientation, int rows, int columns, RecyclerView.State state) {
            int position;
            int onePageSize = rows * columns;
            if (orientation == HORIZONTAL) {
                int surplus = currentPosition % onePageSize;
                //水平滑动
                //向前追加item
                if (surplus == 0) {
                    //一页的第一个位置
                    position = currentPosition - 1;
                } else {
                    //在第几行
                    int rowIndex = surplus / columns;
                    //是否在第一行
                    boolean isFirstRow = rowIndex == 0;
                    if (isFirstRow) {
                        position = currentPosition - 1 + (rows - 1) * columns;
                    } else {
                        position = currentPosition - columns;
                    }
                }
            } else {
                //垂直滑动
                position = currentPosition - 1;
            }
            return position;
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

    /**
     * @see RecyclerView.LayoutManager#onSaveInstanceState()
     * @see RecyclerView.LayoutManager#onRestoreInstanceState(Parcelable)
     */
    protected static class SavedState implements Parcelable {
        /**
         * 当前滑动方向
         */
        protected int mOrientation;
        /**
         * 行数
         */
        protected int mRows;
        /**
         * 列数
         */
        protected int mColumns;
        /**
         * 当前页码下标
         * 从0开始
         */
        protected int mCurrentPagerIndex = NO_ITEM;

        protected boolean mReverseLayout = false;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mOrientation);
            dest.writeInt(this.mRows);
            dest.writeInt(this.mColumns);
            dest.writeInt(this.mCurrentPagerIndex);
        }

        public void readFromParcel(Parcel source) {
            this.mOrientation = source.readInt();
            this.mRows = source.readInt();
            this.mColumns = source.readInt();
            this.mCurrentPagerIndex = source.readInt();
        }

        public SavedState() {
        }

        protected SavedState(Parcel in) {
            this.mOrientation = in.readInt();
            this.mRows = in.readInt();
            this.mColumns = in.readInt();
            this.mCurrentPagerIndex = in.readInt();
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        @NonNull
        @Override
        public String toString() {
            return "SavedState{" +
                    "mOrientation=" + mOrientation +
                    ", mRows=" + mRows +
                    ", mColumns=" + mColumns +
                    ", mCurrentPagerIndex=" + mCurrentPagerIndex +
                    '}';
        }
    }

    public interface PagerChangedListener {
        /**
         * 页面总数量变化
         *
         * @param pagerCount 页面总数，从1开始，为0时说明无数据，{{@link #NO_PAGER_COUNT}}
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
