package cl.duoc.duocconecta.contacto.config;

import cl.duoc.duocconecta.contacto.domain.SolicitudContacto;
import cl.duoc.duocconecta.mensajeria.MensajeriaConstantes;
import cl.duoc.duocconecta.mensajeria.SolicitudCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import static cl.duoc.duocconecta.mensajeria.MensajeriaConstantes.EXCHANGE_CONTACTO;
import static cl.duoc.duocconecta.mensajeria.MensajeriaConstantes.RK_SOLICITUD_CREADA;


@Service
public class SolicitudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public SolicitudEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarSolicitudCreada(SolicitudContacto s) {
        var evento = new SolicitudCreadaEvent(
                s.getId(), s.getProyectoId(), s.getSolicitanteId(), s.getSolicitadoId());
        rabbitTemplate.convertAndSend(EXCHANGE_CONTACTO, RK_SOLICITUD_CREADA, evento);
        log.info("Evento solicitud.creada publicado: {}", s.getId());
    }
}
