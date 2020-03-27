#!/bin/bash

docker network create bravo-network

docker run --rm -e bravodb.server.self.host=bravodbclient1 \
  -e bravodb.server.self.port=8919 \
  -e bravodb.server.other.host=bravodb1 \
  -e bravodb.server.other.port=8919 \
  --name bravodbclient1 \
  --network bravo-network \
  bravo/bravodb-client:0.1
