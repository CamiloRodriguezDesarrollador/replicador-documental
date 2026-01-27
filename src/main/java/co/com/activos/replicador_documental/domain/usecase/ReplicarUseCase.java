package co.com.activos.replicador_documental.domain.usecase;

import co.com.activos.replicador_documental.domain.model.*;
import co.com.activos.replicador_documental.helpers.UseCase;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.DocumentRegistrationClient;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.BatchSoapClientAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientManualImpl;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import com.activos.gcp.pubsub.annotation.Listener;
import co.com.activos.replicador_documental.domain.model.MigrationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicarUseCase implements UseCase<Long, String> {

    private final ParamRepository paramRepository;
    private final AzDigitalRepository azDigitalRepository;
    private final SoapClientManualImpl soapClientManual;
    private final BatchSoapClientAdapter batchSoapClientAdapter;
    private final DocumentRegistrationClient documentRegistrationClient;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100; // Batch más pequeño para mejor control
    private static final int PARALLEL_THREADS = 8; // Reducido para evitar thread starvation
    private static final int MAX_PAGES = 2000000; // Límite para manejar hasta 100,000 carpetas (50 x 2000)

    @Override
    public String ejecutar(Long txpCodigo) {
        long executionId = System.currentTimeMillis();
        String txpCodigoStr = txpCodigo.toString();
        
        // Contadores para tracking
        AtomicLong totalCarpetas = new AtomicLong(0);
        AtomicLong totalDocumentos = new AtomicLong(0);
        AtomicLong documentosMigrados = new AtomicLong(0);
        AtomicLong documentosFallidos = new AtomicLong(0);
        
        // Validar cantidad total de carpetas
        Long totalCarpetasBD = paramRepository.contarCarpetasPorTipoFlujo(txpCodigo);
        log.info("VALIDACIÓN - Total carpetas en BD para txpCodigo {}: {}", txpCodigo, totalCarpetasBD);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas más pequeñas para mejor distribución
            int pageSize = 50; // Página más pequeña para mejor paralelismo
            int pageNumber = 0;
            
            while (pageNumber < MAX_PAGES) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujo(txpCodigo, pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        // log.info("No hay más parámetros. Fin de la migración."); // Comentado para velocidad
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo con timeout
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, totalCarpetas, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página con timeout de 5 minutos
                    try {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .get(5, TimeUnit.MINUTES);
                    } catch (java.util.concurrent.TimeoutException e) {
                        log.error("Timeout procesando página {}. Continuando con la siguiente.", pageNumber);
                        // Cancelar futures que no terminaron
                        futures.forEach(f -> f.cancel(true));
                    }
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(10); // Más corta para mayor velocidad
                    
                } catch (Exception e) {
                    log.error("Error procesando página {} de parámetros: {}", pageNumber, e.getMessage());
                    pageNumber++;
                    continue;
                }
            }
            
            if (pageNumber >= MAX_PAGES) {
                // log.warn("Se alcanzó el límite máximo de páginas ({}). Deteniendo procesamiento.", MAX_PAGES); // Comentado para velocidad
            }
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Log final del proceso
        long tiempoTotal = (System.currentTimeMillis() - executionId) / 1000;
        
        // log.info("Migración completada - Carpetas: {}, Migrados: {}, Fallidos: {}, Tiempo: {}s", 
        //         totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get(), tiempoTotal); // Comentado para velocidad
        
        return String.format("Migración completada. Carpetas: %d, Migrados: %d, Fallidos: %d", 
                totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get());
    }


    @Listener("migration_sb")
    public String ejecutarPorAnio(String payload) {
        try {
            // Deserializar el mensaje JSON
            MigrationMessage message = objectMapper.readValue(payload, MigrationMessage.class);
            
            log.info("Procesando migración asíncrona - txpCodigo: {}, año: {}, messageId: {}", 
                    message.getTxpCodigo(), message.getAnio(), message.getMessageId());
            
            long executionId = System.currentTimeMillis();
            String txpCodigoStr = message.getTxpCodigo().toString();
            int anio = message.getAnio();
        
        // Contadores para tracking
        AtomicLong totalCarpetas = new AtomicLong(0);
        AtomicLong totalDocumentos = new AtomicLong(0);
        AtomicLong documentosMigrados = new AtomicLong(0);
        AtomicLong documentosFallidos = new AtomicLong(0);
        
        // Validar cantidad total de carpetas por año
        Long totalCarpetasBD = paramRepository.contarCarpetasPorTipoFlujoYAnio(message.getTxpCodigo(), message.getAnio());
        log.info("VALIDACIÓN - Total carpetas en BD para txpCodigo {} año {}: {}", message.getTxpCodigo(), message.getAnio(), totalCarpetasBD);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas más pequeñas para mejor distribución
            int pageSize = 50; // Página más pequeña para mejor paralelismo
            int pageNumber = 0;
            
            while (pageNumber < MAX_PAGES) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujoYAnio(message.getTxpCodigo(), message.getAnio(), pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        // log.info("No hay más parámetros para el año {}. Fin de la migración.", message.getAnio()); // Comentado para velocidad
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo con timeout
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, totalCarpetas, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página con timeout de 5 minutos
                    try {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .get(5, TimeUnit.MINUTES);
                    } catch (java.util.concurrent.TimeoutException e) {
                        log.error("Timeout procesando página {} (año {}). Continuando con la siguiente.", pageNumber, message.getAnio());
                        // Cancelar futures que no terminaron
                        futures.forEach(f -> f.cancel(true));
                    }
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(10); // Más corta para mayor velocidad
                    
                } catch (Exception e) {
                    log.error("Error procesando página {} de parámetros del año {}: {}", pageNumber, message.getAnio(), e.getMessage());
                    pageNumber++;
                    continue;
                }
            }
            
            if (pageNumber >= MAX_PAGES) {
                // log.warn("Se alcanzó el límite máximo de páginas ({}). Deteniendo procesamiento del año {}.", MAX_PAGES, message.getAnio()); // Comentado para velocidad
            }
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Log final del proceso
        long tiempoTotal = (System.currentTimeMillis() - executionId) / 1000;
        
        // log.info("Migración año {} completada - Carpetas: {}, Migrados: {}, Fallidos: {}, Tiempo: {}s", 
        //         message.getAnio(), totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get(), tiempoTotal); // Comentado para velocidad
        
        return String.format("Migración año %d completada. Carpetas: %d, Migrados: %d, Fallidos: %d", 
                message.getAnio(), totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get());
                
        } catch (Exception e) {
            log.error("Error procesando migración asíncrona: {}", e.getMessage(), e);
            return "Error en migración asíncrona: " + e.getMessage();
        }
    }
    
    // Método de conteo eliminado - ahora el total se va descubriendo gradualmente durante el procesamiento
    
    private void procesarCarpeta(TaxonomiaParam param, String txpCodigoStr, long executionId,
                                AtomicLong totalDocumentos, AtomicLong totalCarpetas, AtomicLong documentosMigrados, AtomicLong documentosFallidos) throws InterruptedException {
        
        // Extraer todos los códigos de cliente de esta carpeta
        List<String> codigosCliente = new ArrayList<>();
        List<AzDigital> todosLosAzDigitales = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        List<AzDigital> azDigitales;
        
        do {
            azDigitales = azDigitalRepository.listarPorCarpeta(param.getCodigo(), pageable);
            totalDocumentos.addAndGet(azDigitales.size());
            
            // Recolectar códigos y AzDigitales para procesamiento batch
            codigosCliente.addAll(azDigitales.stream()
                    .map(AzDigital::getCodigoCli)
                    .collect(Collectors.toList()));
            
            todosLosAzDigitales.addAll(azDigitales);
            
            pageable = pageable.next();
        } while (!azDigitales.isEmpty());
        
        if (codigosCliente.isEmpty()) {
            return;
        }
        
        // Mostrar total de documentos a procesar
        long currentTotal = totalDocumentos.get();
        if (currentTotal > 0 && currentTotal % 1000 == 0) {
            log.info("Total documentos descubiertos: {}", currentTotal);
        }
        
        // Procesamiento secuencial por carpeta para evitar thread starvation
        // Usar batches pequeños pero sin executor anidado
        int batchSize = 20; // Batch un poco más grande para eficiencia
        
        // Crear Map para búsqueda rápida de AzDigital
        Map<String, AzDigital> azDigitalMap = todosLosAzDigitales.stream()
                .collect(Collectors.toMap(AzDigital::getCodigoCli, az -> az));
        
        // Dividir en batches para procesamiento
        for (int i = 0; i < codigosCliente.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, codigosCliente.size());
            List<String> batch = codigosCliente.subList(i, endIndex);
            
            // Procesar batch directamente sin threads adicionales
            for (String codigoCliente : batch) {
                try {
                    // Búsqueda rápida con Map en lugar de stream
                    AzDigital azDigital = azDigitalMap.get(codigoCliente);
                    
                    if (azDigital == null) {
                        continue;
                    }
                    
                    // Usar SOAP manual para descargar el archivo
                    SolicitarArchivoResponse soapResponse = soapClientManual.solicitarArchivo(codigoCliente);
                    
                    if (soapResponse == null || soapResponse.getArchivo() == null) {
                        continue;
                    }
                    
                    // Procesar documento
                    DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, soapResponse, param);
                    documentRegistrationClient.registerDocument(request);
                    
                    documentosMigrados.incrementAndGet();
                    
                    // Log de progreso cada 100 documentos
                    long current = documentosMigrados.get() + documentosFallidos.get();
                    if (current % 100 == 0) {
                        log.info("Progreso: Docs {}/{} - Carpetas: {}", current, totalDocumentos.get(), totalCarpetas.get());
                    }
                    
                } catch (Exception e) {
                    log.error("Error procesando documento {}: {}", codigoCliente, e.getMessage());
                    documentosFallidos.incrementAndGet();
                    
                    // Log de progreso cada 100 documentos (incluyendo fallidos)
                    long current = documentosMigrados.get() + documentosFallidos.get();
                    if (current % 100 == 0) {
                        log.info("Progreso: Docs {}/{} - Carpetas: {}", current, totalDocumentos.get(), totalCarpetas.get());
                    }
                }
            }
            
            // Pequeña pausa entre batches para no sobrecargar el servidor
            if (i + batchSize < codigosCliente.size()) {
                Thread.sleep(5);
            }
        }
    }
    
    private void procesarDocumento(AzDigital azDigital, TaxonomiaParam param, String txpCodigoStr, long executionId,
                                 AtomicLong documentosMigrados, AtomicLong documentosFallidos) {
        try {
            // log.debug("Procesando documento individual: {}", azDigital.getCodigoCli()); // Comentado para velocidad
            
            SolicitarArchivoResponse archivoResponse = soapClientManual.solicitarArchivo(azDigital.getCodigoCli());
            
            if (archivoResponse == null || archivoResponse.getArchivo() == null) {
                // log.warn("Respuesta vacía para documento: {}", azDigital.getCodigoCli()); // Comentado para velocidad
                documentosFallidos.incrementAndGet();
                return;
            }
            
            DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, archivoResponse, param);
            documentRegistrationClient.registerDocument(request);
            
            // log.debug("Documento {} procesado exitosamente", azDigital.getCodigoCli()); // Comentado para velocidad
            
            // Extraer tipo y número de documento para el log
            DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
            
            // Log de éxito - COMENTADO para no insertar en BigQuery
            /*
            bigQueryAdapter.logSuccess(azDigital, 
                    Collections.singletonList(azDigital.getCodigoCli()), 
                    txpCodigoStr, executionId, 
                    docParams.getTipoDocTrabajador(), 
                    docParams.getDocumentoTrabajador());
            */
            
            documentosMigrados.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error al solicitar archivo para cliente {}: {}", azDigital.getCodigoCli(), e.getMessage(), e);
            
            // Extraer tipo y número de documento para el log de error también
            DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
            
            // Log de error - COMENTADO para no insertar en BigQuery
            /*
            bigQueryAdapter.logError(azDigital, e.getMessage(), txpCodigoStr, executionId, 
                    docParams.getTipoDocTrabajador(), 
                    docParams.getDocumentoTrabajador());
            */
            
            documentosFallidos.incrementAndGet();
        }
    }

    private DocumentRegistrationRequest crearDocumentRegistrationRequest(
            AzDigital azDigital, SolicitarArchivoResponse archivoResponse,
            TaxonomiaParam param) {

        return new DocumentRegistrationRequest(
                azDigital.getPrdCodigo(),
                base64ToMultipart( archivoResponse.getArchivo().getContenido(), archivoResponse.getArchivo().getNombre(), archivoResponse.getArchivo().getContenido()),
                extraerTipoYNumeroDocumento(param.getNombre()));
    }

    private MultipartFile base64ToMultipart(String base64, String fileName, String contentType) {
        byte[] fileBytes = Base64.getDecoder().decode(base64);
        return new MockMultipartFile(fileName, fileName, contentType, fileBytes);
    }


    private DocumentRegistrationRequest.DocumentParams extraerTipoYNumeroDocumento(String nombre) {
        String[] partes = nombre.split(" ");
        String tipoDoc = partes.length > 1 ? partes[1] : "";
        String numeroDoc = partes.length > 2 ? partes[2] : "";
        return new DocumentRegistrationRequest.DocumentParams(tipoDoc, numeroDoc);
    }


}
