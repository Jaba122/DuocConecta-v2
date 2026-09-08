package cl.duoc.duocconecta.proyectos.exception;

import cl.duoc.duocconecta.comun.seguridad.CorreoNoPresenteException;
import cl.duoc.duocconecta.comun.seguridad.DominioNoPermitidoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * El correo del token no pertenece a un dominio institucional autorizado.
     *
     * <p>Sin este manejador la excepción caía en el genérico y se respondía 500, que es
     * engañoso: la petición no falló, se rechazó a propósito.</p>
     */
    @ExceptionHandler(DominioNoPermitidoException.class)
    public ResponseEntity<ApiError> manejarDominioNoPermitido(DominioNoPermitidoException ex) {
        log.warn("Se rechazó un acceso desde el dominio no autorizado '{}'.", ex.getDominio());
        return construir(HttpStatus.FORBIDDEN,
                "Tu correo no pertenece a un dominio institucional de Duoc UC.", List.of());
    }

    /** El token es válido pero no trae el correo: casi siempre es configuración del tenant. */
    @ExceptionHandler(CorreoNoPresenteException.class)
    public ResponseEntity<ApiError> manejarCorreoAusente(CorreoNoPresenteException ex) {
        log.error("Token sin claim de correo: {}", ex.getMessage());
        return construir(HttpStatus.FORBIDDEN,
                "El token no incluye tu correo, así que no se puede asignar un rol.", List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ApiError> manejarNoPermitido(OperacionNoPermitidaException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Error de validación", detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> manejarConstraint(ConstraintViolationException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarGenerico(Exception ex) {
        // Se registra con la traza completa: si no, un fallo inesperado se ve igual que
        // cualquier otro 500 y no queda por dónde empezar a buscar.
        log.error("Error no controlado en ms-proyectos", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, List<String> detalles) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(status).body(body);
    }
}
