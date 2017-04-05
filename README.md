# About

ARLAS-server provides a highly simplified **REST API** for exploring data collections available in **ElasticSearch**. 
**Enhanced capabilities** are provided for collections exposing a **geometry**, a **centroid** and a **timestamp**. An **Admin API** is also provided for managing collections.

The exploration API is described [here](doc/api/API-definition.md) while the  Admin API is described [here](doc/api/API-Admin-definition.md).

# Build

```sh
mvn clean install
```

# [OPTIONAL] Zipkin monitoring
In order to monitor the REST service performances in ZIPKIN:
- Enable zipkin in configuration.yaml
- Then:

```sh
wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
java -jar zipkin.jar &
```

# Run
```sh
java -jar target/elastic-tiler-1.0.jar server configuration.yaml
```

