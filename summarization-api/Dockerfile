# Use an official Python runtime as a parent image
FROM python:3.8-slim-buster

# Set the working directory in the container to /app
WORKDIR /app

# Add the current directory contents into the container at /app
ADD . /app

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Make port 80 available to the world outside this container
EXPOSE 8001

# Run the command to start uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]

# docker build -t summarization-api .
# docker run -e API_KEY=... -p 8001:8001 summarization-api
# docker tag summarization-api:latest ehavener/summarization-api:latest
# docker push ehavener/summarization-api:latest

# If the CMD does not run automatically you can still start the application by running the below command from within a container instance:
# API_KEY=... uvicorn main:app --host 0.0.0.0 --port 8001
