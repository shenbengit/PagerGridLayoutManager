package com.shencoder.demo.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.shencoder.demo.adapter.NestedRecyclerViewAdapter;

import java.util.List;

/**
 * @author ShenBen
 * @date 2021/12/25 13:46
 * @email 714081644@qq.com
 */
public class NestedRecyclerViewBean implements MultiItemEntity {
    private List<GlideBean> list;

    public NestedRecyclerViewBean() {
    }

    public NestedRecyclerViewBean(List<GlideBean> list) {
        this.list = list;
    }

    public List<GlideBean> getList() {
        return list;
    }

    public void setList(List<GlideBean> list) {
        this.list = list;
    }

    @Override
    public int getItemType() {
        return NestedRecyclerViewAdapter.TYPE_RECYCLER_VIEW;
    }
}
