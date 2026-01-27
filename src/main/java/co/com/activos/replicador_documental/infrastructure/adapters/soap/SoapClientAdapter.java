package co.com.activos.replicador_documental.infrastructure.adapters.soap;

import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;

/**
 * Interfaz para el adaptador de cliente SOAP
 */
public interface SoapClientAdapter {
    /**
     * Solicita un archivo mediante el servicio SOAP
     * @param request Datos de la solicitud
     * @return Respuesta del servicio
     */
    SolicitarArchivoResponse solicitarArchivo(SolicitarArchivoRequest request);
    
    /**
     * Solicita un archivo mediante el servicio SOAP (consumo manual)
     * @param docId ID del documento a solicitar
     * @return Respuesta del servicio
     */
    SolicitarArchivoResponse solicitarArchivo(String docId);
}
