#!/bin/bash
echo "----------- Initializing LocalStack Resources -----------"

awslocal sns create-topic --name ore-sim
awslocal kinesis create-stream --stream-name ore-facts --shard-count 2