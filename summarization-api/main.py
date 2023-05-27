from datetime import datetime

import boto3
import json

import botocore as botocore
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import os
import uuid

# Check if GPU is available and if not, use CPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Load the model and tokenizer
model_name = "lmsys/fastchat-t5-3b-v1.0"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

# Move model to the device
model = model.to(device)

# TODO: Test a model with a larger context window for better summaries of >10 minute videos. MPT | RWKV | Guanaco.
# TODO: Improve summary quality. Improve per-token inference speed.
def summarize(text):
    # add summarization prompt to text
    text = "Summarize the following video transcript text into a 5-10 sentence paragraph.\n\n" + text + "TL;DR: "

    max_length = 1750

    # Encode input text
    tokenized_text = tokenizer.tokenize(text, return_tensors="pt")

    # Ensure no sentence is cut off at the start
    if not tokenized_text[0].isupper() and not tokenized_text[0].isdigit():
        for i in range(len(tokenized_text)):
            if tokenized_text[i] in {'.', '!', '?'}:
                tokenized_text = tokenized_text[i+1:]
                break

    # Ensure no sentence is cut off at the end
    encoded_text = []
    sentence = []
    for token in tokenized_text:
        if len(encoded_text) + len(sentence) + 1 > max_length:
            break
        elif token in {'.', '!', '?'}:
            sentence.append(token)
            encoded_text.extend(sentence)
            sentence = []
        else:
            sentence.append(token)

    inputs_ids = tokenizer.convert_tokens_to_ids(encoded_text)

    print(len(inputs_ids))
    inputs = torch.tensor([inputs_ids])

    # Move inputs to the device
    inputs = inputs.to(device)

    # Generate output. 250 Tokens is 10 sentences.
    # TODO: Evaluate results using 8-bit precision.
    outputs = model.generate(inputs, max_length=250, temperature=0.2, top_p=0.9, do_sample=True)

    # Decode the output
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    summary = summary.replace("  ", " ")
    summary = summary.replace("<pad> ", "")  # Remove leading pad token
    print("completed generation for: ", len(inputs_ids))
    return summary


sqs = boto3.client('sqs',
                   region_name=os.getenv('AWS_REGION'),
                   aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
                   aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY'))
input_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/TextToSummarize.fifo'
output_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/Summaries.fifo'

try:
    while True:
        response = sqs.receive_message(
            QueueUrl=input_queue_url,
            AttributeNames=['All'],
            MaxNumberOfMessages=1,
            MessageAttributeNames=['All'],
            VisibilityTimeout=30,
            WaitTimeSeconds=1
        )

        if 'Messages' in response:
            message = response['Messages'][0]
            receipt_handle = message['ReceiptHandle']

            # Print out the message body
            # print('Received message: %s' % message['Body'])
            print("Received at ", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
            body = json.loads(message['Body'])

            summary = summarize(body["transcript"])

            output_message_body = json.dumps({
                "videoId": body["videoId"],
                "chapterId": body["chapterId"],
                "summary": summary})

            # Enqueue a new message to Summaries.fifo
            sqs.send_message(
                QueueUrl=output_queue_url,
                MessageGroupId=str(body["videoId"]),
                MessageDeduplicationId=str(uuid.uuid4()),
                MessageBody=output_message_body
            )
            print('New message enqueued to the destination queue.')

            # Delete received message from queue
            try:
                sqs.delete_message(
                    QueueUrl=input_queue_url,
                    ReceiptHandle=receipt_handle
                )
                print('Deleted message with receipt handle:', receipt_handle)
            except botocore.exceptions.ClientError as e:
                if e.response['Error']['Code'] == 'InvalidParameterValue' and e.response['Error']['Message'].find('The receipt handle has expired.'):
                    print('Receipt handle expired. Skipping message deletion.')
                else:
                    raise  # Reraise the exception if it's not related to an expired receipt handle
        else:
            print('No messages to process.')
except KeyboardInterrupt:
    print("\nInterrupted by user. Exiting...")