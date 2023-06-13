from datetime import datetime

import boto3
import json

import botocore as botocore
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import os
import uuid

from deepmultilingualpunctuation import PunctuationModel
import nltk
from sumy.parsers.plaintext import PlaintextParser
from sumy.nlp.tokenizers import Tokenizer
from sumy.summarizers.lsa import LsaSummarizer
import openai

nltk.download('punkt')

openai.api_key = os.getenv('OPENAI_API_KEY')

punctuation_model = PunctuationModel()

# Load the summarization model and tokenizer
model_name = "jordiclive/flan-t5-11b-summarizer-filtered"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name, load_in_4bit=True, device_map="auto")

target_length = 150
max_source_length = 512

example_prompts = {
    "social": "Produce a short summary of the following social media post:",
    "ten": "Summarize the following article in 10-20 words:",
    "5": "Summarize the following article in 0-5 words:",
    "100": "Summarize the following article in about 100 words:",
    "summary": "Write a ~ 100 word summary of the following text:",
    "short": "Provide a short summary of the following article:",
    "video": "Produce a short summary of the following video chapter transcript:",
}

def generate(inputs, max_source_length=512, summarization_type=None, prompt=None):
    """returns a list of zipped inputs, outputs and number of new tokens"""

    if prompt is not None:
        inputs = [f"{prompt.strip()} \n\n {i.strip()}" for i in inputs]
    if summarization_type is not None:
        inputs = [
            f"{example_prompts[summarization_type].strip()} \n\n {i.strip()}"
            for i in inputs
        ]
    if summarization_type is None and prompt is None:
        inputs = [f"Summarize the following: \n\n {i.strip()}" for i in inputs]
    input_tokens = tokenizer.batch_encode_plus(
        inputs,
        max_length=max_source_length,
        padding="max_length",
        truncation=True,
        return_tensors="pt",
    )
    for t in input_tokens:
        if torch.is_tensor(input_tokens[t]):
            input_tokens[t] = input_tokens[t].to("cuda:0")

    outputs = model.generate(
        **input_tokens,
        use_cache=True,
        num_beams=5,
        min_length=5,
        max_new_tokens=target_length,
        no_repeat_ngram_size=3,
    )

    input_tokens_lengths = [x.shape[0] for x in input_tokens.input_ids]
    output_tokens_lengths = [x.shape[0] for x in outputs]

    total_new_tokens = [
        o - i for i, o in zip(input_tokens_lengths, output_tokens_lengths)
    ]
    outputs = tokenizer.batch_decode(outputs, skip_special_tokens=True)

    return inputs, outputs, total_new_tokens


def summarize_chapter(text):
    # TODO: Segment transcript text into sentences using punctuation model.
    _, outputs, _ = generate([text], summarization_type="video")
    return outputs[0]

def summarize_full(text):
    # Restore punctuation using deepmultilingualpunctuation
    result = punctuation_model.restore_punctuation(text)

    # Extract 50 sentences that best summarize the transcript using LSA so that is input around 1500 words
    parser = PlaintextParser.from_string(result, Tokenizer("english"))
    summarizer = LsaSummarizer()
    lsa_summary = summarizer(parser.document, 50)
    lsa_summary_string = " ".join(str(sentence) for sentence in lsa_summary)

    # Call OpenAI API and return
    gpt_response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=[
            {
                "role": "system",
                "content": "You are a helpful assistant."
            },
            {
                "role": "user",
                "content": "Below are 100 sentences extracted from a YouTube video transcript. Generate a summary in no more than 400 words: \n\n" + lsa_summary_string
            }
        ],
        max_tokens=1000,
        temperature=0.1
    )
    return gpt_response['choices'][0]['message']['content']


sqs = boto3.client('sqs',
                   region_name=os.getenv('AWS_REGION'),
                   aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
                   aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY'))
input_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/TextToSummarize.fifo'
output_queue_url = 'https://sqs.us-west-1.amazonaws.com/887897278824/Summaries.fifo'

try:
    print("summarization-api running and listening...")
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

            if body["type"] == "full":
                summary = summarize_full(body["transcript"])
            elif body["type"] == "chapter":
                summary = summarize_chapter(body["transcript"])

            output_message_body = json.dumps({
                "videoId": body["videoId"],
                "chapterId": body["chapterId"],
                "summaryLanguage": body["summaryLanguage"],
                "summary": summary
            })

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
