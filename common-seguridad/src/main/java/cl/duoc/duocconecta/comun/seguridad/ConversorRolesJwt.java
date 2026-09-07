package cl.duoc.duocconecta.comun.seguridad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte el JWT en una autenticación de Spring, con el rol como authority.
 *
 * <p>Usa los App Roles del token si vienen; si no, deriva el rol del dominio del correo. Un
 * dominio no autorizado deja al usuario sin authorities, y {@code @PreAuthorize} responde 403.</p>
 */
public class ConversorRolesJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final TokenClaims tokenClaims;
    private final ResolvedorRol resolvedorRol;

    public ConversorRolesJwt(TokenClaims tokenClaims, ResolvedorRol resolvedorRol) {
        this.tokenClaims = tokenClaims;
        this.resolvedorRol = resolvedorRol;
    }

    /** El principal se nombra con el correo, para que los registros se lean. */
    @Override
    public AbstractAuthenticationToken convert(Jwt token) {
        List<GrantedAuthority> authorities = new ArrayList<>(calcularAuthorities(token));

        String nombrePrincipal = tokenClaims.correo(token)
                .or(() -> tokenClaims.oid(token))
                .orElse(token.getSubject());

        return new JwtAuthenticationToken(token, authorities, nombrePrincipal);
    }

    private List<GrantedAuthority> calcularAuthorities(Jwt token) {
        List<GrantedAuthority> desdeAppRoles = authoritiesDesdeAppRoles(token);
        if (!desdeAppRoles.isEmpty()) {
            return desdeAppRoles;
        }
        return authoritiesDesdeDominio(token);
    }

    /** Se ignora todo rol que no exista acá: uno suelto en Azure AD no debe dar permisos. */
    private List<GrantedAuthority> authoritiesDesdeAppRoles(Jwt token) {
        return tokenClaims.rolesDelToken(token).stream()
                .map(this::aRolConocido)
                .flatMap(Optional::stream)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(rol.comoAuthority()))
                .distinct()
                .toList();
    }

    /** Dominio no autorizado: lista vacía, y el usuario queda sin permisos. */
    private List<GrantedAuthority> authoritiesDesdeDominio(Jwt token) {
        return tokenClaims.correo(token)
                .flatMap(resolvedorRol::resolverPorCorreo)
                .map(rol -> List.<GrantedAuthority>of(new SimpleGrantedAuthority(rol.comoAuthority())))
                .orElseGet(List::of);
    }

    private Optional<Rol> aRolConocido(String nombreRol) {
        try {
            return Optional.of(Rol.valueOf(nombreRol.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException noEsUnRolDeLaPlataforma) {
            return Optional.empty();
        }
    }
}
