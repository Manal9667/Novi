# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Trust any extra root CAs dropped into certs/ before resolving dependencies.
# This is a no-op on a clean checkout (certs/ holds only .gitkeep), but lets the
# build work on a machine whose network does TLS interception (e.g. a corporate
# proxy or antivirus "web shield") by placing that root cert in backend/certs/.
COPY certs/ /tmp/certs/
RUN for f in /tmp/certs/*.cer; do [ -e "$f" ] || continue; \
      keytool -importcert -noprompt -trustcacerts -alias "extra-$(basename "$f")" \
      -file "$f" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; done

COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Skip compiling AND running tests in the image build: test-only dependencies
# aren't needed to produce the runnable jar.
RUN mvn -B clean package -Dmaven.test.skip=true

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Same extra-CA trust at runtime, so outbound HTTPS (Gemini, Open Library) works
# on a TLS-intercepting network. No-op when certs/ has no .cer files.
COPY certs/ /tmp/certs/
RUN for f in /tmp/certs/*.cer; do [ -e "$f" ] || continue; \
      keytool -importcert -noprompt -trustcacerts -alias "extra-$(basename "$f")" \
      -file "$f" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; done

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
