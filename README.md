# Concise Video Summarizer

### Introduction
Concise is a web application for summarizing YouTube videos. Concise was developed primarily as an exercise in Java Spring Boot development and as an exercise in applying Transformer language models to solve problems like summarization and machine translation. (goal is fully locally runnable)

### Features
  - One-Click Summarization: ...
  - Chapter Summarization: ...
  - Translation: ...

### Techniques
  - **Chrome Extension:** Provides quick access to the Concise application directly from any YouTube video.
  - **Transformers**: Provide detailed summaries of videos, which can be further translated into multiple languages.
  - **Authentication and User Management**: Standard user management system with JWT authentication for secure access.
  - **Microservices**: Dedicated microservices for fetching video transcripts, summarization, and translation, ensuring scalibility.

### Technologies
  - **Frontend:** TypeScript, Next.js, HTML5, CSS3
  - **Extension:** JavaScript
  - **Backend:** Java, Spring Boot
  - **Microservices:** Python, Transformer Language Models (jordiclive/flan-t5-11b-summarizer-filtered, gpt-3.5-turbo, facebook/nllb-200-3.3B, and oliverguhr/fullstop-punctuation-multilang-large)
  - **Authentication:** JWT (JSON Web Token)
  - **Third Party APIs:** YouTube Data API

### Architecture
Concise uses Spring Boot in conjunction with NLP-task-specific python microservices than interface using queues.

### Setup
Setting up the project requires following the setup instructions for each application. The recomended order is:

1. [Backend](https://github.com/ehavener/concise#backend)
2. [Transcript-api](https://github.com/ehavener/concise#transcript-api)
3. [Frontend](https://github.com/ehavener/concise#frontend)
4. [Extension](https://github.com/ehavener/concise#extension)
5. [Translation-api](https://github.com/ehavener/concise#translation-api)
6. [Summarization-api](https://github.com/ehavener/concise#summarization-api)

Each of these steps should run without requiring the higher order applications so that you can test as you go along. This is also the order for starting the services once you have them ready to deploy.

### Contributing
If you encounter any bugs or issues while running the project, or would like to improve it, please feel free to open an issue or contact me directly.

### License
...

### Application State Diagrams
Rectangle bacgrounds designate the service and squares/diamonds/squircles designate the state.
![application-state-diagram drawio (1)](https://github.com/ehavener/concise/assets/18430808/70b3ff97-0fdf-4866-a98b-cddaf32e7ec7)

### Organization

This project is organized into several sub-projects at the root level directory.

# Extension
This application is a Google Chrome extension that wraps the NextJS frontend.
### Setup


# Frontend
This is a NextJS application that provides the entire frontend for Concise.
### Setup


# Backend
This is a Java Spring Boot application that provides JWT authentication and user management, persists and serves summaries, and sends requests to respective python APIs when necessary.
### Setup


# Transcript-api
This is a python microservice that fetches video transcripts from YouTube's API. YouTube videos typically have multiple transcripts to choose from. This application selects the best available on the assumption that the transcript will be input LLM.

### Setup


# Summarization-api
This is a python microservice that summarizes transcripts using either `jordiclive/flan-t5-11b-summarizer-filtered` or `gpt-3.5-turbo`.
### Setup
This model requires GPU with a 24GB of VRAM to run such as a NVIDIA GTX 3090. You can rent one of these instances from vast.ai for around `$0.25` per hour. [LambdaLabs](https://lambdalabs.com/service/gpu-cloud#pricing), [CoreWeave](https://www.coreweave.com/gpu-cloud-pricing), and [Paperspace](https://www.paperspace.com/pricing) offer comparable but slightly higher rates and provide a reliable service. [Major](https://aws.amazon.com/sagemaker/pricing/) [cloud](https://azure.microsoft.com/en-us/pricing/details/machine-learning/#pricing) [providers](https://cloud.google.com/compute/gpus-pricing) also offer appropriate instances.

Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.

# Translation-api
This is a python microservice that translates transcripts using `facebook/nllb-200-3.3B`.
### Setup
The same GPU requirements specified in the `summarization-api` section apply to this service.

Once the docker container has started the service will be available. If for some reason it does not run you may run it manually from within the container.


