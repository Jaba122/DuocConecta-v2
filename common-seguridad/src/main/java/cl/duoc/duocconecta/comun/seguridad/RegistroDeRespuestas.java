package cl.duoc.duocconecta.comun.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Deja constancia de toda respuesta de error, sin importar qué componente la produzca.
 *
 * <p>Los manejadores de excepciones solo registran los errores que pasan por ellos. Un 403 puede
 * salir de la cadena de filtros, de la seguridad de método o del manejador de errores por defecto
 * de Spring Boot, y en esos casos no queda rastro en ningún lado: la aplicación dice que no y no
 * hay forma de saber quién lo decidió.</p>
 *
 * <p>Este filtro se pone antes que todo y mira el estado final, así que ve todas las respuestas.</p>
 */
@Configuration
public class RegistroDeRespuestas {

    private static final Logger log = LoggerFactory.getLogger(RegistroDeRespuestas.class);

    @Bean
    public OncePerRequestFilter filtroRegistroDeRespuestas() {
        return new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(HttpServletRequest peticion,
                                            HttpServletResponse respuesta,
                                            FilterChain cadena) throws ServletException, IOException {
                try {
                    cadena.doFilter(peticion, respuesta);
                } finally {
                    if (respuesta.getStatus() >= 400) {
                        Authentication quien = SecurityContextHolder.getContext().getAuthentication();
                        log.warn("{} {} -> {} · usuario={} · authorities={}",
                                peticion.getMethod(),
                                peticion.getRequestURI(),
                                respuesta.getStatus(),
                                quien == null ? "(sin autenticación)" : quien.getName(),
                                quien == null ? "(ninguna)" : quien.getAuthorities());
                    }
                }
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest peticion) {
                // El monitoreo consulta la salud cada 30 segundos: no tiene sentido registrarlo.
                return peticion.getRequestURI().startsWith("/actuator");
            }
        };
    }

    /** Se registra antes que la cadena de seguridad para ver también lo que ella rechaza. */
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<OncePerRequestFilter>
            ordenDelRegistroDeRespuestas(OncePerRequestFilter filtroRegistroDeRespuestas) {
        var registro = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filtroRegistroDeRespuestas);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }
}
