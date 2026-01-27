package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SoapClientFactory implements SoapClientAdapter {

    private final SoapClientAdapterImpl springClient;
    private final SoapClientManualImpl manualClient;

    @Value("${soap.client.implementation:manual}")
    private String implementation;

    public SoapClientFactory(SoapClientAdapterImpl springClient, SoapClientManualImpl manualClient) {
        this.springClient = springClient;
        this.manualClient = manualClient;
    }

    private SoapClientAdapter getClient() {
        return "spring".equals(implementation) ? springClient : manualClient;
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request) {
        log.info("Usando implementación SOAP: {}", implementation);
        return getClient().solicitarArchivo(request);
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(String docId) {
        log.info("Usando implementación SOAP: {}", implementation);
        return getClient().solicitarArchivo(docId);
    }
}
