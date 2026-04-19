#!/bin/bash

cd ..

echo "Building api-gateway..."
cd api-gateway
mvn clean package -DskipTests
docker build -t api-gateway:latest .
cd ..

echo "Building account-service..."
cd account-service
mvn clean package -DskipTests
docker build -t account-service:latest .
cd ..

echo "Building react-flow-be..."
cd react-flow-be
mvn clean package -DskipTests
docker build -t react-flow-be:latest .
cd ..

echo "Building react-flow..."
cd react-flow
docker build -t react-flow:latest .
cd ..

echo "All services built successfully!"