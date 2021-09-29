package com.shencoder.demo.adapter;


import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shencoder.demo.R;
import com.shencoder.demo.bean.TestBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;


/**
 * @author ShenBen
 * @date 2021/01/10 17:28
 * @email 714081644@qq.com
 */
public class TestAdapter extends BaseQuickAdapter<TestBean, BaseViewHolder> {
    public static final String TAG = "TestAdapter";

    public TestAdapter() {
        super(R.layout.item_test);
    }

    @Override
    protected void onItemViewHolderCreated(@NonNull BaseViewHolder viewHolder, int viewType) {
        Log.i(TAG, "onItemViewHolderCreated: ");
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, TestBean testBean) {
        int position = holder.getLayoutPosition() - getHeaderLayoutCount();
        Log.i(TAG, "convert: " + position);
        RecyclerView.LayoutManager layoutManager = getRecyclerView().getLayoutManager();
        if (layoutManager instanceof PagerGridLayoutManager) {
            int onePageSize = ((PagerGridLayoutManager) layoutManager).getOnePageSize();
            if (position % onePageSize == 0) {
                holder.setTextColor(R.id.tvItem, Color.RED);
            } else if (position % onePageSize == onePageSize - 1) {
                holder.setTextColor(R.id.tvItem, Color.GREEN);
            } else {
                holder.setTextColor(R.id.tvItem, Color.WHITE);
            }
        } else {
            holder.setTextColor(R.id.tvItem, Color.WHITE);
        }
        holder.setText(R.id.tvItem, testBean.getName());
    }
}
