package com.example.cloudprog.ui;
import java.util.ArrayList;
import java.io.*;

import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.example.cloudprog.R;
import java.util.List;


import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Custom_adapter extends BaseAdapter {
    private ArrayList<String> list = new ArrayList<String>();
    private Context context;
    private Context activity_context;
    private String sqs_name;
    private String identity_pool_id;
    public Custom_adapter(ArrayList<String> list, Context context,Context context2, String name,String id) {
        this.list = list;
        this.context = context;
        this.activity_context = context2;
        this.sqs_name = name;
        this.identity_pool_id = id;
        Log.v("debug","initialize");
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }
    @Override
    public long getItemId(int pos) {
        return pos;
        //just return 0 if your list items do not have an Id variable.
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.customize_layout, null);
        }

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                activity_context,
                identity_pool_id,// Identity pool ID
                Regions.US_EAST_1 // Region
        );
        final AmazonSQSClient sqsClient = new AmazonSQSClient(credentialsProvider.getCredentials());

        //Handle TextView and display string from your list
        final TextView tvContact= (TextView)view.findViewById(R.id.item);
        tvContact.setText(list.get(position).toString().split("!")[0]);

        //Handle buttons and add onClickListeners
        Button takebtn= (Button)view.findViewById(R.id.take_btn);

        takebtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //do something
                String sid = list.get(position).toString().split("!")[1].split(":")[1];
                String message = "{\"warehouse_name\":\"warehouse\", \"receive_count\":\"1\", \"sid\":\""+sid+"\"}";
                GetQueueUrlResult getQueueUrlResult = sqsClient.getQueueUrl(sqs_name);
                SendMessageRequest sendMessageRequest = new SendMessageRequest(getQueueUrlResult.getQueueUrl(), message);
                sqsClient.sendMessage(sendMessageRequest);

                String new_count = list.get(position).toString().split(",")[2].split(":")[1];
                Log.v("test",new_count.trim());
                int tmp = Integer.parseInt(new_count.trim());
                String i1 = list.get(position).toString().split(",")[0];
                String i2 = list.get(position).toString().split(",")[1];
                String i3 = list.get(position).toString().split(",")[3];
                String i4 = list.get(position).toString().split("!")[1];

                String new_message = i1 + ", " + i2 + ", donation_count : " + Integer.toString(tmp-1) + ", " + i3 + "!" +i4;
                tvContact.setText(new_message.split("!")[0]);
                list.set(position,new_message);
            }
        });


        return view;
    }
}
