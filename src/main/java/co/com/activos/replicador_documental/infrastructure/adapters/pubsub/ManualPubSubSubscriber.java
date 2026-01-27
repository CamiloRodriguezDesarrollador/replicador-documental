package co.com.activos.replicador_documental.infrastructure.adapters.pubsub;

import co.com.activos.replicador_documental.domain.usecase.ReplicarUseCase;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualPubSubSubscriber {
    
    private final ReplicarUseCase replicarUseCase;
    
    @Value("${activos.gcp.pubsub.project-id}")
    private String projectId;
    
    @Value("${activos.gcp.pubsub.topics.vinculacion.name}")
    private String topicName;
    
    private Subscriber subscriber;
    
    @PostConstruct
    public void startListening() {
        try {
            String subscriptionId = topicName + "-sub";
            ProjectSubscriptionName subName = ProjectSubscriptionName.of(projectId, subscriptionId);
            
            MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
                try {
                    String payload = message.getData().toStringUtf8();
                    log.info("Mensaje recibido de Pub/Sub: {}", payload);
                    
                    // Procesar el mensaje
                    String result = replicarUseCase.ejecutarPorAnio(payload);
                    log.info("Mensaje procesado exitosamente: {}", result);
                    
                    // Acknowledge el mensaje
                    consumer.ack();
                    
                } catch (Exception e) {
                    log.error("Error procesando mensaje de Pub/Sub: {}", e.getMessage(), e);
                    // No hacer ack para que se reintente
                    consumer.nack();
                }
            };
            
            Subscriber subscriber = Subscriber.newBuilder(subName, receiver).build();
            subscriber.startAsync().awaitRunning();
            
            log.info("Pub/Sub listener started: subscription='{}'", subscriptionId);
            
        } catch (Exception e) {
            log.error("Error starting listener for subscription", e);
            throw new RuntimeException("Error iniciando Pub/Sub Subscriber", e);
        }
    }
    
    @PreDestroy
    public void stopListening() {
        if (subscriber != null) {
            subscriber.stopAsync();
            log.info("Subscriber de Pub/Sub detenido");
        }
    }
}
