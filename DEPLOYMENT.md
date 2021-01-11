# Application deployment

TBD

## Creating a native executable

You can create a native executable using:

```
./mvnw clean package -Pnative -Dquarkus.native.container-runtime=podman -Dquarkus.native.container-build=true
```

Later it can be added into a container image:

```
podman build -f src/main/docker/Dockerfile.native -t service:latest .
```

Such image is ready to be run in production.
