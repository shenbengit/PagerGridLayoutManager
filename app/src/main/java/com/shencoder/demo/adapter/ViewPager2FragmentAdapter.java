package com.shencoder.demo.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.shencoder.demo.bean.MenuBean;
import com.shencoder.demo.fragment.EmptyFragment;
import com.shencoder.demo.fragment.ViewPager2Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ShenBen
 * @date 2021/09/27 20:26
 * @email 714081644@qq.com
 */
public class ViewPager2FragmentAdapter extends FragmentStateAdapter {

    private final List<MenuBean> mData = new ArrayList<>();


    public ViewPager2FragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ViewPager2FragmentAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public ViewPager2FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    public void setNewData(@Nullable List<MenuBean> list) {
        mData.clear();
        if (list != null) {
            mData.addAll(list);
        }
        notifyDataSetChanged();
    }

    public MenuBean getItem(int position) {
        return mData.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MenuBean item = getItem(position);
        return item.isEmpty() ? EmptyFragment.newInstance(item.getTitle()) : ViewPager2Fragment.newInstance();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}
