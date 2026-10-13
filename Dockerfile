FROM eclipse-temurin:21-jdk
COPY ChatterServer.java .
RUN javac ChatterServer.java
EXPOSE 8080
CMD ["java", "ChatterServer"]
