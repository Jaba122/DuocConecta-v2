package cl.duoc.duocconecta.proyectos.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {
}