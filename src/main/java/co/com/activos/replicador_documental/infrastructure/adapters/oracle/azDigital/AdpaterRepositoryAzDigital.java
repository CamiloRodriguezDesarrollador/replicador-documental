package co.com.activos.replicador_documental.infrastructure.adapters.oracle.azDigital;

import co.com.activos.replicador_documental.domain.model.AzDigital;
import co.com.activos.replicador_documental.domain.model.AzDigitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdpaterRepositoryAzDigital implements AzDigitalRepository {

    private final co.com.activos.replicador_documental.infrastructure.adapters.oracle.azDigital.AzDigitalRepository repository;

    @Override
    public List<AzDigital> listarPorCarpeta(Long txpCodigo) {
        return repository.buscarPorCarpeta(txpCodigo)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AzDigital toDomain(AzDigitalData azDigitalData) {
        return AzDigital.builder()
                .codigo(azDigitalData.getCodigo())
                .codigoCli(azDigitalData.getCodigoCli())
                .nombre(azDigitalData.getNombreRuta())
                .prdCodigo(azDigitalData.getPrdCodigo())
                .build();
    }
}
