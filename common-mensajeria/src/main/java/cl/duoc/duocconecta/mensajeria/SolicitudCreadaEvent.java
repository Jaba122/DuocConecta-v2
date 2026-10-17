package cl.duoc.duocconecta.mensajeria;

import java.util.UUID;

public record SolicitudCreadaEvent(UUID solicitudId, UUID proyectoId,
                                   String solicitanteId, String solicitadoId) {}