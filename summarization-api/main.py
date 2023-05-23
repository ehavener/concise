from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import APIKeyHeader
import os

from pydantic import BaseModel
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

API_KEY = os.getenv("API_KEY")  # Get API Key from environment variable
API_KEY_NAME = "X-API-Key"

api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)

app = FastAPI()


async def get_api_key(api_key_header: str = Depends(api_key_header)):
    if api_key_header == API_KEY:
        return api_key_header
    else:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid API Key",
        )


class RequestBodyModel(BaseModel):
    text: str


# Check if GPU is available and if not, use CPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Load the model and tokenizer
model_name = "lmsys/fastchat-t5-3b-v1.0"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

# Move model to the device
model = model.to(device)

@app.post("/summarize/", dependencies=[Depends(get_api_key)])
async def summarize(body: RequestBodyModel):
    # Encode input text
    inputs = tokenizer.encode(body.text, return_tensors="pt")

    # Calculate the maximum length for the generate method. Constraint: input_length + generation length <= 2048 tokens
    # TODO: Chunk summaries per 1500-ish tokens. Determine appropriate summary lengths. Recurse if necessary.
    if inputs.shape[1] > 1500:
        inputs = inputs[:, :1500]

    # Move inputs to the device
    inputs = inputs.to(device)

    # Generate output. 500 Tokens is 20 sentences.
    outputs = model.generate(inputs, max_length=500, temperature=0.2, top_p=0.9, do_sample=True)

    # Decode the output
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    summary = summary.replace("  ", " ")
    summary = summary.replace("<pad> ", "")  # Remove leading pad token

    return {"summary": summary}

