package co.com.activos.replicador_documental.infrastructure.adapters.soap.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

/**
 * Configuración del cliente SOAP para el servicio de solicitud de archivos
 */
@Configuration
public class SoapClientConfig {

    @Value("${soap.client.default-uri:http://192.168.21.4:7804/carpeta}")
    private String defaultUri;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller m = new Jaxb2Marshaller();
        // ❌ NO usar contextPath sin ObjectFactory/jaxb.index
        // m.setContextPath("co.com.activos.replicador_documental.infrastructure.adapters.soap.model");

        // ✅ Vincular por clases anotadas con @XmlRootElement (Jakarta)
        m.setClassesToBeBound(
                co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest.class,
                co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse.class
        );
        return m;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate wst = new WebServiceTemplate();
        wst.setMarshaller(marshaller);
        wst.setUnmarshaller(marshaller);
        wst.setDefaultUri(defaultUri);
        wst.setMessageSender(httpComponentsMessageSender());
        return wst;
    }

    @Bean
    public HttpComponents5MessageSender httpComponentsMessageSender() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        HttpComponents5MessageSender sender = new HttpComponents5MessageSender();
        sender.setHttpClient(httpClient);
        return sender;
    }
}


