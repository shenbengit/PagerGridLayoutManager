package com.shencoder.demo.fragment;

import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.shencoder.demo.R;
import com.shencoder.demo.adapter.TestAdapter;
import com.shencoder.demo.bean.TestBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class ViewPager2Fragment extends Fragment {
    private static final String TAG = "ViewPager2Fragment";

    public static ViewPager2Fragment newInstance() {
        ViewPager2Fragment fragment = new ViewPager2Fragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView rv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_pager2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rv = view.findViewById(R.id.rv);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(10, 10, 10, 10);
            }
        });
        TextView tvPagerIndex = view.findViewById(R.id.tvPagerIndex);
        TextView tvPagerCount = view.findViewById(R.id.tvPagerCount);
        final PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(3, 3, PagerGridLayoutManager.HORIZONTAL);
        layoutManager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {
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

        /*
         是否启用处理滑动冲突滑动冲突，default: true；若不需要库中自带的处理方式，则置为false，自行处理。
         setHandlingSlidingConflictsEnabled() 必须要在{@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)} 之前调用，否则无效
         you must call this method before {@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)}
        */
        layoutManager.setHandlingSlidingConflictsEnabled(true);

        rv.setLayoutManager(layoutManager);
        TestAdapter adapter = new TestAdapter();
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            Toast.makeText(requireContext(), "点击了位置：" + position, Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            Toast.makeText(requireContext(), "点击了位置：" + position, Toast.LENGTH_SHORT).show();
        });

        List<TestBean> list = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            list.add(new TestBean(i, String.valueOf(i)));
        }
        adapter.setList(list);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.w(TAG, "onDestroyView: ");
    }
}