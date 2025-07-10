#!/bin/bash

# Step 1: Check for Docker
if ! command -v docker &> /dev/null
then
    echo "🐋 Docker not found. Installing Docker..."

    # Update and install Docker for Ubuntu
    sudo apt-get update
    sudo apt-get install -y \
        ca-certificates \
        curl \
        gnupg \
        lsb-release

    # Add Docker’s official GPG key
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

    # Set up stable repo
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

    echo "✅ Docker installed successfully."
else
    echo "✅ Docker already installed."
fi

# Step 2: Pull Docker Image
echo "📥 Pulling Docker image: atresh/camelflow-visualizer:latest"
docker pull atresh/camelflow-visualizer:latest

# Step 3: Run the Container
echo "🚀 Running CamelFlow Visualizer on port 5000"
docker run -it -p 5000:5000 atresh/camelflow-visualizer:latest
