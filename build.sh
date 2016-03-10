#!/bin/sh -e
mvn clean install
docker build -t exareme .

