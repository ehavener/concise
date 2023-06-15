# Concise Video Summarizer

### Introduction
Concise is a web application for summarizing YouTube videos. Concise was developed primarily as an exercise in Java Spring Boot development and as an exercise in applying Transformer language models to solve problems like summarization and machine translation. (TODO: goal is fully locally runnable)

### Features
  - One-Click Summarization: ...
  - Chapter Summarization: ...
  - Translation: ...

### Techniques
  - **Chrome Extension:** Provides quick access to the Concise application directly from any YouTube video.
  - **Transformers**: Provide detailed summaries of videos, which can be further translated into multiple languages.
  - **Authentication and User Management**: Standard user management system with JWT authentication for secure access.
  - **Microservices**: Dedicated microservices for fetching video transcripts, summarization, and translation, ensuring scalibility.
  - **Message Queues**: (TODO)

### Technologies
  - **Frontend:** TypeScript, Next.js, HTML5, CSS3
  - **Extension:** JavaScript
  - **Backend:** Java, Spring Boot, Amazon SQS
  - **Microservices:** Python, Transformer Language Models (jordiclive/flan-t5-11b-summarizer-filtered, gpt-3.5-turbo, facebook/nllb-200-3.3B, and oliverguhr/fullstop-punctuation-multilang-large)
  - **Authentication:** JWT (JSON Web Token)
  - **Third Party APIs:** YouTube Data API

### Architecture
Concise uses Spring Boot in conjunction with NLP-task-specific python microservices than interface using queues.

### Development Setup
Setting up the project requires following the setup instructions for each application. The recomended order is:

1. [Transcript-api](https://github.com/ehavener/concise#transcript-api)
2. [Backend](https://github.com/ehavener/concise#backend)
3. [Translation-api](https://github.com/ehavener/concise#translation-api)
4. [Summarization-api](https://github.com/ehavener/concise#summarization-api)
5. [Frontend](https://github.com/ehavener/concise#frontend)
6. [Extension](https://github.com/ehavener/concise#extension)

Each of these steps should run without requiring the higher order applications so that you can test as you go along. This is also the order for starting the services once you have them ready to deploy. You should first ensure that you have a docker account and are ready to create queues in Amazon SQS.

### Contributing
If you encounter any bugs or issues while running the project, or would like to improve it, please feel free to open an issue or contact me directly.

### License
...

### Application State Diagrams
Rectangle backgrounds designate the service and squares/diamonds designate the state.
![application-state-diagram drawio (1)](https://github.com/ehavener/concise/assets/18430808/70b3ff97-0fdf-4866-a98b-cddaf32e7ec7)

### Organization

This project is organized into several sub-projects at the root level directory.

# Transcript-api
This is a python microservice that fetches video transcripts from YouTube's API. YouTube videos typically have multiple transcripts to choose from. This application selects the best transcript available for input to a summarization LLM.

### Setup
1. Generate an API key by running `transcript-api/generate_api_key.py`. Backend's application.properties requires this api key for the value of `transcript.api.key`.
2. run ```pip install -r requirements.txt```
3. run ```/Users/emerson/concise/transcript-api/venv/bin/python -m uvicorn main:app --reload```

# Backend
This is a Java Spring Boot application that provides JWT authentication and user management, persists and serves summaries, and sends requests to respective python APIs when necessary.
### Setup
1. Install or run postgres
2. Create `application.properties` at `~concise/backend/src/main/resources/application.properties`
3. Add the following code to your application.properties file replacing bracketed code where present:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/{database}
spring.datasource.username={username}
spring.datasource.password=

spring.datasource.driverClassName=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect

spring.jpa.hibernate.ddl-auto=update
logging.level.sql=debug
spring.jpa.show-sql=true

youtube.api.key={Youtube API key from Google Cloud}

transcript.api.url=http://localhost:8000/transcript/
transcript.api.key={The api key you generated in step 0}
summarization.api.url=http://localhost:8001/summarize/
translation.api.url=http://localhost:8002/translate/

jwt.secret={The jwt secret key you generated in step 0}

```
4. Replace the queue urls defined in `backend/src/main/java/com/concise/backend/sqs/SqsProducerService.java` and `backend/src/main/java/com/concise/backend/sqs/SummaryMessageListener.java` with your own Amazon SQS FIFO queues for `TextToSummarize`, `SummariesToTranslate`, and `Summaries`.

# Summarization-api
This is a python microservice that summarizes transcripts using either `jordiclive/flan-t5-11b-summarizer-filtered` or `gpt-3.5-turbo`.
### Setup
This model requires GPU with a 24GB of VRAM to run such as a NVIDIA GTX 3090. You can rent one of these instances from vast.ai for around `$0.25` per hour. [LambdaLabs](https://lambdalabs.com/service/gpu-cloud#pricing), [CoreWeave](https://www.coreweave.com/gpu-cloud-pricing), and [Paperspace](https://www.paperspace.com/pricing) offer comparable but slightly higher rates and provide a reliable service. [Major](https://aws.amazon.com/sagemaker/pricing/) [cloud](https://azure.microsoft.com/en-us/pricing/details/machine-learning/#pricing) [providers](https://cloud.google.com/compute/gpus-pricing) also offer appropriate instances.

Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.

# Translation-api
This is a python microservice that translates transcripts using `facebook/nllb-200-3.3B`.
### Setup
The same GPU requirements specified in the `summarization-api` section apply to this service.

1. Replace `input_queue_url` and `output_queue_url` with your own Amazon SQS queues for `SummariesToTranslate` and `TranslatedSummaries` respectively.

2. Build and push the container to your own docker repository. Replace {username} with the username of your docker account.
```
cd ~/concise/translation-api
docker build -t translation-api .
docker tag translation-api:latest {username}/translation-api:latest
docker push {username}/translation-api:latest
```
3. Load and start the docker container on a service of your choosing.
4. Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.

# Extension
This application contains a Google Chrome extension that wraps the NextJS frontend.
### Setup
The extension can be loaded into chrome directly without any modification.
1. Open Google Chrome.
2. Click on the three-dot menu icon in the top right corner of the browser window and select "More Tools", then "Extensions" from the dropdown menu.
3. On the Extensions page, you'll find a toggle on the top right for "Developer mode". Ensure it is enabled.
4. Once you enable Developer Mode, you'll see additional options at the top of the page, including "Load unpacked".
5. Click on "Load unpacked", and a file dialog will pop up.
7. Select the directory (folder) with the extension's code i.e. (~/Downloads/concise/extension) and click the "Open" button.
8. You should see the extension icon in your Chrome toolbar.

# Frontend
This is a NextJS application that provides the entire frontend for Concise.
### Setup
1. Run ```cd ~/concise/frontend```
2. Run ```npm install```
3. Run ```npm run dev```


