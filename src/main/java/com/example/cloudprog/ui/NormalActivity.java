package com.example.cloudprog.ui;

//lab9-2 import
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.example.cloudprog.R;
public class NormalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.identity_pool_id), // Identity pool ID
                Regions.US_EAST_1 // Region
        );

        Button button1 = findViewById(R.id.bot_btn);
        button1.setOnClickListener(btn_1_click);

        Button button2 = findViewById(R.id.user_query_btn);
        button2.setOnClickListener(btn_2_click);

    }
        private View.OnClickListener btn_1_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NormalActivity.this, BotActivity.class);
                startActivity(intent);
            }
        };

        private View.OnClickListener btn_2_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NormalActivity.this, QueryActivity.class);
                startActivity(intent);
            }
        };

}
