package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.AutorDto;
import cl.duoc.duocconecta.bff.dto.PerfilPublicoDto;
import cl.duoc.duocconecta.bff.dto.PerfilUsuarioDto;
import cl.duoc.duocconecta.bff.dto.RedesDto;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia ms-usuarios.
 *
 * <p>Propaga el token original en vez de una credencial propia: el microservicio resuelve los
 * permisos con la identidad real, y el BFF nunca puede pedir más que el usuario.</p>
 */
@Service
public class UsuariosClient {

    private static final String RUTA_PERFIL_PROPIO = "/api/v1/usuarios/me";
    private static final String RUTA_REDES_PROPIAS = "/api/v1/usuarios/me/redes";
    private static final String RUTA_PERFIL_POR_OID = "/api/v1/usuarios/por-oid/{oid}";

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public UsuariosClient(RestClient clienteMsUsuarios, UsuarioActual usuarioActual) {
        this.cliente = clienteMsUsuarios;
        this.usuarioActual = usuarioActual;
    }

    /** Si es el primer ingreso, el microservicio crea el perfil en el momento. */
    public PerfilUsuarioDto obtenerPerfilPropio() {
        return cliente.get()
                .uri(RUTA_PERFIL_PROPIO)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .retrieve()
                .body(PerfilUsuarioDto.class);
    }

    public List<String> obtenerRedesPropias() {
        RedesDto respuesta = cliente.get()
                .uri(RUTA_REDES_PROPIAS)
                .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                .retrieve()
                .body(RedesDto.class);

        return (respuesta == null || respuesta.redes() == null) ? List.of() : respuesta.redes();
    }

    /**
     * Devuelve {@link AutorDto#DESCONOCIDO} si el perfil no existe o está oculto: que alguien se
     * haya ocultado no es motivo para que falle la pantalla entera.
     */
    public AutorDto buscarAutorPorOid(String oid) {
        try {
            PerfilPublicoDto perfil = cliente.get()
                    .uri(RUTA_PERFIL_POR_OID, oid)
                    .header(HttpHeaders.AUTHORIZATION, cabeceraAuthorization())
                    .retrieve()
                    .body(PerfilPublicoDto.class);
            return perfil == null
                    ? AutorDto.DESCONOCIDO
                    : new AutorDto(perfil.nombre(), perfil.carrera(), perfil.sede());
        } catch (HttpClientErrorException.NotFound ignorada) {
            return AutorDto.DESCONOCIDO;
        }
    }

    /** La cabecera Authorization con el token original de la petición en curso. */
    private String cabeceraAuthorization() {
        return "Bearer " + usuarioActual.tokenActual().getTokenValue();
    }
}
