package com.shencoder.demo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class TestFragment extends Fragment {
    private static final String TAG = "TestFragment";

    public static TestFragment newInstance() {
        TestFragment fragment = new TestFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView rv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rv = view.findViewById(R.id.rv);
//        PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(2, 3);
//        layoutManager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {
//            @Override
//            public void onPagerCountChanged(int pagerCount) {
//                Log.w(TAG, "onPagerCountChanged: " + pagerCount);
//            }
//
//            @Override
//            public void onPagerIndexSelected(int prePagerIndex, int currentPagerIndex) {
//                Log.w(TAG, "onPagerIndexSelected-prePagerIndex " + prePagerIndex + ",currentPagerIndex:" + currentPagerIndex);
//            }
//        });
//        layoutManager.setPagerScrollStateListener(new PagerGridLayoutManager.PagerScrollStateListener() {
//            @Override
//            public void onScrollToStart() {
//                Log.i(TAG, "onScrollToStart: ");
//            }
//
//            @Override
//            public void onScrollToEnd() {
//                Log.i(TAG, "onScrollToEnd: ");
//            }
//        });
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(layoutManager);
        TestAdapter adapter = new TestAdapter();
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
//            adapter.removeAt(position);
//            adapter.addData(0, new TestBean(111, "111"));
            Toast.makeText(requireContext(), "点击了位置：" + position, Toast.LENGTH_SHORT).show();
        });
        List<TestBean> list = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            list.add(new TestBean(i, String.valueOf(i)));
        }
        adapter.setList(list);
    }
}