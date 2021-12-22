package com.shencoder.demo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.shencoder.MyLinearLayoutManager;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.GlideAdapter;
import com.shencoder.demo.bean.GlideBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class GlideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glide);
        RecyclerView rv = findViewById(R.id.rv);
        PagerGridLayoutManager manager = new PagerGridLayoutManager(2, 4);
//        MyLinearLayoutManager manager = new MyLinearLayoutManager(this);
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