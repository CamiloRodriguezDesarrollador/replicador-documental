package co.com.activos.replicador_documental.infrastructure.adapters.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request model for document registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRegistrationRequest {
    private String documentId;
    private MultipartFile file;
    private DocumentParams params;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentParams {
        @JsonProperty("tipo_doc_trabajador")
        private String tipoDocTrabajador;
        @JsonProperty("documento_trabajador")
        private String documentoTrabajador;
    }
}
