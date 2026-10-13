FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY ChatterServer.java .
RUN apk add --no-cache openjdk21-jdk && javac ChatterServer.java

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/ChatterServer*.class .
# Change internal port map rules to bypass the web proxy engine mapping layout walls
EXPOSE 8080
ENV PORT=8080
CMD ["java", "ChatterServer"]
