# Use an official Python runtime as a parent image
FROM python:3.8-slim-buster as builder

# Set the working directory in the container to /app
WORKDIR /app

# Install any needed packages specified in requirements.txt
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Install pinned protobuf version
RUN pip install protobuf==3.20.0

# This is the second stage where we actually run the application
FROM python:3.8-slim-buster as runner

# Set the working directory in the container to /app
WORKDIR /app

# Copy the dependencies from the builder stage
COPY --from=builder /usr/local/lib/python3.8/site-packages /usr/local/lib/python3.8/site-packages

# Add the current directory contents into the container at /app
COPY . .

# Make port 8001 available to the world outside this container
EXPOSE 8001

# Run the command to start uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]

# docker build -t summarization-api .
# docker run -e API_KEY=... -p 8001:8001 summarization-api
# docker tag summarization-api:latest ehavener/summarization-api:latest
# docker push ehavener/summarization-api:latest

# If the CMD does not run automatically you can still start the application by running the below command from within a container instance:
# API_KEY=... uvicorn main:app --host 0.0.0.0 --port 8001
