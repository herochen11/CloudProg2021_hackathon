# Periodly scan Donation Events and assign tasks to Logistics Department

import time
import json
import hashlib
import boto3

# AWS DynamoDB
dynamodb_resource = boto3.resource('dynamodb', 'us-east-1')
dynamodb_client = boto3.client('dynamodb', 'us-east-1')
# AWS SQS
sqs_client = boto3.client('sqs', 'us-east-1')

def query_recently_donation():
	# Scan Cooperative Restaurants to get the list of restaurants
	cooperative_dbtable_name = 'Cooperative_Restaurants'
	cooperative_dbtable = dynamodb_resource.Table(cooperative_dbtable_name)
	cooperative_list = []
	last_evaluated_key = None
	while True:
		if last_evaluated_key:
			response = cooperative_dbtable.scan(ExclusiveStartKey=last_evaluated_key)
		else:
			response = cooperative_dbtable.scan()
		last_evaluated_key = response.get('LastEvaluatedKey')
		cooperative_list.extend(response['Items'])
		if not last_evaluated_key:
			break
	# Query all available donation information
	donation_dbtable_name = 'Donation_Records'
	donation_dbtable = dynamodb_resource.Table(donation_dbtable_name)
	donation_list = []
	last_evaluated_key = None
	while True:
		if last_evaluated_key:
			response = donation_dbtable.scan(ExclusiveStartKey=last_evaluated_key)
		else:
			response = donation_dbtable.scan()
		last_evaluated_key = response.get('LastEvaluatedKey')
		donation_list.extend(response['Items'])
		if not last_evaluated_key:
			break
	# Partition the donation information based on its provider (restuarant)
	restuarant_donation_dict = {}
	for restuarant in cooperative_list:
		restuarant_donation_info = {}
		restuarant_donation_info['restaurant_name'] = restuarant['name']
		restuarant_donation_info['restaurant_address'] = restuarant['address']
		restuarant_donation_info['restaurant_phone'] = restuarant['phone']
		restuarant_donation_info['restaurant_email'] = restuarant['email']
		restuarant_donation_info['donation_items'] = []
		restuarant_donation_dict[restuarant['name']] = restuarant_donation_info.copy()
		
	for item in donation_list:
		item_copy = {
			'sid': item['sid'], 
			'product_name': item['product_name'],
			'donation_count': int(item['donation_count']),
			'expiration_date': item['expiration_date'],
			'submission_date': item['submission_date']
		}
		restuarant_donation_dict[item['restaurant_name']]['donation_items'].append(item_copy)
	
	donation_info = []
	for key in restuarant_donation_dict.keys():
		if len(restuarant_donation_dict[key]['donation_items']) > 0:
			donation_info.append(restuarant_donation_dict[key].copy())
		
	return donation_info

def donation_distribution(donation_info):
	# Scan Destination Warehouses to get the list of warehouses
	destination_dbtable_name = 'Destination_Warehouses'
	destination_dbtable = dynamodb_resource.Table(destination_dbtable_name)
	destination_list = []
	last_evaluated_key = None
	while True:
		if last_evaluated_key:
			response = destination_dbtable.scan(ExclusiveStartKey=last_evaluated_key)
		else:
			response = destination_dbtable.scan()
		last_evaluated_key = response.get('LastEvaluatedKey')
		destination_list.extend(response['Items'])
		if not last_evaluated_key:
			break
	# Decide how donation to be distributed. The following uses round robin as example.
	round_robin = 0;
	delivery_info = []
	for item in donation_info:
		delivery_item = item.copy()
		delivery_item['destination_name'] = destination_list[round_robin]['name']
		delivery_item['destination_address'] = destination_list[round_robin]['address']
		delivery_item['destination_phone'] = destination_list[round_robin]['phone']
		delivery_item['destination_email'] = destination_list[round_robin]['email']
		sid_seed = str(delivery_item.copy)
		md5 = hashlib.md5()
		md5.update(sid_seed.encode('utf-8'))
		delivery_item['sid'] = md5.hexdigest()
		delivery_info.append(delivery_item.copy())
		# Update round robin information
		round_robin = (round_robin + 1) % len(destination_list)
	
	return delivery_info

def contact_logistics(delivery_info):
	# Assign tasks to Logistics Department through SQS.
	request_queue = sqs_client.get_queue_url(QueueName='Delivery_Request_Queue')
	for item in delivery_info:
		msg = json.dumps(item.copy())
		sqs_client.send_message(QueueUrl=request_queue['QueueUrl'], MessageBody=msg)
	return

def get_warehouse_dbtable(table_name):
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

if __name__ == '__main__':
	donation_dbtable_name = 'Donation_Records'
	donation_dbtable = dynamodb_resource.Table(donation_dbtable_name)
	respond_queue = sqs_client.get_queue_url(QueueName='Delivery_Respond_Queue')
	while True:
		print('Start to collection donation information from cooperative restuarants.')
		# Generate neccesary information to distribute donation
		donation_info = query_recently_donation()
		if len(donation_info) > 0:
			print('Send requests to Logistics Department to deliver the donation supplies.')
			delivery_info = donation_distribution(donation_info)
			contact_logistics(delivery_info)
			# Wait for the completion of distribution
			print('Waiting for the delivery to be completed...')
			while len(delivery_info) != 0:
				# Receive responds from SQS
				respond = sqs_client.receive_message(
					QueueUrl=respond_queue['QueueUrl'],
					MaxNumberOfMessages=len(delivery_info),
					WaitTimeSeconds=20,
				)
				
				if 'Messages' in respond.keys():
					# Update the information of corresponding warehouse.
					pop_list = []
					msg_delete_list = []
					for msg in respond['Messages']:
						sid = msg['Body']
						for request in delivery_info:
							if request['sid'] == sid:
								print('Donation from {} has been sent to the warehouse {}.'.format(request['restaurant_name'], request['destination_name']))
								pop_list.append(delivery_info.index(request))
								msg_delete_list.append(msg['ReceiptHandle'])
					for item in msg_delete_list:
						delete_msg_response = sqs_client.delete_message(
							QueueUrl=respond_queue['QueueUrl'],
							ReceiptHandle=item
						)
					for idx in pop_list:
						request = delivery_info.pop(idx)
						warehouse_dbtable_name = request['destination_name']
						warehouse_dbtable = get_warehouse_dbtable(warehouse_dbtable_name)
						for item in request['donation_items']:
							warehouse_dbtable.put_item(
								Item={
									'sid': item['sid'],
									'restaurant_name': request['restaurant_name'],
									'product_name': item['product_name'],
									'donation_count': item['donation_count'],
									'expiration_date': item['expiration_date']
								}
							)
							donation_dbtable.delete_item(
								Key={
									'sid': item['sid']
								}
							)
				else:
					pass
		else:
			print('No donation requests for now.')
		# Wait for next period for distribution
		print('The next schedule will begin in 60 secs...')
		time.sleep(60)