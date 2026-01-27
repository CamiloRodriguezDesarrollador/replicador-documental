package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class SoapClientManualImpl implements SoapClientAdapter {

    @Value("${soap.client.default-uri}")
    private String endpoint;

    private final RestTemplate restTemplate;

    public SoapClientManualImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request) {
        return solicitarArchivo(request.getId());
    }

    @Override
    public SolicitarArchivoResponse solicitarArchivo(String docId) {
        log.info("Descargando documento ID: {} de AZDigital (consumo manual)", docId);

        try {
            // Construir el XML SOAP manualmente
            String soapRequest = buildSoapRequest(docId);
            
            String fullEndpoint = endpoint + "ServiciosAZDigital.php";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "urn:/#SolicitarArchivo");
            
            HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);
            
            log.debug("Enviando request SOAP manual a: {}", fullEndpoint);
            log.debug("Request SOAP: {}", soapRequest);
            
            String response = restTemplate.postForObject(fullEndpoint, entity, String.class);
            
            log.debug("Response SOAP: {}", response);
            
            // Parsear la respuesta manualmente
            return parseSoapResponse(response);
            
        } catch (Exception e) {
            log.error("Error al obtener documento {}: {}", docId, e.getMessage());
            throw e;
        }
    }

    private String buildSoapRequest(String docId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                               "xmlns:xsds=\"http://www.analitica.com.co/AZDigital/xsds/\">\n" +
               "   <soapenv:Header/>\n" +
               "   <soapenv:Body>\n" +
               "      <xsds:SolicitarArchivo Id=\"" + docId + "\"/>\n" +
               "   </soapenv:Body>\n" +
               "</soapenv:Envelope>";
    }

    private SolicitarArchivoResponse parseSoapResponse(String response) {
        // Implementar parsing manual de la respuesta SOAP
        SolicitarArchivoResponse result = new SolicitarArchivoResponse();
        
        try {
            // Extraer el contenido del archivo de la respuesta SOAP
            if (response.contains("<xsds:Archivo>") && response.contains("</xsds:Archivo>")) {
                String archivoXml = response.substring(
                    response.indexOf("<xsds:Archivo>"),
                    response.indexOf("</xsds:Archivo>") + "</xsds:Archivo>".length()
                );
                
                // Extraer nombre y contenido
                String nombre = extractXmlValue(archivoXml, "Nombre");
                String contenido = extractXmlValue(archivoXml, "Contenido");
                
                SolicitarArchivoResponse.ArchivoData archivoData = new SolicitarArchivoResponse.ArchivoData();
                archivoData.setNombre(nombre);
                archivoData.setContenido(contenido);
                result.setArchivo(archivoData);
                
                log.info("Archivo obtenido exitosamente: {}", nombre);
            } else {
                log.warn("No se encontró el elemento <xsds:Archivo> en la respuesta");
            }
        } catch (Exception e) {
            log.error("Error parseando respuesta SOAP: {}", e.getMessage());
            throw new RuntimeException("Error parseando respuesta SOAP", e);
        }
        
        return result;
    }

    private String extractXmlValue(String xml, String tagName) {
        String openTag = "<xsds:" + tagName + ">";
        String closeTag = "</xsds:" + tagName + ">";
        
        int startIndex = xml.indexOf(openTag);
        if (startIndex == -1) return null;
        
        startIndex += openTag.length();
        int endIndex = xml.indexOf(closeTag, startIndex);
        
        return endIndex == -1 ? null : xml.substring(startIndex, endIndex);
    }
}
