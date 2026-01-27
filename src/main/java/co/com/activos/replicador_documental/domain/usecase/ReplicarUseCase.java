package co.com.activos.replicador_documental.domain.usecase;

import co.com.activos.replicador_documental.domain.model.*;
import co.com.activos.replicador_documental.helpers.UseCase;
import co.com.activos.replicador_documental.infrastructure.adapters.BigQueryAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.DocumentRegistrationClient;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.BatchSoapClientAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientManualImpl;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
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
    private final BigQueryAdapter bigQueryAdapter;

    private static final int BATCH_SIZE = 100; // Batch más pequeño para mejor control
    private static final int PARALLEL_THREADS = 15; // Más threads para mejor paralelismo

    @Override
    public String ejecutar(Long txpCodigo) {
        long executionId = System.currentTimeMillis();
        String txpCodigoStr = txpCodigo.toString();
        
        // Contadores para tracking
        AtomicLong totalCarpetas = new AtomicLong(0);
        AtomicLong totalDocumentos = new AtomicLong(0);
        AtomicLong documentosMigrados = new AtomicLong(0);
        AtomicLong documentosFallidos = new AtomicLong(0);
        
        log.info("Iniciando migración txpCodigo: {}", txpCodigo);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas más pequeñas para mejor distribución
            int pageSize = 50; // Página más pequeña para mejor paralelismo
            int pageNumber = 0;
            
            while (true) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujo(txpCodigo, pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        log.info("No hay más parámetros. Fin de la migración.");
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página antes de continuar
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(20); // Más corta para mayor velocidad
                    
                } catch (Exception e) {
                    log.error("Error procesando página {} de parámetros: {}", pageNumber, e.getMessage());
                    pageNumber++;
                    continue;
                }
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
        
        log.info("Migración completada - Carpetas: {}, Migrados: {}, Fallidos: {}, Tiempo: {}s", 
                totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get(), tiempoTotal);
        
        return String.format("Migración completada. Carpetas: %d, Migrados: %d, Fallidos: %d", 
                totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get());
    }
    
    // Nuevo método para migración por año
    public String ejecutarPorAnio(Long txpCodigo, int anio) {
        long executionId = System.currentTimeMillis();
        String txpCodigoStr = txpCodigo.toString();
        
        // Contadores para tracking
        AtomicLong totalCarpetas = new AtomicLong(0);
        AtomicLong totalDocumentos = new AtomicLong(0);
        AtomicLong documentosMigrados = new AtomicLong(0);
        AtomicLong documentosFallidos = new AtomicLong(0);
        
        log.info("Iniciando migración año {} txpCodigo: {}", anio, txpCodigo);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas más pequeñas para mejor distribución
            int pageSize = 50; // Página más pequeña para mejor paralelismo
            int pageNumber = 0;
            
            while (true) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujoYAnio(txpCodigo, anio, pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        log.info("No hay más parámetros para el año {}. Fin de la migración.", anio);
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página antes de continuar
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(20); // Más corta para mayor velocidad
                    
                } catch (Exception e) {
                    log.error("Error procesando página {} de parámetros del año {}: {}", pageNumber, anio, e.getMessage());
                    pageNumber++;
                    continue;
                }
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
        
        log.info("Migración año {} completada - Carpetas: {}, Migrados: {}, Fallidos: {}, Tiempo: {}s", 
                anio, totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get(), tiempoTotal);
        
        return String.format("Migración año %d completada. Carpetas: %d, Migrados: %d, Fallidos: %d", 
                anio, totalCarpetas.get(), documentosMigrados.get(), documentosFallidos.get());
    }
    
    private void procesarCarpeta(TaxonomiaParam param, String txpCodigoStr, long executionId,
                                AtomicLong totalDocumentos, AtomicLong documentosMigrados, AtomicLong documentosFallidos) {
        
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
        
        // log.info("Procesando {} documentos carpeta {}", codigosCliente.size(), param.getCodigo()); // Comentado para velocidad
        
        // Mejorar paralelismo: procesar en batches más pequeños con threads controlados
        int batchSize = 10; // Batch más pequeño para mejor control
        int maxThreads = 5;  // Threads para esta carpeta específica
        
        ExecutorService carpetaExecutor = Executors.newFixedThreadPool(maxThreads);
        
        try {
            // Dividir en batches para procesamiento paralelo
            for (int i = 0; i < codigosCliente.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, codigosCliente.size());
                List<String> batch = codigosCliente.subList(i, endIndex);
                
                // log.debug("Procesando batch {}-{} de {} documentos (carpeta: {})", 
                        // i + 1, endIndex, codigosCliente.size(), param.getCodigo()); // Comentado para velocidad
                
                // Procesar este batch en paralelo
                List<CompletableFuture<Void>> futures = batch.stream()
                        .map(codigoCliente -> CompletableFuture.runAsync(() -> {
                            try {
                                // Buscar el AzDigital correspondiente (optimizado con Map)
                                AzDigital azDigital = todosLosAzDigitales.stream()
                                        .filter(az -> az.getCodigoCli().equals(codigoCliente))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (azDigital == null) {
                                    // log.warn("No se encontró AzDigital para código: {}", codigoCliente); // Comentado para velocidad
                                    return;
                                }
                                
                                // log.debug("Descargando documento {} con SOAP manual", codigoCliente); // Comentado para velocidad
                                
                                // Usar SOAP manual para descargar el archivo
                                SolicitarArchivoResponse soapResponse = soapClientManual.solicitarArchivo(codigoCliente);
                                
                                if (soapResponse == null || soapResponse.getArchivo() == null) {
                                    // log.warn("Respuesta vacía para documento: {}", codigoCliente); // Comentado para velocidad
                                    return;
                                }
                                
                                // Procesar documento
                                DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, soapResponse, param);
                                documentRegistrationClient.registerDocument(request);
                                
                                // log.debug("Documento {} procesado exitosamente", codigoCliente); // Comentado para velocidad
                                
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
                                log.error("Error procesando documento {}: {}", codigoCliente, e.getMessage(), e);
                                
                                // Para el log de error, necesitamos el AzDigital
                                AzDigital azDigital = todosLosAzDigitales.stream()
                                        .filter(az -> az.getCodigoCli().equals(codigoCliente))
                                        .findFirst()
                                        .orElse(null);
                                
                                if (azDigital != null) {
                                    DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
                                    
                                    // Log de error - COMENTADO para no insertar en BigQuery
                                    /*
                                    bigQueryAdapter.logError(azDigital, e.getMessage(), txpCodigoStr, executionId, 
                                            docParams.getTipoDocTrabajador(), 
                                            docParams.getDocumentoTrabajador());
                                    */
                                }
                                
                                documentosFallidos.incrementAndGet();
                            }
                        }, carpetaExecutor))
                        .toList();
                
                // Esperar a que termine el batch antes de continuar
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // Pequeña pausa entre batches para no sobrecargar
                Thread.sleep(10); // Más corta para mayor velocidad
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // log.error("Procesamiento interrumpido para carpeta: {}", param.getCodigo()); // Comentado para velocidad
        } finally {
            carpetaExecutor.shutdown();
            try {
                if (!carpetaExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    carpetaExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                carpetaExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // log.info("Carpeta {} completada - Migrados: {}, Fallidos: {}", 
        //         param.getCodigo(), documentosMigrados.get(), documentosFallidos.get()); // Comentado para velocidad
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
