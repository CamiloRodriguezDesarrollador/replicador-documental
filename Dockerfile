# Etapa de construcción: usa Maven y JDK 17
FROM maven:3.8.4-openjdk-17-slim AS dockerbuild

# Define directorio de trabajo
WORKDIR /app

# Copia todo el proyecto al contenedor
COPY . .

# Compila y empaqueta el JAR, omitiendo tests
RUN mvn clean package -DskipTests

# Etapa de ejecución: usa Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

# Establece la zona horaria reconocida por Oracle
ENV TZ=America/Bogota

RUN apt-get update && \
    apt-get install -y tzdata && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Define directorio de trabajo
WORKDIR /app

# Copia el .jar desde la etapa de construcción
COPY --from=dockerbuild /app/target/*.jar app.jar

# Expone el puerto de tu aplicación
EXPOSE 8080

# Ejecuta la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
