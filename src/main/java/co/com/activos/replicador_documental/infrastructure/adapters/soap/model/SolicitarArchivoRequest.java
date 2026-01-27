package co.com.activos.replicador_documental.infrastructure.adapters.soap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;


import jakarta.xml.bind.annotation.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BuscarArchivo", namespace = "http://www.analitica.com.co/AZDigital/xsds/")
public class SolicitarArchivoRequest {

    @XmlElement(name = "Id", namespace = "http://www.analitica.com.co/AZDigital/xsds/")
    private String id; // El ID del documento en AZDigital
}

