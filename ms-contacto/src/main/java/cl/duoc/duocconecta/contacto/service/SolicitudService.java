package cl.duoc.duocconecta.contacto.service;

import cl.duoc.duocconecta.contacto.domain.EstadoSolicitud;
import cl.duoc.duocconecta.contacto.domain.SolicitudContacto;
import cl.duoc.duocconecta.contacto.dto.RespuestaSolicitudDTO;
import cl.duoc.duocconecta.contacto.dto.SolicitudRequestDTO;
import cl.duoc.duocconecta.contacto.dto.SolicitudResponseDTO;
import cl.duoc.duocconecta.contacto.exception.OperacionNoPermitidaException;
import cl.duoc.duocconecta.contacto.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.contacto.repository.SolicitudRepository;
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
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    /** Ni a uno mismo, ni dos pendientes a la misma persona: la bandeja no se debe poder inundar. */
    @Transactional
    public SolicitudResponseDTO crear(SolicitudRequestDTO dto, String solicitanteId) {
        if (solicitanteId.equals(dto.solicitadoId())) {
            throw new OperacionNoPermitidaException("No puedes solicitar contacto contigo mismo");
        }

        solicitudRepository.findBySolicitanteIdAndSolicitadoIdAndEstado(
                        solicitanteId, dto.solicitadoId(), EstadoSolicitud.PENDIENTE)
                .ifPresent(s -> {
                    throw new OperacionNoPermitidaException(
                            "Ya existe una solicitud pendiente a este usuario");
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
        return SolicitudResponseDTO.desdeEntidad(solicitudRepository.save(solicitud));
    }

    /**
     * Solo responde la destinataria, y solo mientras siga pendiente. Los datos se guardan
     * únicamente si acepta: ese es el punto exacto del consentimiento.
     */
    @Transactional
    public SolicitudResponseDTO responder(UUID id, String usuarioId, RespuestaSolicitudDTO respuesta) {
        SolicitudContacto solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + id));

        if (!solicitud.getSolicitadoId().equals(usuarioId)) {
            throw new OperacionNoPermitidaException(
                    "Solo el usuario solicitado puede responder esta solicitud");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new OperacionNoPermitidaException("Esta solicitud ya fue respondida");
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

        return SolicitudResponseDTO.desdeEntidad(solicitudRepository.save(solicitud));
    }

    /** Solicitudes que le llegaron a esta persona, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> recibidas(String usuarioId) {
        return solicitudRepository.findBySolicitadoIdOrderByFechaSolicitudDesc(usuarioId).stream()
                .map(SolicitudResponseDTO::desdeEntidad)
                .toList();
    }

    /** Solicitudes que esta persona envió, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> enviadas(String usuarioId) {
        return solicitudRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(usuarioId).stream()
                .map(SolicitudResponseDTO::desdeEntidad)
                .toList();
    }
}
