#!/bin/bash

echo "🛑 Step 1: Stop running containers..."
docker compose stop data-provider-app heatmap-ui-app stock-data-app

echo "🗑️  Step 2: Remove old containers..."
docker rm data-provider-app 2>/dev/null
docker rm stock-data-app 2>/dev/null
docker rm heatmap-ui-app 2>/dev/null

echo "🔨 Step 3: Maven build + Docker image build..."

cd project-data-provider
mvn clean install -DskipTests
docker build -t project-data-provider:0.0.1 -f Dockerfile .
cd ..

cd project-stock-data
mvn clean install -DskipTests
docker build -t project-stock-data:0.0.1 -f Dockerfile .
cd ..

cd project-heatmap-ui
mvn clean install -DskipTests
docker build -t project-heatmap-ui:0.0.1 -f Dockerfile .
cd ..

echo "🚀 Step 4: Start all containers..."
docker compose up -d

echo "✅ Done! All services running."
echo "   - data-provider:  http://localhost:8101"
echo "   - stock-data:     http://localhost:8102"
echo "   - heatmap-ui:     http://localhost:8080"
