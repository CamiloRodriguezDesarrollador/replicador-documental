package co.com.activos.replicador_documental.infrastructure.adapters.oracle.taxonomiaParam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@Table(name = "taxonomia_param", schema = "ADM")
public class TaxonomiaParamData {

    @Id
    @Column(name = "TXP_CODIGO")
    private Long codigo;

    @Column(name = "TXP_CODIGO_REF")
    private Long codigoRef;

    @Column(name = "TXP_DESCRIPCION")
    private String nombre;

}
