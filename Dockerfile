FROM eclipse-temurin:21-jre
COPY /build/libs/main.jar main.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","/main.jar"]