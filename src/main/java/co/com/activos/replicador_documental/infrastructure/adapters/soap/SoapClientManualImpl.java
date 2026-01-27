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
        log.info("Iniciando descarga de documento ID: {} mediante IP Estática", docId);

        try {
            String soapRequest = buildSoapRequest(docId);

            // La URL debe ser exactamente:
            // https://activos.analitica.com.co/AZDigital_Pruebas/WebServices/SOAP/
            String fullEndpoint = endpoint;

            HttpHeaders headers = new HttpHeaders();

            // Ajuste 1: Especificar charset UTF-8 (Vital para evitar el 403 en algunos IIS/Apache)
            headers.set("Content-Type", "text/xml;charset=UTF-8");

            // Ajuste 2: SOAPAction. En Node funciona porque la librería lo extrae del WSDL.
            // Si el valor del namespace no funciona, prueba dejándolo como "" (comillas vacías).
            headers.set("SOAPAction", "http://www.analitica.com.co/AZDigital/xsds/SolicitarArchivo");

            // Ajuste 3: User-Agent idéntico a una herramienta de testing conocida
            headers.set("User-Agent", "PostmanRuntime/7.29.2");
            headers.set("Accept", "*/*");

            HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);

            log.debug("Enviando POST a Endpoint: {}", fullEndpoint);

            // Usamos postForEntity para capturar el código de estado en caso de error
            String response = restTemplate.postForObject(fullEndpoint, entity, String.class);

            return parseSoapResponse(response);

        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            log.error("ERROR 403: El servidor externo rechazó la conexión.");
            log.error("Cuerpo de la respuesta del servidor: {}", e.getResponseBodyAsString());
            log.error("Verifica que la IP Estática del Cloud NAT sea: [Tu IP Pública]");
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en comunicación SOAP: {}", e.getMessage());
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
