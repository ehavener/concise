# Use an official Python runtime as a parent image
FROM python:3.10 as builder

# Install git
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Set the working directory in the container to /app
WORKDIR /app

# Install any needed packages specified in requirements.txt
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Install latest version of transformers
RUN pip install -q -U git+https://github.com/huggingface/transformers.git
RUN pip install -q -U git+https://github.com/huggingface/accelerate.git

# This is the second stage where we actually run the application
FROM python:3.10 as runner

# Set the working directory in the container to /app
WORKDIR /app

# Copy the dependencies from the builder stage
COPY --from=builder /usr/local/lib/python3.10/site-packages /usr/local/lib/python3.10/site-packages

# Install Uvicorn for command line usage
RUN pip install uvicorn

# Add the current directory contents into the container at /app
COPY . .

# Run the command to start uvicorn
CMD ["uvicorn", "main:app"]