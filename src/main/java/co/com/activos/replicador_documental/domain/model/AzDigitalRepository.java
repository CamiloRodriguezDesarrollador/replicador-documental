package co.com.activos.replicador_documental.domain.model;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AzDigitalRepository {

    List<AzDigital> listarPorCarpeta(Long txpCodigo);
    
    List<AzDigital> listarPorCarpeta(Long txpCodigo, Pageable pageable);

}
