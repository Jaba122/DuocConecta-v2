package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Lo que el frontend envía para pedirle contacto a otra persona. */
@Schema(description = "Solicitud de colaboración hacia otra persona")
public record SolicitudColaboracionRequest(

        @NotBlank(message = "Falta indicar a quién le pedís contacto")
        @Schema(description = "Identificador de la persona a la que se le pide el contacto")
        String solicitadoId,

        @Schema(description = "Proyecto de la vitrina desde el que sale la solicitud")
        UUID proyectoId,

        @Size(max = 500)
        @Schema(description = "Mensaje para quien recibe la solicitud")
        String mensaje) {
}
