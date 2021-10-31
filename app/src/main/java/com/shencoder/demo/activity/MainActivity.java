package com.shencoder.demo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.shencoder.demo.BuildConfig;
import com.shencoder.demo.R;
import com.shencoder.demo.adapter.TestAdapter;
import com.shencoder.demo.bean.TestBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RadioGroup rgOrientation = findViewById(R.id.rgOrientation);
        RadioButton rbHorizontal = findViewById(R.id.rbHorizontal);
        RadioButton rbVertical = findViewById(R.id.rbVertical);
        EditText etRows = findViewById(R.id.etRows);
        EditText etColumns = findViewById(R.id.etColumns);
        EditText etPosition = findViewById(R.id.etPosition);
        EditText etPagerIndex = findViewById(R.id.etPagerIndex);

        PagerGridLayoutManager.setDebug(BuildConfig.DEBUG);

        findViewById(R.id.btnVp1).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewPagerActivity.class));
        });
        findViewById(R.id.btnVp2).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewPager2Activity.class));
        });
        RecyclerView rv = findViewById(R.id.rv);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(10, 10, 10, 10);
            }
        });
        TextView tvPagerIndex = findViewById(R.id.tvPagerIndex);
        TextView tvPagerCount = findViewById(R.id.tvPagerCount);
        final PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(
                Integer.parseInt(etRows.getText().toString()),
                Integer.parseInt(etColumns.getText().toString()),
                rbHorizontal.isChecked() ? PagerGridLayoutManager.HORIZONTAL : PagerGridLayoutManager.VERTICAL);
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

        //设置滑动每像素需要花费的时间
        layoutManager.setMillisecondPreInch(70);
        //设置最大滚动时间
        layoutManager.setMaxScrollOnFlingDuration(200);

        rv.setLayoutManager(layoutManager);
        TestAdapter adapter = new TestAdapter();
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            Toast.makeText(this, "点击了位置：" + position, Toast.LENGTH_SHORT).show();
        });
        //长按删除数据
        adapter.setOnItemLongClickListener((adapter12, view12, position) -> {
            Toast.makeText(this, "删除了位置：" + position, Toast.LENGTH_SHORT).show();
            adapter12.removeAt(position);
            return true;
        });
        findViewById(R.id.btnSetRows).setOnClickListener(v -> {
            String string = etRows.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(this, "行数不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            layoutManager.setRows(Integer.parseInt(string));
        });
        rgOrientation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbHorizontal) {
                layoutManager.setOrientation(PagerGridLayoutManager.HORIZONTAL);
            } else if (checkedId == R.id.rbVertical) {
                layoutManager.setOrientation(PagerGridLayoutManager.VERTICAL);
            }
        });
        findViewById(R.id.btnSetColumns).setOnClickListener(v -> {
            String string = etColumns.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(this, "列数不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            layoutManager.setColumns(Integer.parseInt(string));
        });
        findViewById(R.id.btnScrollToPosition).setOnClickListener(v -> {
            String string = etPosition.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(this, "指定位置不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            rv.scrollToPosition(Integer.parseInt(string));
        });
        findViewById(R.id.btnSmoothScrollToPosition).setOnClickListener(v -> {
            String string = etPosition.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(this, "指定位置不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            rv.smoothScrollToPosition(Integer.parseInt(string));
        });
        findViewById(R.id.btnScrollToPagerIndex).setOnClickListener(v -> {
            String string = etPagerIndex.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(this, "指定页不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            layoutManager.scrollToPagerIndex(Integer.parseInt(string));
        });
        findViewById(R.id.btnSmoothScrollToPagerIndex).setOnClickListener(v -> {
            String string = etPagerIndex.getText().toString();
            if (TextUtils.isEmpty(string)) {
                Toast.makeText(MainActivity.this, "指定页不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            layoutManager.smoothScrollToPagerIndex(Integer.parseInt(string));
        });
        findViewById(R.id.btnPrePager).setOnClickListener(v -> layoutManager.scrollToPrePager());
        findViewById(R.id.btnNextPager).setOnClickListener(v -> layoutManager.scrollToNextPager());
        findViewById(R.id.btnSmoothPrePager).setOnClickListener(v -> layoutManager.smoothScrollToPrePager());
        findViewById(R.id.btnSmoothNextPager).setOnClickListener(v -> layoutManager.smoothScrollToNextPager());
        findViewById(R.id.btnAddDataToStart).setOnClickListener(v -> adapter.addData(0, new TestBean(0, "A")));
        findViewById(R.id.btnAddDataToEnd).setOnClickListener(v -> adapter.addData(new TestBean(0, "Z")));
        findViewById(R.id.btnDeleteDataFromStart).setOnClickListener(v -> {
            if (!adapter.getData().isEmpty()) {
                adapter.removeAt(0);
            }
        });
        findViewById(R.id.btnDeleteDataFromEnd).setOnClickListener(v -> {
            if (!adapter.getData().isEmpty()) {
                adapter.removeAt(adapter.getData().size() - 1);
            }
        });
        findViewById(R.id.btnUpdateFirstData).setOnClickListener(v -> {
            if (!adapter.getData().isEmpty()) {
                adapter.getItem(0).setName("我更新了");
                adapter.notifyItemChanged(0);
            }
//            adapter.notifyItemRangeChanged(0, 5);
        });

        List<TestBean> list = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            list.add(new TestBean(i, String.valueOf(i)));
        }
        adapter.setList(list);
    }
}