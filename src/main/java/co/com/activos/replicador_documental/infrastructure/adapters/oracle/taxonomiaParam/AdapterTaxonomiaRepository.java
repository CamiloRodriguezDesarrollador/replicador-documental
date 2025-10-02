package co.com.activos.replicador_documental.infrastructure.adapters.oracle.taxonomiaParam;

import co.com.activos.replicador_documental.domain.model.ParamRepository;
import co.com.activos.replicador_documental.domain.model.TaxonomiaParam;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdapterTaxonomiaRepository implements ParamRepository {

    private final TaxonomiaParamRepository repository;


    @Override
    public List<TaxonomiaParam> listarPorTipoFlujo(Long txpCodigo) {

//        int pageSize = 1000;
//        int pageNumber = 0;
//        boolean ultimaPagina = false;

        /*
        List<TaxonomiaParam> resultado = new ArrayList<>();

        while (!ultimaPagina) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<TaxonomiaParamData> page = repository.findByCodigoRef(txpCodigo, pageable);

            resultado.addAll(
                    page.getContent().stream()
                            .map(this::toDomain)
                            .toList()
            );

            ultimaPagina = page.isLast();
            pageNumber++;
        }

        return resultado;

         */

        return repository.listarPorTipoFlujo(txpCodigo)
                .stream()
                .map(this::toDomain)
                .toList();

    }

    public TaxonomiaParam toDomain(TaxonomiaParamData data) {
        return TaxonomiaParam.builder()
                .codigo(data.getCodigo())
                .codigoRef(data.getCodigoRef())
                .nombre(data.getNombre())
                .build();
    }
}
