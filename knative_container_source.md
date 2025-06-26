# Kamelet as ContainerSource workload

As discussed in the [`README.md`](./README.md) for building (and pushing) the container image is build via the following Maven command:

```shell
./mvnw package -Dquarkus.container-image.build=true [-Dquarkus.container-image.push=true]
```

Once the image is in an accessible OCI registry, it can be easily used to serve as a `ContainerSource` workload from Knative:

```yaml
apiVersion: sources.knative.dev/v1
kind: ContainerSource
metadata:
  name: star-wars-source
spec:
  template:
    spec:
      containers:
        - image: quay.io/christophd/star-wars-source:1.0-SNAPSHOT
          name: star-wars-source
          env:
            - name: CAMEL_KAMELET_STAR_WARS_SOURCE_RESOURCE
              value: "starships"
  sink:
    ref:
      apiVersion: eventing.knative.dev/v1
      kind: Broker
      name: default
```
