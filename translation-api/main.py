import boto3
import os
from datetime import datetime
import json
import uuid
import botocore as botocore

import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import nltk

nltk.download('punkt')

# available models: 'facebook/nllb-200-distilled-600M', 'facebook/nllb-200-distilled-1.3B',
#                   'facebook/nllb-200-1.3B',           'facebook/nllb-200-3.3B'
model_name = 'facebook/nllb-200-3.3B'
model = AutoModelForSeq2SeqLM.from_pretrained(model_name, device_map="auto")
tokenizer = AutoTokenizer.from_pretrained(model_name)

def generate(text: str, output_lang_code: str, max_length: int):
    inputs = tokenizer(text, return_tensors="pt", max_length=max_length, truncation=True, padding="max_length")

    inputs = {key: value.to("cuda:0") for key, value in inputs.items()}

    bos_token_id = tokenizer.lang_code_to_id[output_lang_code]
    eos_token_id = tokenizer.eos_token_id

    with torch.no_grad():
        outputs = model.generate(**inputs, forced_bos_token_id=bos_token_id, forced_eos_token_id=eos_token_id,
                                 max_new_tokens=max_length)

    translated_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
    return translated_text

def translate(text: str, output_lang_code: str):
    sentences = nltk.sent_tokenize(text)
    translated_text = ""
    for sentence in sentences:
        translated_text += generate(sentence, output_lang_code, 200) + " "
    return translated_text


sqs = boto3.client('sqs',
                   region_name=os.getenv('AWS_REGION'),
                   aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
                   aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY'))
input_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/SummariesToTranslate.fifo'
output_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/TranslatedSummaries.fifo'

try:
    print("translation-api running and listening...")
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
            translatedSummary = translate(body["summary"], body["summaryLanguage"])

            output_message_body = json.dumps({
                "videoId": body["videoId"],
                "chapterId": body["chapterId"],
                "summary": translatedSummary})

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
            pass
except KeyboardInterrupt:
    print("\nInterrupted by user. Exiting...")
