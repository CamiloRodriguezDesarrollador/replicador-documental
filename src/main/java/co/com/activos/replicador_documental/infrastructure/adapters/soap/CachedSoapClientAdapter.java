package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedSoapClientAdapter implements SoapClientAdapter {
    
    private final SoapClientAdapter soapClientAdapter;
    
    @Override
    @Cacheable(value = "soap-responses", key = "#request.codigoCliente", 
               unless = "#result == null || #result.archivo == null")
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request) {
        return solicitarArchivo(request.getId());
    }

    @Override
    @Cacheable(value = "soap-responses", key = "#docId", 
               unless = "#result == null || #result.archivo == null")
    public SolicitarArchivoResponse solicitarArchivo(String docId) {
        Instant start = Instant.now();
        
        try {
            SolicitarArchivoResponse response = soapClientAdapter.solicitarArchivo(docId);
            
            Duration duration = Duration.between(start, Instant.now());
            log.info("SOAP request (sin caché) para cliente {} en {} ms", 
                    docId, duration.toMillis());
            
            return response;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Error en SOAP request para cliente {} después de {} ms: {}", 
                    docId, duration.toMillis(), e.getMessage());
            throw e;
        }
    }
}
