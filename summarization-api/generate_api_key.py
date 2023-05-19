import os
import base64

def generate_api_key(length=32):
    key = os.urandom(length)
    return base64.urlsafe_b64encode(key).rstrip(b'=').decode('utf-8')

print(generate_api_key())
