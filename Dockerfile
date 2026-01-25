# Use Eclipse Temurin OpenJDK 17 as base image
FROM eclipse-temurin:17-jdk-alpine

# Crear el grupo y usuario
RUN addgroup -S devopsc && adduser -S javams -G devopsc

# Crear y asignar permisos al directorio
RUN mkdir -p /opt/app/config && chmod -R 755 /opt/app/config

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Copiar el JAR al directorio de la aplicación
COPY target/replicador-documental-0.0.1-SNAPSHOT.jar /opt/app/app.jar

# Cambiar al usuario sin privilegios
USER javams:devopsc

# Definir variables de entorno y volumen
ENV JAVA_OPTS=""
VOLUME /tmp

# Exponer el puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/app/app.jar " ]
