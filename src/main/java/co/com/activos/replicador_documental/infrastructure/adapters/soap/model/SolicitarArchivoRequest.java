package co.com.activos.replicador_documental.infrastructure.adapters.soap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ReqSolicitarArchivo", namespace = "http://tempuri.org/solicitar_archivo_ms")
public class SolicitarArchivoRequest {
    @XmlElement(name = "id", namespace = "http://tempuri.org/solicitar_archivo_ms")
    private String id;
}

