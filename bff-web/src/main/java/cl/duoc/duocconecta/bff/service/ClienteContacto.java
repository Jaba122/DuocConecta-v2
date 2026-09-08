package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.ColaboracionRespuesta;
import cl.duoc.duocconecta.bff.dto.DatosDeContacto;
import cl.duoc.duocconecta.bff.dto.SolicitudColaboracionDatos;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Cliente hacia ms-contacto. Propaga el token original, igual que {@link ClienteUsuarios}. */
@Service
public class ClienteContacto {

    private static final String RUTA = "/api/v1/colaboraciones";

    private static final ParameterizedTypeReference<List<ColaboracionRespuesta>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public ClienteContacto(RestClient clienteMsContacto, UsuarioActual usuarioActual) {
        this.cliente = clienteMsContacto;
        this.usuarioActual = usuarioActual;
    }

    /** Envía una solicitud de colaboración a otra persona. */
    public ColaboracionRespuesta crear(SolicitudColaboracionDatos solicitud) {
        return cliente.post()
                .uri(RUTA)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(ColaboracionRespuesta.class);
    }

    /** En un rechazo, {@code contacto} va vacío y ms-contacto no guarda nada. */
    public ColaboracionRespuesta responder(UUID id, boolean aceptar, DatosDeContacto contacto) {
        return cliente.patch()
                .uri(RUTA + "/{id}/responder", id)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "aceptar", aceptar,
                        "correo", texto(contacto.correo()),
                        "telefono", texto(contacto.telefono()),
                        "redes", texto(contacto.redes())))
                .retrieve()
                .body(ColaboracionRespuesta.class);
    }

    /** Map.of no acepta nulos: un dato ausente viaja como cadena vacía. */
    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** Solicitudes que le llegaron al usuario autenticado. */
    public List<ColaboracionRespuesta> recibidas() {
        return listar("/recibidas");
    }

    /** Solicitudes que el usuario autenticado envió. */
    public List<ColaboracionRespuesta> enviadas() {
        return listar("/enviadas");
    }

    private List<ColaboracionRespuesta> listar(String sufijo) {
        List<ColaboracionRespuesta> respuesta = cliente.get()
                .uri(RUTA + sufijo)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(LISTA);
        return respuesta == null ? List.of() : respuesta;
    }
}
