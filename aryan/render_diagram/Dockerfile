FROM eclipse-temurin:21-jdk

# Install Python, pip, Maven, Git
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    maven \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy application code
COPY . /app

# Install Python dependencies with override
RUN pip3 install --no-cache-dir --break-system-packages -r requirements.txt

# Make scripts executable
RUN chmod +x /app/entrypoint.sh /app/java_component/generate_report.sh

# Start app
ENTRYPOINT ["/app/entrypoint.sh"]
