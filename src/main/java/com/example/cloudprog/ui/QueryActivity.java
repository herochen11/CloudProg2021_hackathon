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

public class QueryActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);

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

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":rid",new AttributeValue().withS(identityManager.getCachedUserID()));
        ScanRequest scanRequest = new ScanRequest().withTableName("warehouse")
                .withProjectionExpression("restaurant_name ,sid, product_name, donation_count, expiration_date");
        ScanResult result = dbClient.scan(scanRequest);

        ListView listView = (ListView) findViewById(R.id.result_list);
        ArrayList<String> values = new ArrayList<String>();
        for (Map item : result.getItems()){
            Log.v("input",item.toString());
            String tmp1 = "Restaurant : " + item.get("restaurant_name").toString().split(":")[1].split(",")[0];
            String tmp2 = "product name : " + item.get("product_name").toString().split(":")[1].split(",")[0];
            String tmp3 = "donation count : " + item.get("donation_count").toString().split(":")[1].split(",")[0];
            String tmp4 = "expiration date : " + item.get("expiration_date").toString().split(":")[1].split(",")[0];
            String tmp5 = "!sid : " + item.get("sid").toString().split(":")[1].split(",")[0];
            Log.v("input",tmp1);
            values.add(tmp1 + ", " + tmp2 + ", " + tmp3 + ", " + tmp4 + " "+ tmp5);
        }
        Log.v("input",values.toString());
        Custom_adapter adapter = new Custom_adapter(values,this,getApplicationContext(),getString(R.string.queue_name),getString(R.string.identity_pool_id));
        listView.setAdapter(adapter);
    }
}
