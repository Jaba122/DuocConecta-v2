package cl.duoc.duocconecta.contacto.repository;

import cl.duoc.duocconecta.contacto.domain.EstadoSolicitud;
import cl.duoc.duocconecta.contacto.domain.SolicitudContacto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las solicitudes de colaboración. */
public interface SolicitudRepository extends JpaRepository<SolicitudContacto, UUID> {

    /** Bandeja de entrada: lo que le pidieron a esta persona, lo más nuevo primero. */
    List<SolicitudContacto> findBySolicitadoIdOrderByFechaSolicitudDesc(String solicitadoId);

    /** Bandeja de salida: lo que esta persona pidió, lo más nuevo primero. */
    List<SolicitudContacto> findBySolicitanteIdOrderByFechaSolicitudDesc(String solicitanteId);

    /** Sirve para no permitir dos solicitudes pendientes entre las mismas dos personas. */
    Optional<SolicitudContacto> findBySolicitanteIdAndSolicitadoIdAndEstado(
            String solicitanteId, String solicitadoId, EstadoSolicitud estado);
}
