#!/bin/bash

docker network create bravo-network

docker run --rm -e bravodb.discovery.server.self.host=bravodb1 \
  -e bravodb.discovery.server.self.port=8919 \
  --name bravodb1 \
  --network bravo-network \
  bravo/bravodb:0.1

docker run --rm -e bravodb.discovery.server.self.host=bravodb2 \
  -e bravodb.discovery.server.self.port=8919 \
  -e bravodb.discovery.server.other.host=bravodb1 \
  -e bravodb.discovery.server.other.port=8919 \
  --name bravodb2 \
  --network bravo-network \
  bravo/bravodb:0.1

docker run --rm -e bravodb.discovery.server.self.host=bravodb3 \
  -e bravodb.discovery.server.self.port=8919 \
  -e bravodb.discovery.server.other.host=bravodb2 \
  -e bravodb.discovery.server.other.port=8919 \
  --name bravodb3 \
  --network bravo-network \
  bravo/bravodb:0.1
