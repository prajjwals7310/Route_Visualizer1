#!/bin/bash

IMAGE_NAME="aryanmamania17/route-visualizer:latest"
CONTAINER_NAME="camel-route-visualizer"

# Step 1: Check if Docker is installed
if ! command -v docker &> /dev/null
then
    echo "🚧 Docker not found. Installing Docker..."

    # Update & install prerequisites
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl gnupg lsb-release

    # Add Docker’s GPG key
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
      sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

    # Add Docker repo
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    echo "✅ Docker installed successfully."
else
    echo "✅ Docker is already installed."
fi

# Step 2: Pull your image
echo "📦 Pulling image: $IMAGE_NAME"
docker pull $IMAGE_NAME

# Step 3: Run container
echo "🚀 Running container..."
docker run -d --rm --name $CONTAINER_NAME -p 5000:5000 $IMAGE_NAME

echo "🌐 Application running at: http://localhost:5000"
