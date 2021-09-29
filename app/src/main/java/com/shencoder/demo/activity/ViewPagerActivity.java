package com.shencoder.demo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.shencoder.demo.bean.MenuBean;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.ViewPagerFragmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerActivity extends AppCompatActivity {
    public static final String TAG = "TestActivity";
    private TabLayout tabLayout;
    private ViewPager vp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        tabLayout = findViewById(R.id.tabLayout);
        vp = findViewById(R.id.vp);
        ViewPagerFragmentAdapter adapter = new ViewPagerFragmentAdapter(getSupportFragmentManager());
        vp.setAdapter(adapter);
        tabLayout.setupWithViewPager(vp);
        List<MenuBean> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(new MenuBean("Menu" + (i + 1), i != 0));
        }
        adapter.setNewData(list);
    }
}