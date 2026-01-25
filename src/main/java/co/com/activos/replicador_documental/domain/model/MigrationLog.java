package co.com.activos.replicador_documental.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationLog {
    private String cedula;
    private String tipo;
    private List<String> idDocumentosMigrados;
    private List<String> idDocumentosFallidos;
    private int totalDocumentos;
    private int documentosMigrados;
    private int documentosFallidos;
    private String status;
    private String errorMessage;
    private LocalDateTime fechaMigracion;
    private String txpCodigo;
    private long executionId;
}
