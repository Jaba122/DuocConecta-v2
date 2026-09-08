package cl.duoc.duocconecta.contacto.service;

import cl.duoc.duocconecta.contacto.domain.EstadoSolicitud;
import cl.duoc.duocconecta.contacto.domain.SolicitudContacto;
import cl.duoc.duocconecta.contacto.dto.DecisionSolicitud;
import cl.duoc.duocconecta.contacto.dto.SolicitudDatos;
import cl.duoc.duocconecta.contacto.dto.SolicitudRespuesta;
import cl.duoc.duocconecta.contacto.exception.ConflictoDeEstadoException;
import cl.duoc.duocconecta.contacto.exception.OperacionNoPermitidaException;
import cl.duoc.duocconecta.contacto.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.contacto.exception.SolicitudInvalidaException;
import cl.duoc.duocconecta.contacto.repository.RepositorioSolicitudes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitudes de colaboración.
 *
 * <p>Devuelve DTOs desde dentro de la transacción: con {@code open-in-view: false} la sesión se
 * cierra al salir del servicio y traducir después sería tarde.</p>
 */
@Service
@RequiredArgsConstructor
public class ServicioSolicitudes {

    private final RepositorioSolicitudes repositorioSolicitudes;

    /** Ni a uno mismo, ni dos pendientes a la misma persona: la bandeja no se debe poder inundar. */
    @Transactional
    public SolicitudRespuesta crear(SolicitudDatos dto, String solicitanteId) {
        if (solicitanteId.equals(dto.solicitadoId())) {
            throw new SolicitudInvalidaException("No puedes solicitar contacto contigo mismo");
        }

        repositorioSolicitudes.findBySolicitanteIdAndSolicitadoIdAndEstado(
                        solicitanteId, dto.solicitadoId(), EstadoSolicitud.PENDIENTE)
                .ifPresent(s -> {
                    throw new ConflictoDeEstadoException(
                            "Ya le enviaste una solicitud a esta persona y sigue pendiente");
                });

        SolicitudContacto solicitud = SolicitudContacto.builder()
                .solicitanteId(solicitanteId)
                .solicitadoId(dto.solicitadoId())
                .proyectoId(dto.proyectoId())
                .mensaje(dto.mensaje())
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        // TODO (EP3): publicar el evento "solicitud.creada" para avisar por correo.
        // Hoy el destinatario se entera consultando GET /api/v1/colaboraciones/recibidas.
        return SolicitudRespuesta.desdeEntidad(repositorioSolicitudes.save(solicitud));
    }

    /**
     * Solo responde la destinataria, y solo mientras siga pendiente. Los datos se guardan
     * únicamente si acepta: ese es el punto exacto del consentimiento.
     */
    @Transactional
    public SolicitudRespuesta responder(UUID id, String usuarioId, DecisionSolicitud respuesta) {
        SolicitudContacto solicitud = repositorioSolicitudes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + id));

        if (!solicitud.getSolicitadoId().equals(usuarioId)) {
            throw new OperacionNoPermitidaException(
                    "Solo el usuario solicitado puede responder esta solicitud");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoDeEstadoException("Esta solicitud ya fue respondida");
        }

        if (respuesta.aceptar()) {
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
            solicitud.setCorreoCompartido(respuesta.correo());
            solicitud.setTelefonoCompartido(respuesta.telefono());
            solicitud.setRedesCompartidas(respuesta.redes());
        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        }
        solicitud.setFechaRespuesta(Instant.now());

        return SolicitudRespuesta.desdeEntidad(repositorioSolicitudes.save(solicitud));
    }

    /** Solicitudes que le llegaron a esta persona, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> recibidas(String usuarioId) {
        return repositorioSolicitudes.findBySolicitadoIdOrderByFechaSolicitudDesc(usuarioId).stream()
                .map(SolicitudRespuesta::desdeEntidad)
                .toList();
    }

    /** Solicitudes que esta persona envió, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<SolicitudRespuesta> enviadas(String usuarioId) {
        return repositorioSolicitudes.findBySolicitanteIdOrderByFechaSolicitudDesc(usuarioId).stream()
                .map(SolicitudRespuesta::desdeEntidad)
                .toList();
    }
}
