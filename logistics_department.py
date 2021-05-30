# Receive the requests from Service Provider and assign truck for delivery

import json
import time
import threading
import random
import boto3

from awscrt import io, mqtt, auth, http
from awsiot import mqtt_connection_builder, iotshadow
from concurrent.futures import Future

# AWS SQS
sqs_client = boto3.client('sqs', 'us-east-1')
# AWS SNS
sns_client = boto3.client('sns', 'us-east-1')
# AWS IoT Core
iot_client = boto3.client('iot-data', 'us-east-1')

class Truck:
	

	def __init__(self, id):
		self.id = id
		self.state = 'idle'
		self.temperature = 15.0
		self.humidity = 40.0
		self.mission = None
		self.cert = '{}.crt'.format(id)
		self.key = '{}.key'.format(id)
		self.root_ca = 'rootCA.crt'
		self.awsiot_endpoint = 'a1qbqmv22olc5o-ats.iot.us-east-1.amazonaws.com'
		self.connect_to_awsiot()
		
	def connect_to_awsiot(self):
		self.event_loop_group = io.EventLoopGroup(1)
		self.host_resolver = io.DefaultHostResolver(self.event_loop_group)
		self.client_bootstrap = io.ClientBootstrap(self.event_loop_group, self.host_resolver)
		self.mqtt_connection = mqtt_connection_builder.mtls_from_path(
			endpoint=self.awsiot_endpoint,
			cert_filepath=self.cert,
			pri_key_filepath=self.key,
			client_bootstrap=self.client_bootstrap,
			ca_filepath=self.root_ca,
			client_id=self.id,
			clean_session=False,
			keep_alive_secs=6
		)
		print("Connecting to {} with client ID '{}'...".format(self.awsiot_endpoint, self.id))
		self.connect_future = self.mqtt_connection.connect()
		self.shadow_client = iotshadow.IotShadowClient(self.mqtt_connection)
		print("Client ID '{}' connected!".format(self.id))
		'''
		# Subscribe to necessary topics.
		print("Client ID '{}' is subscribing to Update responses...".format(self.id))
		update_accepted_subscribed_future, _ = self.shadow_client.subscribe_to_update_shadow_accepted(
			request=iotshadow.UpdateShadowSubscriptionRequest(thing_name=self.id),
			qos=mqtt.QoS.AT_LEAST_ONCE,
			callback=self.on_update_shadow_accepted)
		update_rejected_subscribed_future, _ = self.shadow_client.subscribe_to_update_shadow_rejected(
			request=iotshadow.UpdateShadowSubscriptionRequest(thing_name=self.id),
			qos=mqtt.QoS.AT_LEAST_ONCE,
			callback=self.on_update_shadow_rejected)
		# Wait for subscriptions to succeed
		update_accepted_subscribed_future.result()
		update_rejected_subscribed_future.result()
		'''
		# Initialize the shadow states
		print("Client ID '{}' is initilaizing shadow status...".format(self.id))
		request = iotshadow.UpdateShadowRequest(
			thing_name=self.id,
			state=iotshadow.ShadowState(
				reported={ 'temperature': self.temperature, 'humidity': self.humidity },
				desired={ 'temperature': self.temperature, 'humidity': self.humidity }
			)
		)
		future = self.shadow_client.publish_update_shadow(request, mqtt.QoS.AT_LEAST_ONCE)
		#future.add_done_callback(self.on_publish_update_shadow)
		print("Client ID '{}' initialization complete.".format(self.id))

	def get_truck_info(self):
		return { 'id': self.id, 'state': self.state, 'mission': self.mission }

	def start_mission(self, mission):
		self.mission = mission
		# Use thread to simulate the delivery
		task = threading.Thread(target=self.delivery)
		task.daemon = True
		task.start()
		
	def delivery(self):
		self.state = 'working'
		# Estimate the spending time to restaurant and warehouse
		total_time = random.randint(5, 10)
		for tick in range(total_time):
			# Update temperture and humidity based on the sensor in truck
			self.temperature = 15.0 + float(random.randint(-50, 50)) / 10.0
			self.humidity = 40.0 + float(random.randint(-100, 100)) / 10.0
			request = iotshadow.UpdateShadowRequest(
				thing_name=self.id,
				state=iotshadow.ShadowState(
					reported={ 'temperature': self.temperature, 'humidity': self.humidity },
					desired={ 'temperature': self.temperature, 'humidity': self.humidity }
				)
			)
			future = self.shadow_client.publish_update_shadow(request, mqtt.QoS.AT_LEAST_ONCE)
			#future.add_done_callback(self.on_publish_update_shadow)
			time.sleep(1)
		self.state = 'idle'
		# Inform the Logistics Department that the task is complete
		self.mqtt_connection.publish(
			topic='hackathon/delivery',
			payload=self.mission['sid'],
			qos=mqtt.QoS.AT_LEAST_ONCE
		)
	'''
	def on_update_shadow_accepted(response):
		# type: (iotshadow.UpdateShadowResponse) -> None
		try:
			pass
		except Exception as e:
			exit(e)

	def on_update_shadow_rejected(error):
		# type: (iotshadow.ErrorResponse) -> None
		exit("Update request was rejected. code:{} message:'{}'".format(error.code, error.message))
	
	def on_publish_update_shadow(future):
		#type: (Future) -> None
		try:
			future.result()
		except Exception as e:
			exit(e)
	'''

def trace_delivery_state(truck_list):
	abnormal_topic_arn = 'arn:aws:sns:us-east-1:414491172781:Delivery_Abnormal_Topic'
	while True:
		# Poll for the states of working trunks
		for truck in truck_list:
			truck_info = truck.get_truck_info()
			if truck_info['state'] == 'working':
				respond = iot_client.get_thing_shadow(thingName=truck_info['id'])
				truck_state = json.loads(respond['payload'].read())
				temperature = truck_state['state']['reported']['temperature']
				humidity = truck_state['state']['reported']['humidity']
				print('{}: temperature: {}, humidity: {}'.format(truck_info['id'], str(temperature), str(humidity)))
				if temperature > 19.5 or temperature < 10.5 or humidity > 49.0 or humidity < 31.0:	
					# Publish to sns to report abnormal state
					request = truck_info['mission']
					msg = 'The sensor on {} detected abnormal delivery environment at {} during handling the request "{}".'.format(truck_info['id'], time.ctime(), request['sid'])
					if temperature > 19.5:
						msg = msg + '\nThe temperature is {} degree of Celsius. (Too High)'.format(temperature)
					elif temperature < 10.5:
						msg = msg + '\nThe temperature is {} degree of Celsius. (Too Low)'.format(temperature)
					if humidity > 49.0:
						msg = msg + '\nThe humidity is {}%. (Too High)'.format(humidity)
					elif humidity < 31.0:
						msg = msg + '\nThe humidity is {}%. (Too Low)'.format(humidity)
					msg = msg + '\nPlease checkout the quality of your products after the delivery complete.'
					sns_client.publish(TopicArn=abnormal_topic_arn, Message=msg)
		time.sleep(1)


def receive_delivery_complete():
	complete_queue = sqs_client.get_queue_url(QueueName='Delivery_Complete_Queue')
	respond_queue = sqs_client.get_queue_url(QueueName='Delivery_Respond_Queue')
	while True:
		# Receive the delivery status or the abnormal state from trucks
		respond = sqs_client.receive_message(
			QueueUrl=complete_queue['QueueUrl'],
			MaxNumberOfMessages=1,
			WaitTimeSeconds=20,
		)
		# Respond to Service Provider when the task complete
		if 'Messages' in respond.keys():
			msg_delete_list = []
			for msg in respond['Messages']:
				sid = msg['Body']
				print('Request "{}" is complete!'.format(sid))
				msg_delete_list.append(msg['ReceiptHandle'])
				sqs_client.send_message(QueueUrl=respond_queue['QueueUrl'], MessageBody=sid)
			for item in msg_delete_list:
				delete_msg_response = sqs_client.delete_message(
					QueueUrl=complete_queue['QueueUrl'],
					ReceiptHandle=item
				)
	
if __name__ == '__main__':
	# All trucks available in Logistics Department.
	truck_list = []
	total_trucks = 3
	for i in range(total_trucks):
		truck = Truck('truck{}'.format(str(i)))
		truck_list.append(truck)
	# Create a thread to trace the delivery state.	
	trace = threading.Thread(target=trace_delivery_state, args=(truck_list, ))
	trace.daemon = True
	trace.start()
	# Create a thread to trace the delivery state.	
	delivery = threading.Thread(target=receive_delivery_complete)
	delivery.daemon = True
	delivery.start()
	# Main thread is used to handle requests
	round_robin = 0
	request_queue = sqs_client.get_queue_url(QueueName='Delivery_Request_Queue')
	while True:
		# Poll requests from Service Provider in sqs
		respond = sqs_client.receive_message(
			QueueUrl=request_queue['QueueUrl'],
			MaxNumberOfMessages=total_trucks,
			WaitTimeSeconds=20,
		)
		# Assign trucks for delivery
		if 'Messages' in respond.keys():
			print('Received requests from Service Provider.')
			for msg in respond['Messages']:
				request = json.loads(msg['Body'])
				anchor = round_robin
				while True:
					truck_info = truck_list[round_robin].get_truck_info()
					if truck_info['state'] == 'idle':
						print('Assign task {} to {}.'.format(request['sid'], truck_info['id']))
						truck_list[round_robin].start_mission(request)
						round_robin = (round_robin + 1) % total_trucks
						delete_msg_response = sqs_client.delete_message(
							QueueUrl=request_queue['QueueUrl'],
							ReceiptHandle=msg['ReceiptHandle']
						)
						break
					else: 
						round_robin = (round_robin + 1) % total_trucks
					if round_robin == anchor:
						print('All trucks are unavailable now. Wait for 10 secs...')
						time.sleep(10)
					