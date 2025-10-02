package co.com.activos.replicador_documental.infrastructure.adapters.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de respuesta del servicio de solicitud de archivo
 */

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ResSolicitarArchivo", namespace = "http://tempuri.org/solicitar_archivo_ms")
public class SolicitarArchivoResponse {

    @XmlElement(name = "id", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String id;

    @XmlElement(name = "nombre", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String nombre;

    @XmlElement(name = "tipoMime", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String tipoMime;

    @XmlElement(name = "codificacion", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String codificacion;

    // viene como texto Base64; lo dejamos String y luego decodificamos
    @XmlElement(name = "archivo", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String archivo;
}
