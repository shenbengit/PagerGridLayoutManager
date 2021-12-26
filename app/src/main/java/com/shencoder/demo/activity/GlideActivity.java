package com.shencoder.demo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.shencoder.MyLinearLayoutManager;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.GlideAdapter;
import com.shencoder.demo.bean.GlideBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class GlideActivity extends AppCompatActivity {
    private static final String TAG = "GlideActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glide);
        RecyclerView rv = findViewById(R.id.rv);
        TextView tvPagerIndex = findViewById(R.id.tvPagerIndex);
        TextView tvPagerCount = findViewById(R.id.tvPagerCount);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(10, 10, 10, 10);
            }
        });
        PagerGridLayoutManager manager = new PagerGridLayoutManager(2, 4, PagerGridLayoutManager.HORIZONTAL);
//        MyLinearLayoutManager manager = new MyLinearLayoutManager(this);
        manager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {
            @Override
            public void onPagerCountChanged(int pagerCount) {
                Log.w(TAG, "onPagerCountChanged-pagerCount:" + pagerCount);
                tvPagerCount.setText(String.valueOf(pagerCount));
            }

            @Override
            public void onPagerIndexSelected(int prePagerIndex, int currentPagerIndex) {
                tvPagerIndex.setText(currentPagerIndex == PagerGridLayoutManager.NO_ITEM ? "-" : String.valueOf(currentPagerIndex + 1));
                Log.w(TAG, "onPagerIndexSelected-prePagerIndex " + prePagerIndex + ",currentPagerIndex:" + currentPagerIndex);
            }
        });

        rv.setLayoutManager(manager);
        GlideAdapter adapter = new GlideAdapter();
        rv.setAdapter(adapter);

        List<GlideBean> list = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            list.add(new GlideBean(String.valueOf(i)));
        }
        adapter.setList(list);
    }
}