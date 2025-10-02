package co.com.activos.replicador_documental.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TaxonomiaParam {
    private Long codigo;
    private Long codigoRef;
    private String nombre;
    private Integer tipoFlujo;
}
