package co.com.activos.replicador_documental.domain.model;

import java.util.List;

public interface ParamRepository {
    List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo);
    List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo, int pageNumber, int pageSize);
    List<TaxonomiaParam> listarPorTipoFlujoYAnio(Long txpCodigo, int anio);
    List<TaxonomiaParam> listarPorTipoFlujoYAnio(Long txpCodigo, int anio, int pageNumber, int pageSize);
    
    // Métodos de conteo para validación
    Long contarCarpetasPorTipoFlujo(Long txpCodigo);
    Long contarCarpetasPorTipoFlujoYAnio(Long txpCodigo, int anio);
}
