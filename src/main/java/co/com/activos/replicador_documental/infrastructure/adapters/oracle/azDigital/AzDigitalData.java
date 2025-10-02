package co.com.activos.replicador_documental.infrastructure.adapters.oracle.azDigital;


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
@Table(name = "AZ_DIGITAL", schema = "ADM")
public class AzDigitalData {
    @Id
    @Column(name = "AZD_CODIGO")
    private Long codigo;

    @Column(name = "AZD_CODIGO_CLI")
    private String codigoCli;

    @Column(name = "AZD_NOMBRE_RUTA")
    private String nombreRuta;

    @Column(name = "PRD_CODIGO")
    private String prdCodigo;


}
