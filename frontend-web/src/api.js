// Cliente del BFF. Cada petición lleva el access token de Azure AD en la cabecera Authorization.

import { obtenerToken } from './auth'

// Vacío = mismo origen, que es como queda detrás del API Gateway: llamadas relativas y sin CORS.
// En local lleva la URL completa, porque el front está en el 5173 y el BFF en el 8080.
const BASE = import.meta.env.VITE_BFF_URL ?? ''

/** Hace una petición al BFF adjuntando el token y traduciendo los errores a algo legible. */
async function pedir(ruta, opciones = {}) {
  const token = await obtenerToken()

  const respuesta = await fetch(`${BASE}${ruta}`, {
    ...opciones,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...opciones.headers,
    },
  })

  if (respuesta.status === 401) {
    throw new Error('Tu sesión no es válida o expiró. Vuelve a iniciar sesión.')
  }
  if (respuesta.status === 403) {
    // El backend responde 403 cuando el correo no pertenece a un dominio institucional,
    // y también cuando la acción es de otra persona.
    const problema = await respuesta.json().catch(() => ({}))
    throw new Error(problema.detail ?? problema.mensaje ?? 'No tienes permiso para esta operación.')
  }
  if (respuesta.status === 503) {
    throw new Error('El servicio no está respondiendo. Puede que el backend esté apagado.')
  }
  if (!respuesta.ok) {
    const problema = await respuesta.json().catch(() => ({}))
    throw new Error(problema.detail ?? problema.mensaje ?? `El servidor respondió ${respuesta.status}.`)
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

// --- Perfil (ms-usuarios, vía BFF) --------------------------------------------

/** Perfil y redes del usuario autenticado, en una sola llamada agregada por el BFF. */
export const obtenerMiPerfil = () => pedir('/api/v1/bff/mi-perfil')

/** Guarda los cambios del perfil propio. */
export const guardarMiPerfil = (perfil) =>
  pedir('/api/v1/usuarios/me', { method: 'PUT', body: JSON.stringify(perfil) })

/** Muestra u oculta el perfil propio en las búsquedas. */
export const alternarVisibilidad = () =>
  pedir('/api/v1/usuarios/me/visibilidad', { method: 'PATCH' })

// --- Vitrina de proyectos -----------------------------------------------------
//
// Listar y comentar van por el BFF, que suma quién publicó y quién comentó. Publicar, editar y
// borrar van directo a ms-proyectos: ahí no hay nada que componer.

/** Proyectos que la persona puede ver, con los datos de quien publicó cada uno. */
export const listarProyectos = () => pedir('/api/v1/bff/vitrina')

/** Publica un proyecto nuevo. */
export const publicarProyecto = (proyecto) =>
  pedir('/api/v1/proyectos', { method: 'POST', body: JSON.stringify(proyecto) })

/** Edita un proyecto propio. Cambiar la visibilidad a PRIVADO es la forma de ocultarlo. */
export const editarProyecto = (id, proyecto) =>
  pedir(`/api/v1/proyectos/${id}`, { method: 'PUT', body: JSON.stringify(proyecto) })

/** Borra un proyecto propio de forma definitiva. */
export const eliminarProyecto = (id) =>
  pedir(`/api/v1/proyectos/${id}`, { method: 'DELETE' })

/** Hilo de comentarios de un proyecto. */
export const listarComentarios = (id) => pedir(`/api/v1/bff/vitrina/${id}/comentarios`)

/** Deja un comentario. Comentar no comparte ningún dato de contacto. */
export const comentarProyecto = (id, texto) =>
  pedir(`/api/v1/bff/vitrina/${id}/comentarios`, { method: 'POST', body: JSON.stringify({ texto }) })

// --- Solicitudes de colaboración (ms-contacto, vía BFF) ------------------------
//
// Van por el BFF porque al aceptar compone los datos con el perfil de quien acepta: así nadie
// tiene que escribir su correo a mano.

/** Le pide contacto a la persona dueña de un proyecto. */
export const solicitarColaboracion = (solicitud) =>
  pedir('/api/v1/bff/colaboraciones', { method: 'POST', body: JSON.stringify(solicitud) })

/** Acepta o rechaza una solicitud recibida. */
export const responderColaboracion = (id, aceptar, compartirTelefono = false) =>
  pedir(`/api/v1/bff/colaboraciones/${id}/responder`, {
    method: 'PATCH',
    body: JSON.stringify({ aceptar, compartirTelefono }),
  })

/** Solicitudes que otras personas me enviaron. */
export const colaboracionesRecibidas = () => pedir('/api/v1/bff/colaboraciones/recibidas')

/** Solicitudes que envié, con los datos de contacto de las aceptadas. */
export const colaboracionesEnviadas = () => pedir('/api/v1/bff/colaboraciones/enviadas')
