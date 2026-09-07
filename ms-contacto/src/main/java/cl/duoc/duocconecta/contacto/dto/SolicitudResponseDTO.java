package cl.duoc.duocconecta.contacto.dto;

import cl.duoc.duocconecta.contacto.domain.EstadoSolicitud;
import cl.duoc.duocconecta.contacto.domain.SolicitudContacto;
import java.time.Instant;
import java.util.UUID;

/**
 * Cómo se ve una solicitud de colaboración hacia afuera.
 *
 * <p>Los tres campos de contacto solo vienen con contenido cuando el estado es ACEPTADA: antes de
 * la aceptación no hay nada que mostrar, y esa es justamente la garantía de consentimiento. Van
 * separados para que se distinga lo que la persona decidió no compartir de lo que no tiene.</p>
 */
public record SolicitudResponseDTO(
        UUID id,
        String solicitanteId,
        String solicitadoId,
        UUID proyectoId,
        String mensaje,
        EstadoSolicitud estado,
        String correoCompartido,
        String telefonoCompartido,
        String redesCompartidas,
        Instant fechaSolicitud,
        Instant fechaRespuesta
) {
    /** Traduce la entidad a la respuesta de la API. Nunca se expone la entidad JPA. */
    public static SolicitudResponseDTO desdeEntidad(SolicitudContacto s) {
        return new SolicitudResponseDTO(
                s.getId(), s.getSolicitanteId(), s.getSolicitadoId(), s.getProyectoId(),
                s.getMensaje(), s.getEstado(),
                s.getCorreoCompartido(), s.getTelefonoCompartido(), s.getRedesCompartidas(),
                s.getFechaSolicitud(), s.getFechaRespuesta());
    }
}
