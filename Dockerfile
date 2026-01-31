FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY ./infrastructure/build ./
COPY ./web/loveable/dist ./web/loveable/dist
EXPOSE 8080
ENTRYPOINT ["/app/install/infrastructure/bin/infrastructure"]
