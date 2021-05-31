package com.example.cloudprog.ui;

//lab9-2 import
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import com.example.cloudprog.R;

public class RestaurantActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        Button button1 = findViewById(R.id.donate_btn);
        button1.setOnClickListener(btn_1_click);

        Button button2 = findViewById(R.id.record_btn);
        button2.setOnClickListener(btn_2_click);
    }

    private View.OnClickListener btn_1_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RestaurantActivity.this, UploadActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener btn_2_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RestaurantActivity.this, RecordActivity.class);
            startActivity(intent);
        }
    };


}
