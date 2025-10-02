package co.com.activos.replicador_documental.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class AzDigital {
    private Long codigo;
    private String codigoCli;
    private String nombre;
    private String prdCodigo;
}
