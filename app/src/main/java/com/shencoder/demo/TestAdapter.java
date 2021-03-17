package com.shencoder.demo;


import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;


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
        Log.i(TAG, "convert: " + (holder.getLayoutPosition() - getHeaderLayoutCount()));
        holder.setText(R.id.tvItem, testBean.getName());
    }
}
