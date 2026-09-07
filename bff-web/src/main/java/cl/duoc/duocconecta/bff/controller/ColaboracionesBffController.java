package cl.duoc.duocconecta.bff.controller;

import cl.duoc.duocconecta.bff.dto.ColaboracionResponse;
import cl.duoc.duocconecta.bff.dto.PerfilUsuarioDto;
import cl.duoc.duocconecta.bff.dto.RespuestaColaboracionRequest;
import cl.duoc.duocconecta.bff.dto.SolicitudColaboracionRequest;
import cl.duoc.duocconecta.bff.service.ContactoClient;
import cl.duoc.duocconecta.bff.dto.DatosDeContacto;
import cl.duoc.duocconecta.bff.service.ResolvedorAutores;
import cl.duoc.duocconecta.bff.service.UsuariosClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solicitudes de colaboración, vistas desde el frontend.
 *
 * <p>Existe además del microservicio por una razón concreta: al aceptar, los datos de contacto se
 * arman solos con el perfil de quien acepta, en vez de pedirle que los escriba a mano. Componer
 * datos de ms-usuarios y ms-contacto es tarea del BFF, que es la única capa que puede hablar con
 * los dos sin que ninguno lea la base del otro.</p>
 */
@RestController
@RequestMapping("/api/v1/bff/colaboraciones")
@Tag(name = "BFF · Colaboraciones", description = "Solicitudes de contacto con los datos ya compuestos")
@SecurityRequirement(name = "bearer-jwt")
public class ColaboracionesBffController {

    /** Roles que pueden pedir y responder colaboraciones. */
    private static final String ROLES_DE_LA_PLATAFORMA =
            "hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ACADEMICO')";

    /** El campo de la base admite 500 caracteres; se recorta antes de enviarlo. */
    private static final int LARGO_MAXIMO_DATOS = 500;

    private final ContactoClient contactoClient;
    private final UsuariosClient usuariosClient;
    private final ResolvedorAutores resolvedorAutores;

    public ColaboracionesBffController(ContactoClient contactoClient, UsuariosClient usuariosClient,
                                       ResolvedorAutores resolvedorAutores) {
        this.contactoClient = contactoClient;
        this.usuariosClient = usuariosClient;
        this.resolvedorAutores = resolvedorAutores;
    }

    /** Envía una solicitud de colaboración a otra persona. */
    @Operation(
            summary = "Envía una solicitud de colaboración",
            description = "Queda PENDIENTE hasta que la otra parte responda. No se comparte ningún dato todavía.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud enviada"),
            @ApiResponse(responseCode = "403", description = "Solicitud a uno mismo o ya hay una pendiente", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es válido", content = @Content)
    })
    @PostMapping
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public ResponseEntity<ColaboracionResponse> solicitar(
            @Valid @RequestBody SolicitudColaboracionRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactoClient.crear(solicitud));
    }

    /**
     * Acepta o rechaza una solicitud recibida.
     *
     * <p>Al aceptar, el BFF lee el perfil propio y arma con él los datos que se comparten, así la
     * persona no tiene que escribirlos. Al rechazar no se envía nada.</p>
     */
    @Operation(
            summary = "Acepta o rechaza una solicitud recibida",
            description = "Solo responde la persona destinataria, y una sola vez. Al aceptar se comparten sus datos de contacto; al rechazar, ninguno.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud respondida"),
            @ApiResponse(responseCode = "403", description = "No eres el destinatario, o ya fue respondida", content = @Content),
            @ApiResponse(responseCode = "404", description = "La solicitud no existe", content = @Content)
    })
    @PatchMapping("/{id}/responder")
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public ResponseEntity<ColaboracionResponse> responder(
            @Parameter(description = "Identificador de la solicitud") @PathVariable UUID id,
            @RequestBody RespuestaColaboracionRequest respuesta) {

        DatosDeContacto contacto = respuesta.aceptar()
                ? datosDeContactoPropios(respuesta.compartirTelefono())
                : DatosDeContacto.NINGUNO;

        return ResponseEntity.ok(contactoClient.responder(id, respuesta.aceptar(), contacto));
    }

    /** Solicitudes que otras personas me enviaron. */
    @Operation(
            summary = "Lista las solicitudes recibidas",
            description = "Bandeja de entrada: lo que otras personas pidieron, con su estado.")
    @ApiResponse(responseCode = "200", description = "Listado de solicitudes recibidas")
    @GetMapping("/recibidas")
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public ResponseEntity<List<ColaboracionResponse>> recibidas() {
        return ResponseEntity.ok(conPersonas(contactoClient.recibidas()));
    }

    /** Solicitudes que envié, con los datos de contacto de las que ya fueron aceptadas. */
    @Operation(
            summary = "Lista las solicitudes enviadas",
            description = "En las aceptadas vienen el correo, el teléfono y las redes que la otra persona decidió compartir.")
    @ApiResponse(responseCode = "200", description = "Listado de solicitudes enviadas")
    @GetMapping("/enviadas")
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public ResponseEntity<List<ColaboracionResponse>> enviadas() {
        return ResponseEntity.ok(conPersonas(contactoClient.enviadas()));
    }

    /**
     * Reemplaza los identificadores por personas antes de devolver el listado.
     *
     * <p>ms-contacto guarda el oid de cada parte y nada más. Sin esta traducción la bandeja diría
     * "00000000-0000… quiere compartir contacto contigo".</p>
     */
    private List<ColaboracionResponse> conPersonas(List<ColaboracionResponse> solicitudes) {
        ResolvedorAutores.Consulta consulta = resolvedorAutores.abrir();
        return solicitudes.stream()
                .map(s -> s.conPersonas(consulta.autorDe(s.solicitanteId()),
                        consulta.autorDe(s.solicitadoId())))
                .toList();
    }

    /**
     * Arma los datos de contacto con el perfil de quien está aceptando.
     *
     * <p>El correo institucional va siempre porque es el mínimo para poder escribirse. El teléfono
     * es opcional y se suma solo si la persona lo pidió: es el dato más sensible de todos.</p>
     */
    private DatosDeContacto datosDeContactoPropios(boolean incluirTelefono) {
        PerfilUsuarioDto perfil = usuariosClient.obtenerPerfilPropio();
        List<String> redes = usuariosClient.obtenerRedesPropias();

        return new DatosDeContacto(
                perfil.correo(),
                incluirTelefono ? perfil.telefono() : null,
                (redes == null || redes.isEmpty()) ? null : String.join(", ", redes));
    }
}
