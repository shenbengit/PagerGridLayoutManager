package com.shencoder.demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.NestedRecyclerViewAdapter;
import com.shencoder.demo.bean.GlideBean;
import com.shencoder.demo.bean.NestedRecyclerViewBean;
import com.shencoder.demo.bean.TestBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵌套RecyclerView
 *
 * @author ShenBen
 * @date 2021/12/25 13:14
 * @email 714081644@qq.com
 */
public class NestedRecyclerViewFragment extends Fragment {

    public static NestedRecyclerViewFragment newInstance() {
        Bundle args = new Bundle();
        NestedRecyclerViewFragment fragment = new NestedRecyclerViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nested_recycler, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = view.findViewById(R.id.rv);
        NestedRecyclerViewAdapter adapter = new NestedRecyclerViewAdapter();
        rv.setAdapter(adapter);

        List<MultiItemEntity> list = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            if (i == 3) {
                List<GlideBean> glideBeans = new ArrayList<>();
                for (int j = 0; j < 15; j++) {
                    glideBeans.add(new GlideBean(String.valueOf(j)));
                }
                list.add(new NestedRecyclerViewBean(glideBeans));
            } else {
                list.add(new TestBean(i, String.valueOf(i)));
            }
        }
        adapter.setList(list);

    }
}
