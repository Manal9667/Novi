# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY norton-root.cer /tmp/norton-root.cer
RUN keytool -importcert -noprompt -trustcacerts \
	-alias norton-web-mail-shield \
	-file /tmp/norton-root.cer \
	-keystore "$JAVA_HOME/lib/security/cacerts" \
	-storepass changeit
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
