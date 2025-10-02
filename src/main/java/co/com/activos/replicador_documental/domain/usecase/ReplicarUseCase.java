package co.com.activos.replicador_documental.domain.usecase;

import co.com.activos.replicador_documental.domain.model.AzDigital;
import co.com.activos.replicador_documental.domain.model.AzDigitalRepository;
import co.com.activos.replicador_documental.domain.model.ParamRepository;
import co.com.activos.replicador_documental.domain.model.TaxonomiaParam;
import co.com.activos.replicador_documental.helpers.UseCase;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.DocumentRegistrationClient;
import co.com.activos.replicador_documental.infrastructure.adapters.rest.model.DocumentRegistrationRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.SoapClientAdapter;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoRequest;
import co.com.activos.replicador_documental.infrastructure.adapters.soap.model.SolicitarArchivoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicarUseCase implements UseCase<Long, String> {

    private final ParamRepository paramRepository;
    private final AzDigitalRepository azDigitalRepository;
    private final SoapClientAdapter soapClientAdapter;
    private final DocumentRegistrationClient documentRegistrationClient;

    @Override
    public String ejecutar(Long txpCodigo) {
        paramRepository.listarPorTipoFlujo(txpCodigo)
                .forEach(param -> azDigitalRepository.listarPorCarpeta(param.getCodigo()) // bhv cc 1073704700
                        .forEach(azDigital -> {
                            try {
                                SolicitarArchivoResponse archivoResponse = soapClientAdapter.solicitarArchivo(
                                        new SolicitarArchivoRequest(azDigital.getCodigoCli())
                                );
                                DocumentRegistrationRequest request = crearDocumentRegistrationRequest(azDigital, archivoResponse, param);
                                documentRegistrationClient.registerDocument(request);
                            } catch (Exception e) {
                                log.error("Error al solicitar archivo para cliente {}: {}", azDigital.getCodigoCli(), e.getMessage());
                            }
                        })
                );
        return "Carpetas replicadas";
    }

    private DocumentRegistrationRequest crearDocumentRegistrationRequest(
            AzDigital azDigital, SolicitarArchivoResponse archivoResponse,
            TaxonomiaParam param) {

        return new DocumentRegistrationRequest(
                azDigital.getPrdCodigo(),
                base64ToMultipart(archivoResponse.getArchivo(), archivoResponse.getNombre(), archivoResponse.getTipoMime()),
                extraerTipoYNumeroDocumento(param.getNombre()));
    }

    private MultipartFile base64ToMultipart(String base64, String fileName, String contentType) {
        byte[] fileBytes = Base64.getDecoder().decode(base64);
        return new MockMultipartFile(fileName, fileName, contentType, fileBytes);
    }


    private DocumentRegistrationRequest.DocumentParams extraerTipoYNumeroDocumento(String nombre) {
        String[] partes = nombre.split(" ");
        String tipoDoc = partes.length > 1 ? partes[1] : "";
        String numeroDoc = partes.length > 2 ? partes[2] : "";
        return new DocumentRegistrationRequest.DocumentParams(tipoDoc, numeroDoc);
    }


}
