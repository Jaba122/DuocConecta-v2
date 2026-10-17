package cl.duoc.duocconecta.proyectos.service;

import static cl.duoc.duocconecta.mensajeria.MensajeriaConstantes.*;

import cl.duoc.duocconecta.mensajeria.SolicitudCreadaEvent;
import cl.duoc.duocconecta.mensajeria.UsuarioCreadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UsuarioEventListener {

    private static final Logger log = LoggerFactory.getLogger(UsuarioEventListener.class);

    @RabbitListener(queues = COLA_PUBLICACIONES_USUARIO_CREADO)
    public void onUsuarioCreado(UsuarioCreadoEvent evento) {
        log.info("Evento usuario.creado recibido: {}", evento.id());
    }

    @RabbitListener(queues = COLA_PUBLICACIONES_DLQ)
    public void onMensajeMuerto(Message mensaje) {
        log.error("Mensaje no entregado enviado a DLQ. Cola origen: {}, motivo: {}",
                mensaje.getMessageProperties().getHeader("x-first-death-queue"),
                mensaje.getMessageProperties().getHeader("x-first-death-reason"));
    }

    @RabbitListener(queues = COLA_PROYECTOS_SOLICITUD_CREADA)
    public void onSolicitudCreada(SolicitudCreadaEvent evento) {
        log.info("Solicitud {} recibida para el proyecto {}", evento.solicitudId(), evento.proyectoId());
    }

    @RabbitListener(queues = COLA_PROYECTOS_SOLICITUD_DLQ)
    public void onSolicitudMuerta(Message mensaje) {
        log.error("Solicitud no entregada enviada a DLQ. Cola origen: {}, motivo: {}",
                mensaje.getMessageProperties().getHeader("x-first-death-queue"),
                mensaje.getMessageProperties().getHeader("x-first-death-reason"));
    }
}