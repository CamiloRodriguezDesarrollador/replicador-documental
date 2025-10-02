package co.com.activos.replicador_documental.domain.usecase;


import co.com.activos.replicador_documental.helpers.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicarEmpresaUseCase implements UseCase<String, String> {

    @Override
    public String ejecutar(String azCodigoCli) {
        return "";
    }
}
