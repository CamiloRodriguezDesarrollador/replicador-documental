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

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/replicar")
@AllArgsConstructor
class ReplicadorController {

    private final ReplicarUseCase replicarUseCase;
    private final SoapClientAdapter soapClientAdapter;
    private final DocumentRegistrationClient documentRegistrationClient;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/bhv/{txpCodigo}")
    public ResponseEntity<String> replicarBhv(@PathVariable Long txpCodigo) {
        try {
            String resultado = replicarUseCase.ejecutar(txpCodigo);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al replicar carpetas: " + e.getMessage());
        }
    }


    @GetMapping("/bhv/{txpCodigo}/anio/{anio}")
    public CompletableFuture<ResponseEntity<String>> replicarBhvPorAnio(@PathVariable Long txpCodigo, @PathVariable int anio) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String resultado = replicarUseCase.ejecutarPorAnio(txpCodigo, anio);
                return ResponseEntity.ok(resultado);
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body("Error al replicar carpetas del año " + anio + ": " + e.getMessage());
            }
        });
    }


    @GetMapping("/solicitar/{id}")
    public ResponseEntity<SolicitarArchivoResponse> solicitarArchivo(@PathVariable String id) {
        try {
            SolicitarArchivoRequest request = new SolicitarArchivoRequest(id);
            SolicitarArchivoResponse response = soapClientAdapter.solicitarArchivo(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/crear", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentRegistrationResponse> crear(
            @RequestPart("documentId") String documentId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("params") String paramsJson
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ObjectMapper mapper = new ObjectMapper();
            DocumentRegistrationRequest.DocumentParams params =
                    mapper.readValue(paramsJson, DocumentRegistrationRequest.DocumentParams.class);

            DocumentRegistrationRequest request = new DocumentRegistrationRequest(documentId, file, params);
            DocumentRegistrationResponse response = documentRegistrationClient.registerDocument(request);

            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
