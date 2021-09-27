package com.shencoder.demo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class TestFragment extends Fragment {
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
        PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(2, 3);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(layoutManager);
        TestAdapter adapter = new TestAdapter();
        rv.setAdapter(adapter);
        List<TestBean> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add(new TestBean(i, String.valueOf(i)));
        }
        adapter.setList(list);
    }
}