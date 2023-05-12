from fastapi import FastAPI, Depends, HTTPException, status
from youtube_transcript_api import YouTubeTranscriptApi, NoTranscriptFound

from fastapi.security import APIKeyHeader
from dotenv import load_dotenv
import os

from youtube_languages import all_youtube_language_codes

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

@app.get("/transcript/{youtube_id}/{language}", dependencies=[Depends(get_api_key)])
async def get_transcript(youtube_id: str, language: str):
    # English Spanish French German Italian Dutch Portuguese
    highly_resourced_languages = ['en', 'es', 'fr', 'de', 'it', 'nl', 'pt']
    transcript_list = YouTubeTranscriptApi.list_transcripts(youtube_id)

    # cross-reference (MANUALLY CREATED) transcripts with language
    # if a (MANUALLY CREATED) transcript exists for language then download and return it
    transcript = None
    try:
        transcript = transcript_list.find_manually_created_transcript([language] + highly_resourced_languages)
    except NoTranscriptFound:
        print("No (MANUALLY CREATED) transcript found for the given languages.")

    # else if a (GENERATED) transcript exists for language then download and return it
    if transcript is None:
        try:
            transcript = transcript_list.find_generated_transcript([language] + highly_resourced_languages)
        except NoTranscriptFound:
            print("No (GENERATED) transcript found for the given languages.")

    # else if a (TRANSLATABLE) transcript exists at all translate it to English and return it (test with tqrSsXdLQ1k)
    if transcript is None:
        try:
            transcript = transcript_list.find_generated_transcript([language] + highly_resourced_languages + all_youtube_language_codes)
            transcript = transcript.translate('en')
        except NoTranscriptFound:
            print("No (GENERATED) transcript found for the given languages.")

    if transcript is None:
        return 301

    transcript_data = transcript.fetch()
    return {"language": transcript.language_code, "transcript": transcript_data}
