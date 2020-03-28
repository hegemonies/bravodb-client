#!/bin/bash

docker run -it --rm \
 -e bravodb.server.self.host=bravodbclient1 \
 -e bravodb.server.self.port=8919 \
 -e bravodb.server.other.host=bravodb2 \
 -e bravodb.server.other.port=8919 \
 --name bravodbclient1 \
 --network docker_bravo-network \
 -v /home/dan-dy/git/bravodb-client/build/libs:/tmp \
 openjdk:11 /bin/bash
