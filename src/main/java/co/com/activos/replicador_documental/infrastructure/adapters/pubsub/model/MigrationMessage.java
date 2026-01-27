package co.com.activos.replicador_documental.infrastructure.adapters.pubsub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationMessage {
    
    private Long txpCodigo;
    private int anio;
    private String messageId;
    private String status;
    
}
