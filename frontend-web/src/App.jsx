// Estructura de la aplicación: pantalla de login si no hay sesión, o la aplicación con su
// cabecera y sus vistas (Vitrina, Perfil y Diagnóstico) si la persona ya entró.
import { useCallback, useEffect, useState } from 'react'
import { cuentaActual, iniciarSesion, cerrarSesion, claimsDelToken, alCambiarSesion } from './auth'
import { colaboracionesRecibidas, responderColaboracion } from './api'
import { iniciales, fechaLarga } from './catalogo'
import Perfil from './Perfil'
import Vitrina from './Vitrina'

export default function App() {
  // La cuenta se guarda en estado y no se lee en cada render: así, cuando la sesión se pierde
  // o se cierra, la aplicación vuelve sola al login en vez de quedar mostrando vistas vacías.
  const [cuenta, setCuenta] = useState(cuentaActual())
  const [vista, setVista] = useState('vitrina')

  useEffect(() => alCambiarSesion(setCuenta), [])

  if (!cuenta) return <Login />

  return (
    <div className="app">
      <Cabecera vista={vista} irA={setVista} correo={cuenta.username} />
      <main>
        {vista === 'vitrina' && <Vitrina />}
        {vista === 'perfil' && <Perfil />}
        {vista === 'token' && <Diagnostico />}
      </main>
    </div>
  )
}

/**
 * Cabecera fija. Las solicitudes pendientes están acá y no solo en el perfil porque son lo único
 * que le pide algo a la persona: si no se ven al entrar, quien pidió contacto queda esperando.
 */
function Cabecera({ vista, irA, correo }) {
  const [pendientes, setPendientes] = useState([])
  const [abierto, setAbierto] = useState(null)   // 'solicitudes' | 'cuenta' | null
  const [nombre, setNombre] = useState('')

  const recargarPendientes = useCallback(() => {
    colaboracionesRecibidas()
      .then((todas) => setPendientes(todas.filter((s) => s.estado === 'PENDIENTE')))
      .catch(() => setPendientes([]))
  }, [])

  useEffect(() => { recargarPendientes() }, [recargarPendientes])

  // El nombre sale del token y no del perfil: sirve para el avatar antes de cargar nada más.
  useEffect(() => {
    claimsDelToken().then((c) => setNombre(c.name ?? correo)).catch(() => setNombre(correo))
  }, [correo])

  const responder = async (solicitud, aceptar) => {
    const compartirTelefono = aceptar && confirm(
      'Al aceptar se comparte tu correo institucional y tus redes.\n\n'
      + '¿Quieres compartir también tu teléfono?')
    await responderColaboracion(solicitud.id, aceptar, compartirTelefono)
    recargarPendientes()
  }

  const abrir = (cual) => setAbierto(abierto === cual ? null : cual)

  return (
    <header>
      <div className="barra">
        <div className="marca">
          <span className="logo" />
          <strong>DuocConecta</strong>
        </div>

        <nav>
          <button className={vista === 'vitrina' ? 'activo' : ''} onClick={() => irA('vitrina')}>
            Vitrina
          </button>
          <button className={vista === 'perfil' ? 'activo' : ''} onClick={() => irA('perfil')}>
            Perfil
          </button>
          {/* La biblioteca de prompts es la siguiente entrega. Se deja a la vista, apagada,
              para que se entienda que está planificada y no olvidada. */}
          <span className="pronto" title="Biblioteca de prompts: llega en la próxima entrega">
            Prompts<span>Pronto</span>
          </span>
        </nav>

        <div className="desplegable">
          <button className="boton-barra" onClick={() => abrir('solicitudes')}>
            Solicitudes
            <span className={pendientes.length ? 'contador hay' : 'contador'}>{pendientes.length}</span>
          </button>
          {abierto === 'solicitudes' && (
            <div className="panel-flotante ancho">
              <h4>Solicitudes recibidas pendientes</h4>
              {pendientes.length === 0 ? (
                <p className="ayuda">No tienes solicitudes pendientes.</p>
              ) : (
                pendientes.map((s) => (
                  <div key={s.id} className="fila-solicitud" style={{ marginBottom: 10 }}>
                    <p className="persona">{s.solicitante?.nombre ?? 'Alguien de la comunidad'}</p>
                    <p className="detalle">quiere compartir contacto · {fechaLarga(s.fechaSolicitud)}</p>
                    <div className="acciones">
                      <button className="principal crece" onClick={() => responder(s, true)}>Aceptar</button>
                      <button className="secundario crece" onClick={() => responder(s, false)}>Rechazar</button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        <div className="desplegable">
          <button className="avatar" onClick={() => abrir('cuenta')}>{iniciales(nombre)}</button>
          {abierto === 'cuenta' && (
            <div className="panel-flotante angosto">
              <p className="ayuda" style={{ margin: '6px 8px 10px' }}>{correo}</p>
              <button className="opcion" onClick={() => { irA('perfil'); setAbierto(null) }}>
                Ver mi perfil
              </button>
              <button className="opcion" onClick={() => { irA('token'); setAbierto(null) }}>
                Diagnóstico del token
              </button>
              <button className="opcion salir" onClick={cerrarSesion}>Cerrar sesión</button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

/** Pantalla de entrada. El botón dispara el flujo Authorization Code + PKCE contra Azure AD. */
function Login() {
  return (
    <div className="login">
      <div className="login-panel">
        <span className="logo" />
        <h1>Los proyectos se muestran.<br />Los contactos se piden.</h1>
        <p>
          Publica lo que estás haciendo, encuentra gente de otras carreras y sedes, y comparte
          tus datos solo con quien tú aceptes.
        </p>
      </div>
      <div className="login-form">
        <h2>Entra con tu cuenta Duoc</h2>
        <p className="ayuda">
          El acceso se valida con el login institucional. Solo se permiten correos de
          dominios de Duoc UC.
        </p>
        <button className="principal" onClick={iniciarSesion} style={{ marginTop: 20 }}>
          Entrar con Microsoft
        </button>
      </div>
    </div>
  )
}

/**
 * El contenido del token. Comprueba de un vistazo que el login trae lo que el backend necesita:
 * audiencia, identificador y correo. Si el correo no llega, el backend responde 403.
 */
function Diagnostico() {
  const [claims, setClaims] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    // La decodificación vive en auth.js, junto al resto del manejo del token.
    claimsDelToken().then(setClaims).catch((e) => setError(e.message))
  }, [])

  if (error) return <div className="error">{error}</div>
  if (!claims) return <div className="cargando">Pidiendo el token…</div>

  const importantes = ['aud', 'iss', 'oid', 'email', 'preferred_username', 'upn', 'roles', 'scp']

  return (
    <section className="tarjeta">
      <h2>Diagnóstico del token</h2>
      <p className="ayuda">Lo que Azure AD le entrega al backend en cada petición.</p>

      <div className="tabla-scroll">
        <table className="claims">
          <tbody>
            {importantes.map((c) => (
              <tr key={c}>
                <th>{c}</th>
                <td>{claims[c] ? JSON.stringify(claims[c]) : <em>no viene</em>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <details>
        <summary>Ver el token completo</summary>
        <pre>{JSON.stringify(claims, null, 2)}</pre>
      </details>
    </section>
  )
}
