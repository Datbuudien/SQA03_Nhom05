# Stage 1: Build với Maven
FROM eclipse-temurin:21-jdk-alpine AS maven-build
WORKDIR /build
COPY pom.xml .
# Download dependencies trước (cache layer)
RUN apk add --no-cache curl && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Extract layers
FROM eclipse-temurin:21-jre-alpine AS layer-extract
WORKDIR /app
COPY --from=maven-build /build/target/api-gateway-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Final image
FROM eclipse-temurin:21-jre-alpine
COPY --from=layer-extract /app/dependencies/ ./
COPY --from=layer-extract /app/spring-boot-loader/ ./
COPY --from=layer-extract /app/snapshot-dependencies/ ./
COPY --from=layer-extract /app/application/ ./

EXPOSE 8080
ENV JAVA_OPTS="-Xmx384m -Xms128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
