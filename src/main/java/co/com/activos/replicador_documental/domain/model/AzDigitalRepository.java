package co.com.activos.replicador_documental.domain.model;

import java.util.List;

public interface AzDigitalRepository {

    List<AzDigital> listarPorCarpeta(Long txpCodigo);

}
