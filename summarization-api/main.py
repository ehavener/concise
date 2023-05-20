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

    # Move inputs to the device
    inputs = inputs.to(device)

    # Generate output. 75-100 Tokens is around 5 sentences.
    outputs = model.generate(inputs, max_length=100, temperature=0.7, top_p=1.0, do_sample=True)

    # Remove the pad token id (default is 0 for T5) from the generated output
    outputs = outputs[0, 1:] if outputs[0, 0] == tokenizer.pad_token_id else outputs[0]

    # Decode the output
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    summary = summary.replace("  ", " ")

    return {"summary": summary}

