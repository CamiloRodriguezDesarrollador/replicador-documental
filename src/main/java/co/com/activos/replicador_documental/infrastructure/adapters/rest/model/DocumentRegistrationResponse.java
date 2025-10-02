package co.com.activos.replicador_documental.infrastructure.adapters.rest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for document registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRegistrationResponse {
    private boolean success;
    private String message;
    private String documentId;
    // Add additional fields as per the actual API response
}
