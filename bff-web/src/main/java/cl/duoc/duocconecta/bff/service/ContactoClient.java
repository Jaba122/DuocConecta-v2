package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.ColaboracionResponse;
import cl.duoc.duocconecta.bff.dto.DatosDeContacto;
import cl.duoc.duocconecta.bff.dto.SolicitudColaboracionRequest;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Cliente hacia ms-contacto. Propaga el token original, igual que {@link UsuariosClient}. */
@Service
public class ContactoClient {

    private static final String RUTA = "/api/v1/colaboraciones";

    private static final ParameterizedTypeReference<List<ColaboracionResponse>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public ContactoClient(RestClient clienteMsContacto, UsuarioActual usuarioActual) {
        this.cliente = clienteMsContacto;
        this.usuarioActual = usuarioActual;
    }

    /** Envía una solicitud de colaboración a otra persona. */
    public ColaboracionResponse crear(SolicitudColaboracionRequest solicitud) {
        return cliente.post()
                .uri(RUTA)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(ColaboracionResponse.class);
    }

    /** En un rechazo, {@code contacto} va vacío y ms-contacto no guarda nada. */
    public ColaboracionResponse responder(UUID id, boolean aceptar, DatosDeContacto contacto) {
        return cliente.patch()
                .uri(RUTA + "/{id}/responder", id)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "aceptar", aceptar,
                        "correo", texto(contacto.correo()),
                        "telefono", texto(contacto.telefono()),
                        "redes", texto(contacto.redes())))
                .retrieve()
                .body(ColaboracionResponse.class);
    }

    /** Map.of no acepta nulos: un dato ausente viaja como cadena vacía. */
    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** Solicitudes que le llegaron al usuario autenticado. */
    public List<ColaboracionResponse> recibidas() {
        return listar("/recibidas");
    }

    /** Solicitudes que el usuario autenticado envió. */
    public List<ColaboracionResponse> enviadas() {
        return listar("/enviadas");
    }

    private List<ColaboracionResponse> listar(String sufijo) {
        List<ColaboracionResponse> respuesta = cliente.get()
                .uri(RUTA + sufijo)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .retrieve()
                .body(LISTA);
        return respuesta == null ? List.of() : respuesta;
    }

    /** La cabecera Authorization con el token original de la petición en curso. */
    private String cabeceraAuthorization() {
        return "Bearer " + usuarioActual.tokenActual().getTokenValue();
    }
}
