package co.com.activos.replicador_documental.domain.model;

import java.util.List;

public interface ParamRepository {
    List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo);
}
