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
            
            log.info("Response SOAP recibida: {}", response);
            log.info("Longitud de respuesta: {} caracteres", response != null ? response.length() : 0);

            SolicitarArchivoResponse result = parseSoapResponse(response);
            log.info("Resultado del parsing - Archivo: {}", result.getArchivo() != null ? "OK" : "NULL");
            if (result.getArchivo() != null) {
                log.info("Nombre archivo: {}", result.getArchivo().getNombre());
                log.info("Contenido presente: {}", result.getArchivo().getContenido() != null ? "SI" : "NO");
            }
            
            return result;

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
        SolicitarArchivoResponse result = new SolicitarArchivoResponse();
        
        try {
            log.debug("Iniciando parsing de respuesta SOAP");
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Respuesta SOAP es nula o vacía");
                return result;
            }
            
            // Buscar diferentes posibles namespaces y elementos
            String[] possibleElements = {
                "<xsds:Archivo>", "<az:Archivo>", "<Archivo>",
                "<xsds:EntregarArchivo>", "<az:EntregarArchivo>", "<EntregarArchivo>"
            };
            
            String archivoXml = null;
            String elementFound = null;
            
            for (String element : possibleElements) {
                if (response.contains(element)) {
                    String closeElement = element.replace("<", "</");
                    int startIndex = response.indexOf(element);
                    int endIndex = response.indexOf(closeElement) + closeElement.length();
                    
                    if (endIndex > startIndex) {
                        archivoXml = response.substring(startIndex, endIndex);
                        elementFound = element;
                        log.info("Elemento encontrado: {}", element);
                        break;
                    }
                }
            }
            
            if (archivoXml != null) {
                log.debug("XML de archivo extraído: {}", archivoXml);
                
                // Extraer nombre y contenido con diferentes namespaces
                String nombre = extractXmlValue(archivoXml, "Nombre");
                String contenido = extractXmlValue(archivoXml, "Contenido");
                
                log.info("Nombre extraído: {}", nombre);
                log.info("Contenido extraído (longitud): {}", contenido != null ? contenido.length() : 0);
                
                SolicitarArchivoResponse.ArchivoData archivoData = new SolicitarArchivoResponse.ArchivoData();
                archivoData.setNombre(nombre);
                archivoData.setContenido(contenido);
                result.setArchivo(archivoData);
                
                log.info("Archivo parseado exitosamente");
            } else {
                log.warn("No se encontró ningún elemento de archivo en la respuesta");
                log.info("=== RESPUESTA SOAP COMPLETA ===");
                log.info(response);
                log.info("=== FIN RESPUESTA SOAP ===");
            }
        } catch (Exception e) {
            log.error("Error parseando respuesta SOAP: {}", e.getMessage(), e);
            log.debug("Respuesta que causó error: {}", response);
            throw new RuntimeException("Error parseando respuesta SOAP", e);
        }
        
        return result;
    }

    private String extractXmlValue(String xml, String tagName) {
        // Probar diferentes namespaces
        String[] possibleNamespaces = {"xsds:", "az:", ""};
        
        for (String namespace : possibleNamespaces) {
            String openTag = "<" + namespace + tagName + ">";
            String closeTag = "</" + namespace + tagName + ">";
            
            int startIndex = xml.indexOf(openTag);
            if (startIndex != -1) {
                startIndex += openTag.length();
                int endIndex = xml.indexOf(closeTag, startIndex);
                
                if (endIndex != -1) {
                    String value = xml.substring(startIndex, endIndex);
                    log.debug("Valor extraído con namespace '{}': {}", namespace, value);
                    return value;
                }
            }
        }
        
        log.debug("No se encontró el tag {} con ningún namespace", tagName);
        return null;
    }
}
