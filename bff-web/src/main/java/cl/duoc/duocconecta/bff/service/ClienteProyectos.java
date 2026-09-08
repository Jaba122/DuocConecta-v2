package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.ComentarioRequest;
import cl.duoc.duocconecta.bff.dto.ComentarioResponse;
import cl.duoc.duocconecta.bff.dto.ProyectoResponse;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia ms-proyectos.
 *
 * <p>Publicar, editar y borrar los hace el frontend directo: no hay nada que componer. Listar y
 * comentar pasan por acá para sumarles quién publicó y quién comentó.</p>
 */
@Service
public class ProyectosClient {

    private static final String RUTA = "/api/v1/proyectos";

    private static final ParameterizedTypeReference<List<ProyectoResponse>> LISTA =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<List<ComentarioResponse>> COMENTARIOS =
            new ParameterizedTypeReference<>() {};

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public ProyectosClient(RestClient clienteMsProyectos, UsuarioActual usuarioActual) {
        this.cliente = clienteMsProyectos;
        this.usuarioActual = usuarioActual;
    }

    /** Proyectos que la persona autenticada tiene permitido ver. */
    public List<ProyectoResponse> listar() {
        List<ProyectoResponse> respuesta = cliente.get()
                .uri(RUTA)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .retrieve()
                .body(LISTA);
        return respuesta == null ? List.of() : respuesta;
    }

    /** Hilo de comentarios de un proyecto. */
    public List<ComentarioResponse> comentarios(UUID proyectoId) {
        List<ComentarioResponse> respuesta = cliente.get()
                .uri(RUTA + "/{id}/comentarios", proyectoId)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .retrieve()
                .body(COMENTARIOS);
        return respuesta == null ? List.of() : respuesta;
    }

    /** Publica un comentario en un proyecto. */
    public ComentarioResponse comentar(UUID proyectoId, ComentarioRequest comentario) {
        return cliente.post()
                .uri(RUTA + "/{id}/comentarios", proyectoId)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(comentario)
                .retrieve()
                .body(ComentarioResponse.class);
    }

    /** La cabecera Authorization con el token original de la petición en curso. */
    private String cabeceraAuthorization() {
        return "Bearer " + usuarioActual.tokenActual().getTokenValue();
    }
}
