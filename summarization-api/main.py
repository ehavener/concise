from fastapi import FastAPI, Depends, HTTPException, status

from fastapi.security import APIKeyHeader
from dotenv import load_dotenv
import os

import textwrap

from pydantic.main import BaseModel

load_dotenv()  # take environment variables from .env.
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

class Text(BaseModel):
    text: str

@app.post("/summarize/", dependencies=[Depends(get_api_key)])
async def summarize(text: Text):
    summary = textwrap.shorten(text.text, width=100)  # This just shortens the text to 100 characters
    return {"summary": summary}

