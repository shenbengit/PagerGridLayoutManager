package com.shencoder.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * @author ShenBen
 * @date 2021/11/24 21:04
 * @email 714081644@qq.com
 */
public class MyImageView extends AppCompatImageView {
    private static final String TAG = "MyImageView";

    public MyImageView(@NonNull Context context) {
        super(context);
    }

    public MyImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i(TAG, "onMeasure: ");
    }
}
