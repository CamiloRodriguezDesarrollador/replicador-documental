package co.com.activos.replicador_documental.infrastructure.adapters.pubsub;

import co.com.activos.replicador_documental.domain.model.MigrationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualPubSubPublisher {
    
    private final ObjectMapper objectMapper;
    
    @Value("${activos.gcp.pubsub.project-id}")
    private String projectId;
    
    @Value("${activos.gcp.pubsub.topics.vinculacion.name}")
    private String topicName;
    
    private Publisher publisher;
    
    @PostConstruct
    public void init() {
        try {
            TopicName topic = TopicName.of(projectId, topicName);
            this.publisher = Publisher.newBuilder(topic).build();
            log.info("Publisher de Google Cloud Pub/Sub inicializado para topic: {}", topicName);
        } catch (Exception e) {
            log.error("Error inicializando Publisher de Pub/Sub", e);
            throw new RuntimeException("Error inicializando Pub/Sub", e);
        }
    }
    
    public void publish(String topic, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(jsonMessage))
                    .build();
            
            publisher.publish(pubsubMessage);
            log.info("Mensaje publicado en Pub/Sub topic: {}", topic);
            
        } catch (Exception e) {
            log.error("Error publicando mensaje en Pub/Sub: {}", e.getMessage(), e);
            throw new RuntimeException("Error publicando en Pub/Sub", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (publisher != null) {
            try {
                publisher.shutdown();
                log.info("Publisher de Pub/Sub detenido");
            } catch (Exception e) {
                log.error("Error deteniendo Publisher de Pub/Sub", e);
            }
        }
    }
}
