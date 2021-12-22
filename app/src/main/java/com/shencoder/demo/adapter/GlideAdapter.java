package com.shencoder.demo.adapter;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shencoder.demo.GlideApp;
import com.shencoder.demo.R;
import com.shencoder.demo.bean.GlideBean;

/**
 * @author ShenBen
 * @date 2021/11/24 20:30
 * @email 714081644@qq.com
 */
public class GlideAdapter extends BaseQuickAdapter<GlideBean, BaseViewHolder> {

    public GlideAdapter() {
        super(R.layout.item_glide);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, GlideBean glideBean) {
        holder.setText(R.id.tv, glideBean.getTitle());
        ImageView iv = holder.getView(R.id.iv);
        //wrap_content
//        iv.setImageResource(holder.getLayoutPosition() % 2 == 0 ? R.mipmap.ic_launcher_round : R.drawable.bbb);
        GlideApp.with(iv)
                .load(holder.getLayoutPosition() % 2 == 0 ? "https://img0.baidu.com/it/u=3339583410,2877781326&fm=253&fmt=auto&app=120&f=JPEG" : "https://img2.baidu.com/it/u=805055865,2254304384&fm=253&fmt=auto&app=120&f=JPEG")
//                .override(60, 60)
                .into(iv);
    }
}
