package com.shencoder.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author ShenBen
 * @date 2021/09/27 21:37
 * @email 714081644@qq.com
 */
public class MyRecyclerView extends RecyclerView {
    public static final String TAG = "MyRecyclerView";

    public MyRecyclerView(@NonNull Context context) {
        super(context);
    }

    public MyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean intercept = super.onInterceptTouchEvent(e);
        Log.i(TAG, "onInterceptTouchEvent: " + intercept);
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean touch = super.onTouchEvent(e);
        Log.i(TAG, "onTouchEvent-action: " + e.getAction() + ",touch: " + touch);
        return touch;

    }
}
