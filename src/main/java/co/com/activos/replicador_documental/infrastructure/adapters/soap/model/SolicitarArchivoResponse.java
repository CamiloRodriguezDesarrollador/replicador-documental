package co.com.activos.replicador_documental.infrastructure.adapters.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "EntregarArchivo", namespace = "http://www.analitica.com.co/AZDigital/xsds/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SolicitarArchivoResponse {
    @XmlElement(name = "Archivo", namespace = "http://www.analitica.com.co/AZDigital/xsds/")
    private ArchivoData archivo;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ArchivoData { // Sin @XmlRootElement aquí
        @XmlElement(name = "Nombre")
        private String nombre;
        @XmlElement(name = "Contenido")
        private String contenido;
    }
}
