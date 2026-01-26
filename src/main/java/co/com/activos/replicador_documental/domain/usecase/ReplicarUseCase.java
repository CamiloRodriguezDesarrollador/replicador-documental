package co.com.activos.replicador_documental.domain.usecase;

import co.com.activos.replicador_documental.domain.model.*;
import co.com.activos.replicador_documental.helpers.UseCase;
import co.com.activos.replicador_documental.infrastructure.adapters.BigQueryAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.DocumentRegistrationClient;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.BatchSoapClientAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientAdapter;
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
    private final SoapClientAdapter soapClientAdapter;
    private final BatchSoapClientAdapter batchSoapClientAdapter;
    private final DocumentRegistrationClient documentRegistrationClient;
    private final BigQueryAdapter bigQueryAdapter;

    private static final int BATCH_SIZE = 500; // Reducido para mejor rendimiento
    private static final int PARALLEL_THREADS = 10; // Procesamiento paralelo

    @Override
    public String ejecutar(Long txpCodigo) {
        long executionId = System.currentTimeMillis();
        String txpCodigoStr = txpCodigo.toString();
        
        // Contadores para tracking
        AtomicLong totalCarpetas = new AtomicLong(0);
        AtomicLong totalDocumentos = new AtomicLong(0);
        AtomicLong documentosMigrados = new AtomicLong(0);
        AtomicLong documentosFallidos = new AtomicLong(0);
        
        log.info("Iniciando migración paginada con paralelismo para txpCodigo: {}", txpCodigo);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas pero con paralelismo dentro de cada página
            int pageSize = 100; // Parámetros por página
            int pageNumber = 0;
            
            while (true) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujo(txpCodigo, pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        log.info("No hay más parámetros. Fin de la migración.");
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    log.info("Procesando página {} de parámetros ({} carpetas) en paralelo", pageNumber + 1, parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error procesando carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página antes de continuar
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(100);
                    
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
        log.info("Migración completada - Carpetas: {}, Documentos totales: {}, Migrados: {}, Fallidos: {}", 
                totalCarpetas.get(), totalDocumentos.get(), documentosMigrados.get(), documentosFallidos.get());
        
        return String.format("Migración completada. Carpetas procesadas: %d, Documentos migrados: %d, Documentos fallidos: %d", 
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
        
        log.info("Iniciando migración por año {} para txpCodigo: {}", anio, txpCodigo);
        
        // Executor para procesamiento paralelo controlado
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        
        try {
            // Procesar parámetros por páginas pero con paralelismo dentro de cada página
            int pageSize = 100; // Parámetros por página
            int pageNumber = 0;
            
            while (true) {
                try {
                    List<TaxonomiaParam> parametros = paramRepository.listarPorTipoFlujoYAnio(txpCodigo, anio, pageNumber, pageSize);
                    
                    if (parametros.isEmpty()) {
                        log.info("No hay más parámetros para el año {}. Fin de la migración.", anio);
                        break;
                    }
                    
                    totalCarpetas.addAndGet(parametros.size());
                    log.info("Procesando página {} de parámetros del año {} ({} carpetas) en paralelo", 
                            pageNumber + 1, anio, parametros.size());
                    
                    // Procesar esta página de parámetros en paralelo
                    List<CompletableFuture<Void>> futures = parametros.stream()
                            .map(param -> CompletableFuture.runAsync(() -> {
                                try {
                                    procesarCarpeta(param, txpCodigoStr, executionId, 
                                            totalDocumentos, documentosMigrados, documentosFallidos);
                                } catch (Exception e) {
                                    log.error("Error procesando carpeta {}: {}", param.getCodigo(), e.getMessage());
                                }
                            }, executor))
                            .toList();
                    
                    // Esperar a que termine toda la página antes de continuar
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    
                    pageNumber++;
                    
                    // Pequeña pausa entre páginas para liberar conexiones
                    Thread.sleep(100);
                    
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
        log.info("Migración del año {} completada - Carpetas: {}, Documentos totales: {}, Migrados: {}, Fallidos: {}", 
                anio, totalCarpetas.get(), totalDocumentos.get(), documentosMigrados.get(), documentosFallidos.get());
        
        return String.format("Migración del año %d completada. Carpetas procesadas: %d, Documentos migrados: %d, Documentos fallidos: %d", 
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
        
        log.info("Procesando {} documentos de la carpeta {} en modo batch", codigosCliente.size(), param.getCodigo());
        
        // Procesar en batches optimizados con SOAP
        batchSoapClientAdapter.procesarDocumentosEnBatches(
            codigosCliente,
            (codigoCliente, soapResponse) -> {
                try {
                    // Buscar el AzDigital correspondiente
                    AzDigital azDigital = todosLosAzDigitales.stream()
                            .filter(az -> az.getCodigoCli().equals(codigoCliente))
                            .findFirst()
                            .orElseThrow();
                    
                    // Procesar documento
                    DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, soapResponse, param);
                    documentRegistrationClient.registerDocument(request);
                    
                    // Extraer tipo y número de documento para el log
                    DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
                    
                    // Log de éxito usando el adaptador con datos correctos
                    bigQueryAdapter.logSuccess(azDigital, 
                            Collections.singletonList(azDigital.getCodigoCli()), 
                            txpCodigoStr, executionId, 
                            docParams.getTipoDocTrabajador(), // Tipo de documento real
                            docParams.getDocumentoTrabajador()); // Cédula real
                    
                    documentosMigrados.incrementAndGet();
                    
                } catch (Exception e) {
                    log.error("Error procesando documento {}: {}", codigoCliente, e.getMessage());
                    
                    // Para el log de error, necesitamos el AzDigital
                    AzDigital azDigital = todosLosAzDigitales.stream()
                            .filter(az -> az.getCodigoCli().equals(codigoCliente))
                            .findFirst()
                            .orElse(null);
                    
                    if (azDigital != null) {
                        DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
                        
                        bigQueryAdapter.logError(azDigital, e.getMessage(), txpCodigoStr, executionId, 
                                docParams.getTipoDocTrabajador(), // Tipo de documento real
                                docParams.getDocumentoTrabajador()); // Cédula real
                    }
                    
                    documentosFallidos.incrementAndGet();
                }
            },
            25 // Batch size reducido para menor carga
        );
    }
    
    private void procesarDocumento(AzDigital azDigital, TaxonomiaParam param, String txpCodigoStr, long executionId,
                                 AtomicLong documentosMigrados, AtomicLong documentosFallidos) {
        try {
            SolicitarArchivoResponse archivoResponse = soapClientAdapter.solicitarArchivo(
                    new SolicitarArchivoRequest(azDigital.getCodigoCli())
            );
            DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, archivoResponse, param);
            documentRegistrationClient.registerDocument(request);
            
            // Extraer tipo y número de documento para el log
            DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
            
            // Log de éxito usando el adaptador con datos correctos
            bigQueryAdapter.logSuccess(azDigital, 
                    Collections.singletonList(azDigital.getCodigoCli()), 
                    txpCodigoStr, executionId, 
                    docParams.getTipoDocTrabajador(), // Tipo de documento real
                    docParams.getDocumentoTrabajador()); // Cédula real
            
            documentosMigrados.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error al solicitar archivo para cliente {}: {}", azDigital.getCodigoCli(), e.getMessage());
            
            // Extraer tipo y número de documento para el log de error también
            DocumentRegistrationRequest.DocumentParams docParams = extraerTipoYNumeroDocumento(param.getNombre());
            
            // Log de error usando el adaptador con datos correctos
            bigQueryAdapter.logError(azDigital, e.getMessage(), txpCodigoStr, executionId, 
                    docParams.getTipoDocTrabajador(), // Tipo de documento real
                    docParams.getDocumentoTrabajador()); // Cédula real
            
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
