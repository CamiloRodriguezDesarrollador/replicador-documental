package co.com.activos.replicador_documental.infrastructure.adapters.oracle.taxonomiaParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaxonomiaParamRepository extends JpaRepository<TaxonomiaParamData,Long> {
    
    // Método original (mantenido para compatibilidad)
    @Query(value = """
        SELECT  TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION FROM ADM.TAXONOMIA_PARAM tp WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        """, nativeQuery = true)
    List<TaxonomiaParamData> listarPorTipoFlujo(@Param("txpCodigoRef") Long txpCodigoRef);

    // Método con paginación (mantenido para compatibilidad)
    @Query(value = """
        SELECT  TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION FROM ADM.TAXONOMIA_PARAM tp WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        """, countQuery = """
        SELECT count(*) FROM ADM.TAXONOMIA_PARAM tp WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        """, nativeQuery = true)
    Page<TaxonomiaParamData> findByCodigoRef(@Param("txpCodigoRef") Long txpCodigo, Pageable pageable);
    
    // Método optimizado - consulta directa sin paginación para mejor rendimiento
    @Query(value = """
        SELECT TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION 
        FROM ADM.TAXONOMIA_PARAM tp 
        WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        ORDER BY TXP_CODIGO
        """, nativeQuery = true)
    List<TaxonomiaParamData> buscarPorTipoFlujoOptimizado(@Param("txpCodigoRef") Long txpCodigoRef);
    
    // Método paginado - para evitar connection leaks y memory issues
    @Query(value = """
        SELECT TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION 
        FROM ADM.TAXONOMIA_PARAM tp 
        WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        ORDER BY TXP_CODIGO
        OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY
        """, nativeQuery = true)
    List<TaxonomiaParamData> buscarPorTipoFlujoPaginado(
        @Param("txpCodigoRef") Long txpCodigoRef, 
        @Param("offset") int offset, 
        @Param("pageSize") int pageSize
    );
    
    // Métodos para migración por año - optimizados sin JOIN
    @Query(value = """
        SELECT TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION 
        FROM ADM.TAXONOMIA_PARAM tp 
        WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        ORDER BY TXP_CODIGO
        """, nativeQuery = true)
    List<TaxonomiaParamData> buscarPorTipoFlujoYAnio(
        @Param("txpCodigoRef") Long txpCodigoRef, 
        @Param("anio") int anio
    );
    
    @Query(value = """
        SELECT TXP_CODIGO, TXP_CODIGO_REF, TXP_DESCRIPCION 
        FROM ADM.TAXONOMIA_PARAM tp 
        WHERE tp.TXP_CODIGO_REF = :txpCodigoRef
        AND EXTRACT(YEAR FROM tp.AUD_FECHA) = :anio
        ORDER BY TXP_CODIGO
        OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY
        """, nativeQuery = true)
    List<TaxonomiaParamData> buscarPorTipoFlujoYAnioPaginado(
        @Param("txpCodigoRef") Long txpCodigoRef, 
        @Param("anio") int anio,
        @Param("offset") int offset, 
        @Param("pageSize") int pageSize
    );
}
