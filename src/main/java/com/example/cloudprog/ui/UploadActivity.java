package com.example.cloudprog.ui;

//lab9-2 import
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.regions.Regions;

import android.graphics.Bitmap;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.example.cloudprog.R;
import com.example.cloudprog.viewmodels.Injection;


import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.identity_pool_id),// Identity pool ID
                Regions.US_EAST_1 // Region
        );

        final AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentialsProvider.getCredentials());
        final IdentityManager identityManager = Injection.getAWSService().getIdentityManager();

        //final AWSMobileClient client = new AWSMobileClient(credentialsProvider.getCredentials());
        Button addButton = (Button) this.findViewById(R.id.add_btn);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.v("input","test");
                //Todo : Upload a donate to dynamoDB
                EditText food_name = (EditText)findViewById(R.id.food_name);
                EditText food_amount = (EditText)findViewById(R.id.food_amount);
                EditText due_date = (EditText)findViewById(R.id.food_due_date);
                //AWSMobileClient.
                //AWSMobileClient.getInstance().getUsername();
                Log.v("input",food_name.getText().toString());
                Log.v("input",food_amount.getText().toString());
                Log.v("input",due_date.getText().toString());

                String did = null;
                ScanRequest scanRequest = new ScanRequest().withTableName("Donation_Records");
                ScanResult result = dbClient.scan(scanRequest);
                if(result.getCount() == 0){
                    Log.v("input","no result");
                    did = "0";
                }
                else {
                    Log.v("input",result.toString() );
                    Log.v("input",result.getCount().toString());
                    int num = result.getCount();
                    did = Integer.toString(num);
                }
                HashMap<String,AttributeValue> item_values =
                        new HashMap<String,AttributeValue>();
                item_values.put("sid", new AttributeValue(did));
                item_values.put("product_name", new AttributeValue(food_name.getText().toString()));
                item_values.put("expiration_date", new AttributeValue(due_date.getText().toString()));
                item_values.put("donation_count", new AttributeValue(food_amount.getText().toString()));
                item_values.put("restaurant_name", new AttributeValue(identityManager.getCachedUserID()));
                Log.v("input",item_values.toString());

                try {
                    PutItemRequest request = new PutItemRequest("Donation_Records", item_values);
                    dbClient.putItem(request);
                    Toast.makeText(UploadActivity.this, "upload success", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(UploadActivity.this, "upload fail", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
