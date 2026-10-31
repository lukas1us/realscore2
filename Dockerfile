FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run on the host via `mvn verify`, not during the image build.
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home realscore
COPY --from=build /build/target/realscore-*.jar app.jar
USER realscore
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
