package cl.duoc.duocconecta.proyectos.dto;

import cl.duoc.duocconecta.proyectos.domain.Comentario;
import java.time.Instant;
import java.util.UUID;

/** Un comentario tal como sale de la API. El nombre del autor lo resuelve el BFF. */
public record ComentarioResponseDTO(
        UUID id,
        UUID proyectoId,
        String autorId,
        String texto,
        Instant fecha
) {
    public static ComentarioResponseDTO desdeEntidad(Comentario c) {
        return new ComentarioResponseDTO(
                c.getId(), c.getProyectoId(), c.getAutorId(), c.getTexto(), c.getFecha());
    }
}
