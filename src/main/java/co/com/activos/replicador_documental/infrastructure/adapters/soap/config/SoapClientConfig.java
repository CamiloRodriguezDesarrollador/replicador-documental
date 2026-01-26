package co.com.activos.replicador_documental.infrastructure.adapters.soap.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración optimizada del cliente SOAP para alta concurrencia y timeouts
 */
@Configuration
public class SoapClientConfig {

    @Value("${soap.client.default-uri:http://192.168.21.4:7804/carpeta}")
    private String defaultUri;
    
    @Value("${soap.client.timeout.connection:30000}")
    private int connectionTimeout;
    
    @Value("${soap.client.timeout.socket:60000}")
    private int socketTimeout;
    
    @Value("${soap.client.pool.max-total:200}")
    private int maxTotal;
    
    @Value("${soap.client.pool.max-per-route:50}")
    private int maxPerRoute;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // Asegúrate de que este paquete solo tenga las clases de AZDigital
        marshaller.setPackagesToScan("co.com.activos.replicador_documental.infrastructure.adapters.soap.model");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller, HttpComponents5MessageSender messageSender) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(messageSender);
        return template;
    }

    @Bean
    public HttpComponents5MessageSender httpComponentsMessageSender() {
        // Pool de conexiones optimizado para alta concurrencia
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        
        // Configuración de timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(socketTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponents5MessageSender sender = new HttpComponents5MessageSender();
        
        // Configurar timeouts ANTES de setHttpClient
        sender.setConnectionTimeout(Duration.ofMillis(connectionTimeout));
        sender.setReadTimeout(Duration.ofMillis(socketTimeout));
        
        // Luego configurar el HttpClient
        sender.setHttpClient(httpClient);
        
        return sender;
    }
}


