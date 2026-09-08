package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Una solicitud de colaboración tal como la ve el frontend.
 *
 * <p>El BFF la redefine en vez de importar el DTO de ms-contacto: si el microservicio cambia un
 * campo, el frontend no se entera hasta que alguien lo decida. Los tres campos de contacto solo
 * traen algo cuando el estado es ACEPTADA.</p>
 */
@Schema(description = "Solicitud de colaboración con su estado")
public record ColaboracionRespuesta(
        UUID id,
        String solicitanteId,
        @Schema(description = "Quien la envió; null si ocultó su perfil") Autor solicitante,
        String solicitadoId,
        @Schema(description = "Quien la recibió; null si ocultó su perfil") Autor solicitado,
        UUID proyectoId,
        String mensaje,
        @Schema(description = "PENDIENTE, ACEPTADA o RECHAZADA") String estado,
        @Schema(description = "Solo llega si la solicitud fue aceptada") String correoCompartido,
        String telefonoCompartido,
        String redesCompartidas,
        Instant fechaSolicitud,
        Instant fechaRespuesta) {

    /** Copia la solicitud sumándole quién es cada parte, que ms-contacto no sabe. */
    public ColaboracionRespuesta conPersonas(Autor quienEnvia, Autor quienRecibe) {
        return new ColaboracionRespuesta(id, solicitanteId, quienEnvia, solicitadoId, quienRecibe,
                proyectoId, mensaje, estado, correoCompartido, telefonoCompartido, redesCompartidas,
                fechaSolicitud, fechaRespuesta);
    }
}
