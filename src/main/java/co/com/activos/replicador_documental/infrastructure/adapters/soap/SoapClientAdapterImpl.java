package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import java.time.Duration;
import java.time.Instant;


@Slf4j
@Component
@RequiredArgsConstructor
public class SoapClientAdapterImpl extends WebServiceGatewaySupport implements SoapClientAdapter {

    @Value("${soap.client.uri.solicitar-archivo:/solicitar_archivo}")
    private String solicitarArchivoUri;

    @Value("${soap.client.action.solicitar-archivo:http://tempuri.org/solicitar_archivo_ms/ReqSolicitarArchivo}")
    private String solicitarArchivoAction;

    @Value("${soap.client.default-uri:http://192.168.21.4:7804/carpeta}")
    private String defaultUri;
    
    @Value("${soap.client.retry.max-attempts:3}")
    private int maxRetryAttempts;

    private final WebServiceTemplate webServiceTemplate;

    @PostConstruct
    public void init() {
        setWebServiceTemplate(webServiceTemplate);
    }

    @Override
    @Retryable(
        value = {WebServiceClientException.class},
        maxAttemptsExpression = "#{@soapClientAdapterImpl.maxRetryAttempts}",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request) {
        Instant start = Instant.now();
        String endpoint = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(defaultUri)
                .path(solicitarArchivoUri)
                .toUriString();

        try {
            log.debug("Solicitando archivo para cliente: {}", request.getId());
            
            var cb = new SoapActionCallback(solicitarArchivoAction);
            SolicitarArchivoResponse response = (SolicitarArchivoResponse) getWebServiceTemplate()
                    .marshalSendAndReceive(endpoint, request, cb);
            
            Duration duration = Duration.between(start, Instant.now());
            log.info("SOAP request completada para cliente {} en {} ms", 
                    request.getId(), duration.toMillis());
            
            return response;
            
        } catch (WebServiceClientException e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Error en SOAP request para cliente {} después de {} ms: {}", 
                    request.getId(), duration.toMillis(), e.getMessage());
            throw e;
        }
    }
}

