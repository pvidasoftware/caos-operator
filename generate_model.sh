#!/bin/bash

docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$(pwd)":"$(pwd)" \
      -ti --network host ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6  \
      /generate.sh -u $(pwd)/src/k8s/crd.yml -n com.puravida.caos -p com.puravida.caos \
      -o $(pwd)

sudo chown -R $(whoami):$(whoami) src/main/java/*