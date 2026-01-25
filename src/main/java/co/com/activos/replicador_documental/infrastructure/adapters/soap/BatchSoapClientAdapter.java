package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchSoapClientAdapter {
    
    private final SoapClientAdapter soapClientAdapter;
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // Reducido de 20 a 10
    
    /**
     * Procesa múltiples solicitudes SOAP en paralelo
     */
    public Map<String, SolicitarArchivoResponse> solicitarArchivosBatch(List<String> codigosCliente) {
        Instant start = Instant.now();
        
        List<CompletableFuture<Map.Entry<String, SolicitarArchivoResponse>>> futures = codigosCliente.stream()
                .map(codigo -> CompletableFuture.supplyAsync(() -> {
                    try {
                        SolicitarArchivoRequest request = new SolicitarArchivoRequest(codigo);
                        SolicitarArchivoResponse response = soapClientAdapter.solicitarArchivo(request);
                        return Map.entry(codigo, response);
                    } catch (Exception e) {
                        log.error("Error solicitando archivo para cliente {}: {}", codigo, e.getMessage());
                        return Map.entry(codigo, (SolicitarArchivoResponse) null);
                    }
                }, executor))
                .collect(Collectors.toList());
        
        // Esperar a que terminen todas las solicitudes
        Map<String, SolicitarArchivoResponse> resultados = new ConcurrentHashMap<>();
        futures.stream()
                .map(CompletableFuture::join)
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> resultados.put(entry.getKey(), entry.getValue()));
        
        Duration duration = Duration.between(start, Instant.now());
        log.info("Batch SOAP completado: {} de {} solicitudes en {} ms", 
                resultados.size(), codigosCliente.size(), duration.toMillis());
        
        return resultados;
    }
    
    /**
     * Procesa documentos en batches optimizados
     */
    public void procesarDocumentosEnBatches(List<String> codigosCliente, 
                                           DocumentoProcessor processor,
                                           int batchSize) {
        int total = codigosCliente.size();
        
        for (int i = 0; i < total; i += batchSize) {
            int endIndex = Math.min(i + batchSize, total);
            List<String> batch = codigosCliente.subList(i, endIndex);
            
            log.info("Procesando batch {}/{} ({} documentos)", 
                    (i / batchSize) + 1, (total + batchSize - 1) / batchSize, batch.size());
            
            Map<String, SolicitarArchivoResponse> respuestas = solicitarArchivosBatch(batch);
            
            // Procesar respuestas en paralelo
            respuestas.entrySet().parallelStream()
                    .forEach(entry -> processor.procesar(entry.getKey(), entry.getValue()));
        }
    }
    
    @FunctionalInterface
    public interface DocumentoProcessor {
        void procesar(String codigoCliente, SolicitarArchivoResponse response);
    }
}
