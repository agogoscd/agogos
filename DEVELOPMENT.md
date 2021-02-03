# Development

This document describes the development workflow.

## Development environment

Please make sure you have following requirements installed:

* [Minikube](https://minikube.sigs.k8s.io/docs/start/)
  * [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
* [Tekton](https://tekton.dev/docs/getting-started/#installation)
* [JDK 11](https://sdkman.io/jdks#jdk.java.net)

## Registering CRD's

Once you have required tools installed and running on your system the next thing
to do is to register CRD's within the system. This can be done by running this command:

```
./src/main/resources/crds/apply.sh
```

> **NOTE:** Each time after you change the CRD yu need to run the command again.

## Running the development mode

You can run application in dev mode that enables live coding using:

```
./mvnw quarkus:dev
```

## Testing

TBD
