#!/usr/bin/env bash

./mvnw clean package -DskipTests=true;

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 299541157397.dkr.ecr.us-east-1.amazonaws.com/wealth-engine;

docker build -t wealth-engine . ;

docker tag wealth-engine:latest 299541157397.dkr.ecr.us-east-1.amazonaws.com/wealth-engine:latest;

docker push 299541157397.dkr.ecr.us-east-1.amazonaws.com/wealth-engine:latest;
