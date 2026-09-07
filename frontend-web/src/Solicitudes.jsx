// Solicitudes de colaboración y contactos desbloqueados.
// Viven dentro del perfil porque son datos personales, no de la vitrina.
import { useEffect, useState } from 'react'
import { colaboracionesRecibidas, colaboracionesEnviadas, responderColaboracion } from './api'
import { ESTADOS_SOLICITUD, fechaLarga } from './catalogo'

/** Las dos bandejas, compartidas entre solicitudes y colaboraciones para no pedirlas dos veces. */
export function useColaboraciones() {
  const [recibidas, setRecibidas] = useState(null)
  const [enviadas, setEnviadas] = useState(null)
  const [error, setError] = useState(null)

  const recargar = () => {
    setError(null)
    Promise.all([colaboracionesRecibidas(), colaboracionesEnviadas()])
      .then(([r, e]) => { setRecibidas(r); setEnviadas(e) })
      .catch((e) => { setRecibidas([]); setEnviadas([]); setError(e.message) })
  }

  useEffect(recargar, [])

  return { recibidas, enviadas, error, recargar }
}

/** Solo las recibidas y pendientes traen botones: responder es de quien recibe. */
export function MisSolicitudes({ recibidas, enviadas, alResponder }) {
  const [pestana, setPestana] = useState('recibidas')

  const lista = pestana === 'recibidas' ? recibidas : enviadas
  if (lista === null) return <p className="ayuda">Cargando tus solicitudes…</p>

  return (
    <>
      <div className="pestanas">
        <button className={pestana === 'recibidas' ? 'activo' : ''} onClick={() => setPestana('recibidas')}>
          Recibidas
        </button>
        <button className={pestana === 'enviadas' ? 'activo' : ''} onClick={() => setPestana('enviadas')}>
          Enviadas
        </button>
      </div>

      {lista.length === 0 ? (
        <p className="ayuda">
          {pestana === 'recibidas'
            ? 'Aún nadie te ha pedido contacto.'
            : 'Aún no has pedido contacto. Se hace desde la vitrina.'}
        </p>
      ) : (
        <div className="lista">
          {lista.map((s) => (
            <Fila
              key={s.id}
              solicitud={s}
              esRecibida={pestana === 'recibidas'}
              alResponder={alResponder}
            />
          ))}
        </div>
      )}
    </>
  )
}

/** Una solicitud de cualquiera de las dos bandejas. */
function Fila({ solicitud, esRecibida, alResponder }) {
  const otra = esRecibida ? solicitud.solicitante : solicitud.solicitado
  const puedeResponder = esRecibida && solicitud.estado === 'PENDIENTE'

  return (
    <div className="fila-solicitud">
      <div className="etiquetas">
        <span className={`insignia ${solicitud.estado}`}>{ESTADOS_SOLICITUD[solicitud.estado]}</span>
        <span className="fecha">{fechaLarga(solicitud.fechaSolicitud)}</span>
      </div>

      <p className="persona">{otra?.nombre ?? 'Alguien de la comunidad'}</p>
      <p className="detalle">
        {esRecibida ? 'quiere compartir contacto contigo' : 'le pediste contacto'}
        {otra?.carrera ? ` · ${otra.carrera}` : ''}
      </p>
      {solicitud.mensaje && <p className="cita">“{solicitud.mensaje}”</p>}

      {puedeResponder && (
        <div className="acciones">
          <button className="principal" onClick={() => alResponder(solicitud, true)}>Aceptar</button>
          <button className="secundario" onClick={() => alResponder(solicitud, false)}>Rechazar</button>
        </div>
      )}
    </div>
  )
}

/**
 * Contactos desbloqueados, en las dos direcciones: da igual quién pidió, importa que hubo
 * aceptación. Un campo vacío se muestra como "no lo compartió", no como un espacio en blanco.
 */
export function MisColaboraciones({ recibidas, enviadas }) {
  if (recibidas === null || enviadas === null) return <p className="ayuda">Cargando…</p>

  const aceptadas = [
    ...enviadas.filter((s) => s.estado === 'ACEPTADA').map((s) => ({ s, otra: s.solicitado })),
    ...recibidas.filter((s) => s.estado === 'ACEPTADA').map((s) => ({ s, otra: s.solicitante })),
  ]

  if (aceptadas.length === 0) {
    return <p className="ayuda">Cuando aceptes una solicitud, o te acepten una, el contacto aparecerá aquí.</p>
  }

  return (
    <div className="lista">
      {aceptadas.map(({ s, otra }) => (
        <div key={s.id} className="contacto">
          <p className="persona">{otra?.nombre ?? 'Alguien de la comunidad'}</p>
          <p className="detalle">{[otra?.carrera, otra?.sede].filter(Boolean).join(' · ')}</p>
          <dl>
            <Dato etiqueta="Correo" valor={s.correoCompartido} />
            <Dato etiqueta="Teléfono" valor={s.telefonoCompartido} />
            <Dato etiqueta="Redes" valor={s.redesCompartidas} />
          </dl>
        </div>
      ))}
    </div>
  )
}

/** Una fila de contacto. Si no vino el dato, se dice explícitamente que no se compartió. */
function Dato({ etiqueta, valor }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd className={valor ? '' : 'sin'}>{valor || 'no lo compartió'}</dd>
    </div>
  )
}

/**
 * Al aceptar no se escribe nada: el BFF arma los datos con el perfil propio. El teléfono se
 * pregunta aparte porque es el más sensible.
 */
export async function responder(solicitud, aceptar) {
  const compartirTelefono = aceptar && confirm(
    'Al aceptar se comparte tu correo institucional y tus redes.\n\n'
    + '¿Quieres compartir también tu teléfono?')

  await responderColaboracion(solicitud.id, aceptar, compartirTelefono)
}
