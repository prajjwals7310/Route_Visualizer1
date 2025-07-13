#!/bin/bash

CONTAINER_NAME=route-visualizer18
IMAGE_NAME=aryanmamania17/route-visualizer18:latest
WORKDIR=$(pwd)
HOST_PORT=8001

# Stop & remove old container if exists
docker rm -f "$CONTAINER_NAME" 2>/dev/null

# Run new container
docker run -d --name "$CONTAINER_NAME" \
  -p "$HOST_PORT:8001" \
  -v "$WORKDIR/input:/app/input" \
  -v "$WORKDIR/data.cfg:/app/data.cfg" \
  -v "$WORKDIR/Component_Generator/Component_Mapping:/app/Component_Generator/Component_Mapping" \
  -v "$WORKDIR/visualizer:/var/www/html/visualizer" \
  -v "$WORKDIR/htpasswd:/etc/nginx/.htpasswd" \
  "$IMAGE_NAME"
