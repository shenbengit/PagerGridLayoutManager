package com.shencoder.demo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.shencoder.demo.bean.MenuBean;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.ViewPager2FragmentAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ShenBen
 */
public class ViewPager2Activity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private ViewPager2 vp;
    private TabLayout tabLayout;
    private TabLayoutMediator mediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager2);
        tabLayout = findViewById(R.id.tabLayout);
        vp = findViewById(R.id.vp);
        ViewPager2FragmentAdapter adapter = new ViewPager2FragmentAdapter(this);
        vp.setAdapter(adapter);

        mediator = new TabLayoutMediator(tabLayout, vp, (tab, position) -> tab.setText(adapter.getItem(position).getTitle()));
        mediator.attach();

        List<MenuBean> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(new MenuBean("Menu" + (i + 1), i != 0));
        }
        adapter.setNewData(list);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediator.detach();
    }
}