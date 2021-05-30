# This function is used to handle requests of donation reception from SQS and update the information in DynamoDB.

import time
import json
import hashlib
import boto3

# AWS DynamoDB
dynamodb_client = boto3.client('dynamodb')
dynamodb_resource = boto3.resource('dynamodb')

def lambda_handler(event, context):
	# Parse the messages received from SQS.
	for record in event['Records']:
		payload = record['body']
		payload_dict = json.loads(payload)
		product_id = payload_dict['sid']
		receive_count = payload_dict['receive_count']
		warehouse_name = payload_dict['warehouse_name']
		warehouse_dbtable = dynamodb_resource.Table(warehouse_name)
		respond = warehouse_dbtable.update_item(
			Key={'sid': product_id},
			UpdateExpression="set donation_count = donation_count - :val",
			ExpressionAttributeValues={
				':val': receive_count
			},
			ReturnValues="ALL_NEW"
		)
		
	return
