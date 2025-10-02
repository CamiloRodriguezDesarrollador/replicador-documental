package co.com.activos.replicador_documental.infrastructure.adapters.rest;

import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationResponse;

/**
 * Interface for document registration REST client
 */
public interface DocumentRegistrationClient {
    
    /**
     * Registers a document by uploading it to the document management service
     * 
     * @param request The document registration request containing file and metadata
     * @return The registration response with status and document information
     */
    DocumentRegistrationResponse registerDocument(DocumentRegistrationRequest request);
}
