# Concise Video Summarizer
![concise-gif-lq](https://github.com/ehavener/concise/assets/18430808/ee8c53bd-4aea-430e-a5e4-f6b8fe5ed1f9)


### Introduction
Concise is a web application for summarizing YouTube videos. Concise was developed primarily as an exercise in Java and Spring Boot development. It was additionally developed as an exercise in applying Transformer language models to solve problems like summarization and machine translation. An objective of Concise is to demonstrate the capabilities of consumer hardware in solving NLP tasks. The application currently relies on gpt-3.5-turbo for main summaries and a long term goal for this project is to remove dependencies on closed source models.

### Features
  - Two-Click Summarization: Summarize YouTube videos without leaving the page.
  - Chapter Summarization: Jump directly to interesting video content with chapter summaries.
  - Translation: Generate summaries in 200 languages.

### Techniques
  - **Chrome Extension:** Provides quick access to the Concise application directly from any YouTube video.
  - **Transformers**: Provides detailed summaries of videos, which can be further translated into multiple languages.
  - **Authentication and User Management**: Standard user management system with JSON Web Token authentication for secure access.
  - **Microservices**: Dedicated services for fetching video transcripts, summarization, and translation, ensuring scalability.
  - **Message Queues**: Language model inference takes time in the order of seconds. Combined with the reality that videos have several chapters, this makes REST alone unsuitable for processing requests. Some message queueing technique is required to provide a reliable service. After considering alternatives like RabbitMQ, I chose to implement Amazon SQS message queues because they interface over regular REST requests and do not require any port forwarding for communicating between local and remote environments. More specifically, this enabled me to develop locally against remote GPU servers without having to expose ports.

### Technologies
  - **Frontend:** TypeScript, Next.js, HTML5, CSS3
  - **Extension:** JavaScript
  - **Backend:** Java, Spring Boot, Amazon SQS
  - **Microservices:** Python, Hugging Face Transformers Language Models (jordiclive/flan-t5-11b-summarizer-filtered, gpt-3.5-turbo, facebook/nllb-200-3.3B, and oliverguhr/fullstop-punctuation-multilang-large)
  - **Authentication:** JWT (JSON Web Token)
  - **Third Party APIs:** YouTube Data API

### Architecture
Concise uses Spring Boot in conjunction with NLP task specific python microservices that interface using queues.

### Application State Diagrams
Rectangle backgrounds designate the service and squares/diamonds designate the state.
![application-state-diagram drawio (1)](https://github.com/ehavener/concise/assets/18430808/70b3ff97-0fdf-4866-a98b-cddaf32e7ec7)

### Organization
This project is organized into several sub-projects at the root level directory.

### Development Setup
Setting up the project requires following the setup instructions for each application. The recommended order is:

1. [Transcript-api](https://github.com/ehavener/concise#transcript-api)
2. [Backend](https://github.com/ehavener/concise#backend)
3. [Translation-api](https://github.com/ehavener/concise#translation-api)
4. [Summarization-api](https://github.com/ehavener/concise#summarization-api)
5. [Frontend](https://github.com/ehavener/concise#frontend)
6. [Extension](https://github.com/ehavener/concise#extension)

Each of these steps should run without requiring the higher order applications to be setup so that you can test each layer as they are configured. This is also the order for starting the services once they are ready to deploy. You should first ensure that you have a docker account, a google cloud account for the YouTube API, and an AWS account for SQS.

### Contributing
If you encounter any bugs or issues while running the project, or would like to improve it, please feel free to open an issue or contact me directly.

# Transcript-api
This is a python microservice that fetches video transcripts from YouTube's API. YouTube videos typically have multiple transcripts to choose from. This application selects the best transcript available for input to a summarization LLM. It has one REST endpoint that is called by `backend` and authorized using an internal API key.
### Setup
1. Generate an internal API key by running `transcript-api/generate_api_key.py`. Backend's application.properties's `transcript.api.key` value will need to be set using this API key.
2. Install dependencies by running ```pip install -r requirements.txt```
3. Ensure the API key you just generated is available to this application as an environment variable: `API_KEY = ... `
4. Run the application with `transcript-api/venv/bin/python -m uvicorn main:app --reload`

# Backend
This is a Java Spring Boot application that provides JWT authentication and user management, persists and serves summaries, and sends requests to respective python APIs for NLP tasks.
### Setup
1. Generate a JWT Secret key by running `backend/src/main/java/com/concise/backend/security/SecretKeyGenerator.java` 
2. Obtain a YouTube Data API v3 key from Google Cloud
3. Install Postgres and start the Postgres service
4. Create `application.properties` file at `backend/src/main/resources/application.properties`
5. Add the following code to your application.properties file replacing all bracketed variables with your own:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/{database}
spring.datasource.username={username}
spring.datasource.password={password}

spring.datasource.driverClassName=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect

spring.jpa.hibernate.ddl-auto=create-update
logging.level.sql=debug
spring.jpa.show-sql=true

youtube.api.key={YouTube API v3 key obtained from Google Cloud}

transcript.api.url=http://localhost:8000/transcript/
transcript.api.key={The internal API key generated when setting up transcript-api}

jwt.secret={JWT secret key generated in step 1}

```
4. Manually replace the queue urls defined in `backend/src/main/java/com/concise/backend/sqs/SqsProducerService.java` and `backend/src/main/java/com/concise/backend/sqs/SummaryMessageListener.java` with your own Amazon SQS FIFO queues for `TextToSummarize`, `SummariesToTranslate`, and `Summaries`. Queues should be configured with default settings. 

# Summarization-api
This is a python microservice that summarizes transcripts using either `jordiclive/flan-t5-11b-summarizer-filtered` or `gpt-3.5-turbo` depending on whether the summary is for a full video or a chapter.
### Setup
This model (flan-t5-11b) requires GPU with 24GB of VRAM to run, such as a NVIDIA GTX 3090. You can rent one of these instances from vast.ai for around `$0.25` per hour. [LambdaLabs](https://lambdalabs.com/service/gpu-cloud#pricing), [CoreWeave](https://www.coreweave.com/gpu-cloud-pricing), and [Paperspace](https://www.paperspace.com/pricing) offer comparable but slightly higher rates and provide a more reliable service. [Major](https://aws.amazon.com/sagemaker/pricing/) [cloud](https://azure.microsoft.com/en-us/pricing/details/machine-learning/#pricing) [providers](https://cloud.google.com/compute/gpus-pricing) also offer appropriate instances.
1. Replace `input_queue_url` and `output_queue_url` with your own Amazon SQS queues for `TextToSummarize` and `Summaries` respectively.
2. Build and push the container to your own docker repository. Replace {username} with the username of your docker account.
```
cd ~/concise/summarization-api
docker build -t summarization-api .
docker tag summarization-api:latest {username}/summarization-api:latest
docker push {username}/summarization-api:latest
```
3. Load and start the docker container on a service of your choosing. Ensure the following environment variables are available so that the application can access your SQS queues (replace the ...):
```
OPENAI_API_KEY=... AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_REGION=...
```
5. Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.


# Translation-api
This is a python microservice that translates transcripts using `facebook/nllb-200-3.3B`.
### Setup
The same GPU requirements specified in the `summarization-api` section apply to this service.

1. Replace `input_queue_url` and `output_queue_url` with your own Amazon SQS queues for `SummariesToTranslate` and `TranslatedSummaries` respectively.
2. Build and push the container to your own docker repository. Replace {username} with the username of your docker account.
```
cd translation-api
docker build -t translation-api .
docker tag translation-api:latest {username}/translation-api:latest
docker push {username}/translation-api:latest
```
3. Load and start the docker container on a service of your choosing. Ensure the following environment variables are available so that the application can access your SQS queues (replace the ...):
```
AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_REGION=...
```
5. Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.

# Extension
This application contains a Google Chrome extension that wraps the NextJS frontend.
### Setup
The extension can be loaded into chrome directly without any modification.
1. Open Google Chrome.
2. Click on the three-dot menu icon in the top right corner of the browser window and select "More Tools", then "Extensions" from the dropdown menu.
3. On the Extensions page, you'll find a toggle on the top right for "Developer mode". Ensure it is enabled.
4. Once you enable Developer Mode, you'll see additional options at the top of the page, including "Load unpacked".
5. Click on "Load unpacked", and a file dialog will pop up.
7. Select the directory with the extension's code i.e. (extension) and click the "Open" button.
8. You should see the extension icon in your Chrome toolbar.

# Frontend
This is a NextJS application that provides the entire frontend for Concise.
### Setup
1. Ensure you are in the frontend directory `cd frontend`
2. Install dependencies by running `npm install`
3. Serve the application in development mode by running `npm run dev`

Finally, navigate to a YouTube video and generate a summary by clicking on the extension icon and clicking generate.
