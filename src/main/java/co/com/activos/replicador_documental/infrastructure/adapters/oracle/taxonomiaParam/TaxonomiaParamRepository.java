package co.com.activos.replicador_documental.infrastructure.adapters.oracle.taxonomiaParam;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaxonomiaParamRepository extends JpaRepository<TaxonomiaParamData,Long> {
    @Query(value = """
        SELECT  TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION FROM ADM.TAXONOMIA_PARAM tp WHERE tp.TXP_DESCRIPCION = 'BHV CC 1073704700'
        """, nativeQuery = true)
    List<TaxonomiaParamData> listarPorTipoFlujo(@Param("txpCodigoRef") Long txpCodigoRef);


    //Page<TaxonomiaParamData>findByCodigoRef(Long txpCodigo, Pageable pageable);
}
