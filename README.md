## Caos Operator for Kubernetes

An operator for kubernetes who remove pods randomly

## Build 

`./gradlew build`

## Deploy

- install the [CustomerResource definition](src/k8s/crd.yml)
 
- create an [account and roles](src/k8s/operator-roles.yml)

- [Deploy the operator](src/k8s/deployment.yml)

## Creating the caos

- define what pods you want to be removed and [deploy the caos](src/k8s/moderate.yml) 


## See in action

https://fediverse.tv/w/oww2hR96eX8cs6PvdhW1RR

