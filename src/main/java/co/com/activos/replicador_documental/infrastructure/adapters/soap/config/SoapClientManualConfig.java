package co.com.activos.replicador_documental.infrastructure.adapters.soap.config;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientAdapterImpl;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientManualImpl;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class SoapClientManualConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public SoapClientManualImpl soapClientManualImpl(RestTemplate restTemplate) {
        return new SoapClientManualImpl(restTemplate);
    }

    @Bean
    public SoapClientAdapterImpl soapClientAdapterImpl(Jaxb2Marshaller marshaller) {
        SoapClientAdapterImpl client = new SoapClientAdapterImpl(marshaller);
        client.setDefaultUri("https://activos.analitica.com.co/AZDigital/WebServices/SOAP/");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
