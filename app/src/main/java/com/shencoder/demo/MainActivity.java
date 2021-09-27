package com.shencoder.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ShenBen
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private RecyclerView rv;
    private ViewPager2 vp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv = findViewById(R.id.rv);
        vp = findViewById(R.id.vp);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(5, 5, 5, 5);
            }
        });
        PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(2, 3, PagerGridLayoutManager.VERTICAL);
        layoutManager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {

            @Override
            public void onPagerCountChanged(int pagerCount) {
                Log.w(TAG, "onPagerCountChanged: " + pagerCount);
            }

            @Override
            public void onPagerIndexSelected(int prePagerIndex, int currentPagerIndex) {
                Log.w(TAG, "onPagerIndexSelected-prePagerIndex " + prePagerIndex + ",currentPagerIndex:" + currentPagerIndex);
            }
        });
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false);
//        GridLayoutManager layoutManager =new GridLayoutManager(this,4);
        rv.setLayoutManager(layoutManager);
        final TestAdapter adapter = new TestAdapter();
        adapter.setOnItemClickListener((adapter1, view, position) -> Toast.makeText(MainActivity.this, "点击了第" + position + "个位置", Toast.LENGTH_SHORT).show());
        rv.setAdapter(adapter);

        TestAdapter vpAdapter = new TestAdapter();
        vp.setAdapter(vpAdapter);
        List<TestBean> list = new ArrayList<>();
        for (int i = 0; i <= 46; i++) {
            list.add(new TestBean(i, String.valueOf(i)));
        }
        adapter.setList(list);

        vpAdapter.setList(list);
        findViewById(R.id.btnMove).setOnClickListener(v -> {
//            adapter.addData(0,new TestBean(111,"123"));
            adapter.removeAt(adapter.getData().size() - 1);
//            layoutManager.smoothScrollToNextPager();
//            rv.smoothScrollToPosition(30);
//            vp.setCurrentItem(30);
//            adapter.notifyItemChanged(22);
//            vp.setCurrentItem(20);
        });
    }
}