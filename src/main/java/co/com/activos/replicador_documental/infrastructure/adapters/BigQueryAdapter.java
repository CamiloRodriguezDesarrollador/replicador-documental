package co.com.activos.replicador_documental.infrastructure.adapters;

import co.com.activos.replicador_documental.domain.model.AzDigital;
import co.com.activos.replicador_documental.domain.model.MigrationLog;
import co.com.activos.replicador_documental.domain.model.MigrationLogRepository;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.TableId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigQueryAdapter implements MigrationLogRepository {

    private final BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${bigquery.dataset.name}")
    private String datasetName;

    @Value("${bigquery.table.name}")
    private String tableName;

    @Override
    public void save(MigrationLog migrationLog) {
        try {
            TableId tableId = TableId.of(projectId, datasetName, tableName);
            Map<String, Object> rowContent = new HashMap<>();
            
            rowContent.put("cedula", migrationLog.getCedula());
            rowContent.put("tipo", migrationLog.getTipo());
            rowContent.put("id_documentos_migrados", migrationLog.getIdDocumentosMigrados());
            rowContent.put("id_documentos_fallidos", migrationLog.getIdDocumentosFallidos());
            rowContent.put("total_documentos", migrationLog.getTotalDocumentos());
            rowContent.put("documentos_migrados", migrationLog.getDocumentosMigrados());
            rowContent.put("documentos_fallidos", migrationLog.getDocumentosFallidos());
            rowContent.put("status", migrationLog.getStatus());
            rowContent.put("error_message", migrationLog.getErrorMessage());
            rowContent.put("fecha_migracion", migrationLog.getFechaMigracion().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            rowContent.put("txp_codigo", migrationLog.getTxpCodigo());
            rowContent.put("execution_id", migrationLog.getExecutionId());

            InsertAllRequest insertRequest = InsertAllRequest.newBuilder(tableId)
                    .addRow(rowContent)
                    .build();

            var response = bigquery.insertAll(insertRequest);
            
            if (response.hasErrors()) {
                log.error("Error inserting log to BigQuery: {}", response.getInsertErrors());
            }
        } catch (Exception e) {
            log.error("Failed to insert log to BigQuery", e);
            throw new RuntimeException("Failed to log to BigQuery", e);
        }
    }

    @Override
    public void saveAll(List<MigrationLog> migrationLogs) {
        if (migrationLogs == null || migrationLogs.isEmpty()) {
            return;
        }

        try {
            TableId tableId = TableId.of(projectId, datasetName, tableName);
            InsertAllRequest.Builder requestBuilder = InsertAllRequest.newBuilder(tableId);

            for (MigrationLog log : migrationLogs) {
                Map<String, Object> rowContent = new HashMap<>();
                
                rowContent.put("cedula", log.getCedula());
                rowContent.put("tipo", log.getTipo());
                rowContent.put("id_documentos_migrados", log.getIdDocumentosMigrados());
                rowContent.put("id_documentos_fallidos", log.getIdDocumentosFallidos());
                rowContent.put("total_documentos", log.getTotalDocumentos());
                rowContent.put("documentos_migrados", log.getDocumentosMigrados());
                rowContent.put("documentos_fallidos", log.getDocumentosFallidos());
                rowContent.put("status", log.getStatus());
                rowContent.put("error_message", log.getErrorMessage());
                rowContent.put("fecha_migracion", log.getFechaMigracion().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                rowContent.put("txp_codigo", log.getTxpCodigo());
                rowContent.put("execution_id", log.getExecutionId());

                requestBuilder.addRow(rowContent);
            }

            InsertAllRequest insertRequest = requestBuilder.build();
            var response = bigquery.insertAll(insertRequest);
            
            if (response.hasErrors()) {
                log.error("Error inserting batch logs to BigQuery: {}", response.getInsertErrors());
            }
        } catch (Exception e) {
            log.error("Failed to insert batch logs to BigQuery", e);
            throw new RuntimeException("Failed to batch log to BigQuery", e);
        }
    }

    // Métodos de conveniencia para el ReplicarUseCase
    public void logSuccess(AzDigital azDigital, List<String> idDocumentosMigrados, String txpCodigo, long executionId, String tipoDocumento, String cedula) {
        MigrationLog log = MigrationLog.builder()
                .cedula(cedula != null ? cedula : azDigital.getPrdCodigo()) // Usar cédula real o fallback
                .tipo(tipoDocumento != null ? tipoDocumento : "cc") // Usar tipo real o fallback
                .idDocumentosMigrados(idDocumentosMigrados != null ? idDocumentosMigrados : Collections.emptyList())
                .idDocumentosFallidos(Collections.emptyList())
                .totalDocumentos(idDocumentosMigrados != null ? idDocumentosMigrados.size() : 0)
                .documentosMigrados(idDocumentosMigrados != null ? idDocumentosMigrados.size() : 0)
                .documentosFallidos(0)
                .status("SUCCESS")
                .fechaMigracion(LocalDateTime.now())
                .txpCodigo(txpCodigo)
                .executionId(executionId)
                .build();
        save(log);
    }

    public void logPartialSuccess(AzDigital azDigital, List<String> migrados, List<String> fallidos, String txpCodigo, long executionId) {
        MigrationLog log = MigrationLog.builder()
                .cedula(azDigital.getPrdCodigo())
                .tipo("cc")
                .idDocumentosMigrados(migrados != null ? migrados : Collections.emptyList())
                .idDocumentosFallidos(fallidos != null ? fallidos : Collections.emptyList())
                .totalDocumentos((migrados != null ? migrados.size() : 0) + (fallidos != null ? fallidos.size() : 0))
                .documentosMigrados(migrados != null ? migrados.size() : 0)
                .documentosFallidos(fallidos != null ? fallidos.size() : 0)
                .status("PARTIAL_SUCCESS")
                .fechaMigracion(LocalDateTime.now())
                .txpCodigo(txpCodigo)
                .executionId(executionId)
                .build();
        save(log);
    }

    public void logError(AzDigital azDigital, String errorMessage, String txpCodigo, long executionId, String tipoDocumento, String cedula) {
        MigrationLog log = MigrationLog.builder()
                .cedula(cedula != null ? cedula : azDigital.getPrdCodigo()) // Usar cédula real o fallback
                .tipo(tipoDocumento != null ? tipoDocumento : "cc") // Usar tipo real o fallback
                .idDocumentosMigrados(Collections.emptyList())
                .idDocumentosFallidos(Collections.singletonList(azDigital.getCodigoCli()))
                .totalDocumentos(1)
                .documentosMigrados(0)
                .documentosFallidos(1)
                .status("FAILED")
                .errorMessage(errorMessage)
                .fechaMigracion(LocalDateTime.now())
                .txpCodigo(txpCodigo)
                .executionId(executionId)
                .build();
        save(log);
    }
}
