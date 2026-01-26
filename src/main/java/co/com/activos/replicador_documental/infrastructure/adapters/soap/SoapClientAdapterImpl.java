package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import java.io.IOException;
import javax.xml.transform.TransformerException;

@Component
@Slf4j
public class SoapClientAdapterImpl extends WebServiceGatewaySupport  implements SoapClientAdapter{

    @Value("${soap.client.default-uri}")
    private String endpoint;

    // La acción definida en el WSDL para SolicitarArchivo
    private static final String SOAP_ACTION = "urn:/#SolicitarArchivo";

    private final Jaxb2Marshaller jaxb2Marshaller;

    public SoapClientAdapterImpl(Jaxb2Marshaller marshaller) {
        this.jaxb2Marshaller = marshaller;
    }

    @PostConstruct
    public void init() {
        // Configuramos el WebServiceTemplate interno de WebServiceGatewaySupport
        this.getWebServiceTemplate().setMarshaller(jaxb2Marshaller);
        this.getWebServiceTemplate().setUnmarshaller(jaxb2Marshaller);
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest solicitarArchivoRequest) {
        SolicitarArchivoRequest request = SolicitarArchivoRequest.builder()
                .id(solicitarArchivoRequest.getId())
                .build();

        log.info("Descargando documento ID: {} de AZDigital", solicitarArchivoRequest.getId());

        try {
            String fullEndpoint = endpoint + "SolicitarArchivo";
            return (SolicitarArchivoResponse) getWebServiceTemplate()
                    .marshalSendAndReceive(fullEndpoint, request, new SoapActionCallback(SOAP_ACTION) {
                        @Override
                        public void doWithMessage(WebServiceMessage message) throws IOException {
                            super.doWithMessage(message);
                            // Add custom namespaces if needed
                            if (message instanceof SoapMessage) {
                                SoapMessage soapMessage = (SoapMessage) message;
                                // Add az namespace to match WSDL expectations
                                soapMessage.getSoapHeader().addNamespaceDeclaration("az", "http://www.analitica.com.co/AZDigital/xsds/");
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("Error al obtener documento {}: {}", solicitarArchivoRequest.getId(), e.getMessage());
            throw e;
        }
    }
}

