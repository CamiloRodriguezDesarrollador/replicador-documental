package co.com.activos.replicador_documental.infrastructure.entryPoints.api;


import co.com.activos.replicador_documental.domain.usecase.ReplicarUseCase;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.DocumentRegistrationClient;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationResponse;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/replicar")
@AllArgsConstructor
public class replicador {

    private final ReplicarUseCase replicarUseCase;
    private final SoapClientAdapter soapClientAdapter;
    private final DocumentRegistrationClient documentRegistrationClient;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/bhv/{txpCodigo}")
    public String replicarBhv(@PathVariable Long txpCodigo) {
        return replicarUseCase.ejecutar(txpCodigo);
    }


    @GetMapping("/solicitar/{id}")
    public SolicitarArchivoResponse test(@PathVariable String id) {
        var req = new SolicitarArchivoRequest(id);
        return soapClientAdapter.solicitarArchivo(req);
    }


    @PostMapping(value="/crear", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentRegistrationResponse> crear(
            @RequestPart("documentId") String documentId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("params") String paramsJson
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        DocumentRegistrationRequest.DocumentParams params = mapper.readValue(paramsJson, DocumentRegistrationRequest.DocumentParams.class);
        var response = documentRegistrationClient.registerDocument(new DocumentRegistrationRequest(documentId, file, params));
        return ResponseEntity.ok(response);
    }
}
