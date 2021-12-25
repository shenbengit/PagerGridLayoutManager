package com.shencoder.demo.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.shencoder.demo.adapter.NestedRecyclerViewAdapter;

/**
 * @author ShenBen
 * @date 2021/01/10 17:31
 * @email 714081644@qq.com
 */
public class TestBean implements MultiItemEntity {
    private int id;
    private String name;
    private boolean isChecked;

    public TestBean() {
    }

    public TestBean(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Override
    public int getItemType() {
        return NestedRecyclerViewAdapter.TYPE_NORMAL;
    }
}
