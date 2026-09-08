package cl.duoc.duocconecta.bff.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP hacia los microservicios.
 *
 * <p>Con timeout explícito: sin él, un microservicio colgado deja al BFF esperando para siempre y
 * se lleva al frontend con él. Al agotarse, el manejador de errores devuelve 503.</p>
 */
@Configuration
public class RestClientConfig {

    /** Cliente hacia ms-usuarios: perfiles y redes. */
    @Bean
    public RestClient clienteMsUsuarios(PropiedadesBff propiedades) {
        return construir(propiedades.getUrlMsUsuarios(), propiedades.getTimeoutSegundos());
    }

    /** Cliente hacia ms-proyectos: la vitrina. */
    @Bean
    public RestClient clienteMsProyectos(PropiedadesBff propiedades) {
        return construir(propiedades.getUrlMsProyectos(), propiedades.getTimeoutSegundos());
    }

    /** Cliente hacia ms-contacto: solicitudes de colaboración. */
    @Bean
    public RestClient clienteMsContacto(PropiedadesBff propiedades) {
        return construir(propiedades.getUrlMsContacto(), propiedades.getTimeoutSegundos());
    }

    /** Timeout de conexión y de lectura. Usa el cliente del JDK para no sumar una librería. */
    private RestClient construir(String urlBase, int segundos) {
        Duration timeout = Duration.ofSeconds(segundos);

        // Tiempo máximo para establecer la conexión con el microservicio.
        HttpClient clienteHttp = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        // Tiempo máximo de espera de la respuesta una vez conectado.
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteHttp);
        fabrica.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .build();
    }
}
