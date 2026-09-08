package cl.duoc.duocconecta.proyectos.exception;

import cl.duoc.duocconecta.comun.seguridad.ManejadorErroresBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores propios de ms-proyectos. Los comunes vienen de {@link ManejadorErroresBase}. */
@RestControllerAdvice
public class ManejadorErrores extends ManejadorErroresBase {

    /** El proyecto o el comentario no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException excepcion) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", excepcion.getMessage());
    }

    /** El proyecto es de otra persona, o es privado. */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ProblemDetail manejarNoPermitido(OperacionNoPermitidaException excepcion) {
        return problema(HttpStatus.FORBIDDEN, "Sin permiso", excepcion.getMessage());
    }
}
