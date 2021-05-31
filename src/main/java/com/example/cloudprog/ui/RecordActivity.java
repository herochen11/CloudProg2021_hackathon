package com.example.cloudprog.ui;

//lab9-2 import
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.regions.Regions;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.util.StringUtils;
import com.example.cloudprog.R;
import com.example.cloudprog.viewmodels.Injection;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RecordActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:158338ef-e40c-4e36-9b77-970a25e8c693",// Identity pool ID
                Regions.US_EAST_1 // Region
        );

        final AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentialsProvider.getCredentials());
        final IdentityManager identityManager = Injection.getAWSService().getIdentityManager();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":rid",new AttributeValue().withS(identityManager.getCachedUserID()));
        ScanRequest scanRequest = new ScanRequest().withTableName("Donation_Records")
                .withFilterExpression("restaurant_name = :rid").withProjectionExpression("sid, product_name,donation_count , expiration_date")
                .withExpressionAttributeValues(expressionAttributeValues);
        ScanResult result = dbClient.scan(scanRequest);

        ListView listView = findViewById(R.id.record_list);
        ArrayList<String> values = new ArrayList<String>();
        int i = 0;
        for (Map item : result.getItems()){
            Log.v("input",item.toString());
            String tmp = "product name: " + item.get("product_name").toString().split(":")[1].split(",")[0];
            String tmp2 = "donation count: " + item.get("donation_count").toString().split(":")[1].split(",")[0];
            String tmp3 = "expiration date: " + item.get("expiration_date").toString().split(":")[1].split(",")[0];
            Log.v("input",tmp);
            values.add(tmp + " " + tmp2 + " " + tmp3);
        }
        Log.v("input",values.toString());
        ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,values);
        listView.setAdapter(adapter);
    }
}
