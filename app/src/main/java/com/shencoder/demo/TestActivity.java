package com.shencoder.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.github.chrisbanes.photoview.PhotoView;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        PhotoView imageView = findViewById(R.id.photoView);
    }
}