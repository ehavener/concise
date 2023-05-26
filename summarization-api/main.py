from datetime import datetime

import pika
import json
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import socket
import fcntl
import struct

def get_default_gateway():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        return socket.inet_ntoa(fcntl.ioctl(s.fileno(), 0x8915, struct.pack('256s', b'eth0'))[20:24])

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

    return tokenizer.convert_tokens_to_ids(encoded_text)

    inputs = tokenizer.convert_tokens_to_ids(encoded_text)

    print(len(inputs))

    # Move inputs to the device
    inputs = inputs.to(device)

    # Generate output. 250 Tokens is 10 sentences.
    # TODO: Evaluate results using 8-bit precision.
    outputs = model.generate(inputs, max_length=250, temperature=0.2, top_p=0.9, do_sample=True)

    # Decode the output
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    summary = summary.replace("  ", " ")
    summary = summary.replace("<pad> ", "")  # Remove leading pad token
    print("completed generation for: ", len(inputs))
    return summary

def callback(ch, method, properties, body):
    print("Received request at ", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print(ch, method, properties, body)
    message = json.loads(body)
    # print(message)
    summary = summarize(message["transcript"])
    json_message = json.dumps({
        "videoId": message["videoId"],
        "chapterId" : message["chapterId"],
        "summary": summary})
    channel.basic_publish(exchange='', routing_key='summarized', body=json_message)


# RabbitMQ connection
connection = pika.BlockingConnection(pika.ConnectionParameters(host=get_default_gateway()))

channel = connection.channel()

channel.queue_declare(queue='toSummarize')

# Tell RabbitMQ that this particular function should receive messages from our toSummarize queue
channel.basic_consume(queue='toSummarize', on_message_callback=callback, auto_ack=True)

print('Waiting for messages. To exit press CTRL+C')

# Start a never-ending loop that waits for data and runs callbacks whenever necessary
channel.start_consuming()
