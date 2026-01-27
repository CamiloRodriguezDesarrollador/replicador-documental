package co.com.activos.replicador_documental.infrastructure.adapters.oracle.azDigital;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AzDigitalRepository extends JpaRepository<AzDigitalData,Long> {

    @Query(value = "SELECT AZD.AZD_CODIGO, AZD.AZD_CODIGO_CLI, AZD.AZD_NOMBRE_RUTA , DEA.PRD_CODIGO " +
            "FROM adm.DATA_ERP_AZ DEA " +
            "JOIN adm.AZ_DIGITAL AZD ON AZD.AZD_CODIGO = DEA.AZD_CODIGO " +
            "WHERE DEA.TXP_CODIGO = :txpCodigo AND DEA.DEA_ESTADO IN (1,3) AND DEA.PRD_CODIGO IS NOT NULL",
            countQuery = "SELECT count(*) FROM adm.DATA_ERP_AZ DEA WHERE DEA.TXP_CODIGO = :txpCodigo AND DEA.DEA_ESTADO IN (1,3) AND DEA.PRD_CODIGO IS NOT NULL",
            nativeQuery = true)
    Page<AzDigitalData> buscarPorCarpeta(Long txpCodigo, Pageable pageable);

}
