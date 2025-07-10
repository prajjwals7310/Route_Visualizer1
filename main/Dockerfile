# Use official Python base image
FROM python:3.10-slim

# Set environment variables (modern format)
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Set working directory
WORKDIR /app

# Copy project files into the container
COPY . .

# Install bash and git (required by generate_report.sh scripts)
RUN apt-get update && apt-get install -y bash git && apt-get clean

# Remove any existing virtual envs, create a new one, activate it, and install dependencies
RUN rm -rf env venv && \
    python3 -m venv venv && \
    /bin/bash -c "source venv/bin/activate && pip install --upgrade pip && pip install -r requirements.txt"

# Make your main.sh executable
RUN chmod +x main.sh

# Expose the port used by Flask or other UI app
EXPOSE 5000

# Start your app through main.sh using the virtual environment
CMD ["/bin/bash", "-c", "source venv/bin/activate && ./main.sh"]
