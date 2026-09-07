package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.AutorDto;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Traduce identificadores de Azure AD a personas que se puedan mostrar.
 *
 * <p>Consulta a ms-usuarios una vez por persona y no una por fila: en una vitrina, la diferencia
 * entre una llamada y treinta.</p>
 */
@Service
public class ResolvedorAutores {

    private final UsuariosClient usuariosClient;

    public ResolvedorAutores(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    /** Una consulta con memoria. Se usa y se descarta en cada petición. */
    public Consulta abrir() {
        return new Consulta();
    }

    /**
     * No se comparte entre peticiones a propósito: un dato cacheado de más sobreviviría a que
     * la persona se ocultara.
     */
    public class Consulta {

        private final Map<String, AutorDto> conocidos = new HashMap<>();

        /** Nunca null, para que quien llama no tenga que comprobarlo. */
        public AutorDto autorDe(String oid) {
            if (oid == null) {
                return AutorDto.DESCONOCIDO;
            }
            return conocidos.computeIfAbsent(oid, usuariosClient::buscarAutorPorOid);
        }
    }
}
