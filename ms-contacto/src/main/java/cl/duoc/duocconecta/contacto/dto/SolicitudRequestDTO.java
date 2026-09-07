package cl.duoc.duocconecta.contacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SolicitudRequestDTO(

        @NotBlank(message = "El id del usuario solicitado es obligatorio")
        String solicitadoId,


        UUID proyectoId,

        @Size(max = 500)
        String mensaje
) {
}