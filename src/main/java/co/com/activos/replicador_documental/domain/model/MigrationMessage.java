package co.com.activos.replicador_documental.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
