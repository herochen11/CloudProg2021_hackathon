package com.example.cloudprog.ui;

//lab9-2 import
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.AdminGetUserRequest;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.IdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserRequest;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.example.cloudprog.R;
import com.example.cloudprog.viewmodels.Injection;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:158338ef-e40c-4e36-9b77-970a25e8c693", // Identity pool ID
                Regions.US_EAST_1 // Region
        );

        Button button1 = findViewById(R.id.restaurant_btn);
        button1.setOnClickListener(btn_1_click);

        Button button2 = findViewById(R.id.normal_user_btn);
        button2.setOnClickListener(btn_2_click);

    }
    private View.OnClickListener btn_1_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, RestaurantActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener btn_2_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, NormalActivity.class);
            startActivity(intent);
        }
    };
}
