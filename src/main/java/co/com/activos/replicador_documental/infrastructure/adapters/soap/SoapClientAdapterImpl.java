package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;


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

    private final WebServiceTemplate webServiceTemplate;

    @PostConstruct
    public void init() {
        // Usa el template configurado por Spring
        setWebServiceTemplate(webServiceTemplate);
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request) {
        String endpoint = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(defaultUri)
                .path(solicitarArchivoUri)
                .toUriString();

        var cb = new SoapActionCallback(solicitarArchivoAction);
        return (SolicitarArchivoResponse) getWebServiceTemplate()
                .marshalSendAndReceive(endpoint, request, cb);
    }

}

