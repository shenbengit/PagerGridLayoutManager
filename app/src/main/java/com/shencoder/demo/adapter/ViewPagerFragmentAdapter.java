package com.shencoder.demo.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.shencoder.demo.bean.MenuBean;
import com.shencoder.demo.fragment.EmptyFragment;
import com.shencoder.demo.fragment.NestedRecyclerViewFragment;
import com.shencoder.demo.fragment.ViewPagerFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ShenBen
 * @date 2021/9/29 17:00
 * @email 714081644@qq.com
 */
public class ViewPagerFragmentAdapter extends FragmentPagerAdapter {
    private final List<MenuBean> mData = new ArrayList<>();

    public ViewPagerFragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    public ViewPagerFragmentAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    public void setNewData(@Nullable List<MenuBean> list) {
        mData.clear();
        if (list != null) {
            mData.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        MenuBean item = mData.get(position);
        int i = position % 3;
        switch (i) {
            case 0:
                return ViewPagerFragment.newInstance();
            case 1:
                return NestedRecyclerViewFragment.newInstance();
            default:
                return EmptyFragment.newInstance(item.getTitle());
        }
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mData.get(position).getTitle();
    }
}
