package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Un comentario con el nombre de quien lo escribió ya resuelto. */
@Schema(description = "Comentario sobre un proyecto")
public record ComentarioResponse(
        UUID id,
        UUID proyectoId,
        String autorId,
        @Schema(description = "Quien comentó; null si ocultó su perfil") AutorDto autor,
        String texto,
        Instant fecha) {

    /** Copia el comentario sumándole quién lo escribió. */
    public ComentarioResponse conAutor(AutorDto quienComento) {
        return new ComentarioResponse(id, proyectoId, autorId, quienComento, texto, fecha);
    }
}
