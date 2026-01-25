package co.com.activos.replicador_documental.infrastructure.adapters.oracle.taxonomiaParam;

import co.com.activos.replicador_documental.domain.model.ParamRepository;
import co.com.activos.replicador_documental.domain.model.TaxonomiaParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdapterTaxonomiaRepository implements ParamRepository {

    private final TaxonomiaParamRepository repository;

    @Override
    public List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo) {
        // Cargar todo (método original)
        return repository.buscarPorTipoFlujoOptimizado(txpCodigo)
                .stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo, int pageNumber, int pageSize) {
        // Método paginado para evitar connection leaks
        return repository.buscarPorTipoFlujoPaginado(txpCodigo, pageNumber * pageSize, pageSize)
                .stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<TaxonomiaParam> listarPorTipoFlujoYAnio(Long txpCodigo, int anio) {
        // Migración por año - método optimizado
        return repository.buscarPorTipoFlujoYAnio(txpCodigo, anio)
                .stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<TaxonomiaParam> listarPorTipoFlujoYAnio(Long txpCodigo, int anio, int pageNumber, int pageSize) {
        // Migración por año paginado
        return repository.buscarPorTipoFlujoYAnioPaginado(txpCodigo, anio, pageNumber * pageSize, pageSize)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TaxonomiaParam toDomain(TaxonomiaParamData data) {
        return TaxonomiaParam.builder()
                .codigo(data.getCodigo())
                .codigoRef(data.getCodigoRef())
                .nombre(data.getNombre())
                .build();
    }
}
