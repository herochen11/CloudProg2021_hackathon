# This function is used to poll donation messages from SQS and store them into DynamoDB.

import time
import json
import hashlib
import boto3

# AWS DynamoDB
dynamodb_client = boto3.client('dynamodb')
dynamodb_resource = boto3.resource('dynamodb')

def lambda_handler(event, context):
	# Prepare a DynamoDB Table to record the donation information.
	donation_dbtable_name = 'Donation_Records'
	donation_dbtable = get_donation_dbtable(donation_dbtable_name)
	# Parse the messages received from SQS.
	for record in event['Records']:
		payload = record['body']
		payload_dict = json.loads(payload)
		restaurant_name = payload_dict['restaurant_name']
		current_date = time.strftime('%Y-%m-%d', time.gmtime())
		for item in payload_dict['donation_items']:
			product_name = item['product_name']
			donation_count = item['donation_count']
			expiration_date = item['expiration_date']
			# Use MD5 to generate unique ID
			sid_seed = '{}:{}:{}:{}:{}'.format(
				restaurant_name, 
				product_name, 
				str(donation_count), 
				expiration_date, 
				current_date
			)
			md5 = hashlib.md5()
			md5.update(sid_seed.encode('utf-8'))
			# Insert a record into DynamoDB.
			donation_dbtable.put_item(
				Item={
					'sid': md5.hexdigest(),
					'restaurant_name': restaurant_name,
					'product_name': product_name,
					'donation_count': donation_count,
					'expiration_date': expiration_date,
					'submission_date': current_date
				}
			)

	return
		
def get_donation_dbtable(table_name):
	# Check if the DynamoDB Table already exists. If not, create a new one.
	existing_tables = dynamodb_client.list_tables()['TableNames']
	if table_name in existing_tables:
		table = dynamodb_resource.Table(table_name)
	else:
		table = dynamodb_resource.create_table(
			AttributeDefinitions=[
				{
					'AttributeName': 'sid',
					'AttributeType': 'S'
				}
			],
			TableName=table_name,
			KeySchema=[
				{
					'AttributeName': 'sid',
					'KeyType': 'HASH'
				}
			],
			ProvisionedThroughput={
				'ReadCapacityUnits': 60,
				'WriteCapacityUnits': 60
			}
		)
		
	return table
