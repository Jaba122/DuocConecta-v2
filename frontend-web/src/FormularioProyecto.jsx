// Ventana para publicar o editar un proyecto: el mismo formulario en los dos casos.
// Valida antes de enviar, para no depender de un 400 del servidor.
import { useState } from 'react'
import { ESTADOS, HERRAMIENTAS_SUGERIDAS, SEDES } from './catalogo'

/** Un proyecto nuevo empieza público y buscando equipo, que es el caso más común. */
const VACIO = {
  nombre: '', resumen: '', descripcion: '', urlRepositorio: '', sede: SEDES[0],
  herramientas: [], estado: 'BUSCANDO_EQUIPO', visibilidad: 'PUBLICO', archivosAdjuntos: [],
}

export default function FormularioProyecto({ proyecto, alGuardar, alCerrar }) {
  const [datos, setDatos] = useState({ ...VACIO, ...(proyecto ?? {}) })
  const [errores, setErrores] = useState({})
  const [guardando, setGuardando] = useState(false)

  const editando = Boolean(proyecto?.id)

  const campo = (clave) => ({
    value: datos[clave] ?? '',
    onChange: (e) => {
      setDatos({ ...datos, [clave]: e.target.value })
      setErrores({ ...errores, [clave]: '' })
    },
    className: errores[clave] ? 'malo' : '',
  })

  /** Las listas se escriben separadas por comas, que es más cómodo que una línea por ítem. */
  const lista = (clave) => ({
    value: (datos[clave] ?? []).join(', '),
    onChange: (e) => setDatos({
      ...datos,
      [clave]: e.target.value.split(',').map((x) => x.trim()).filter(Boolean),
    }),
  })

  const enviar = async (evento) => {
    evento.preventDefault()

    const fallos = {}
    if (!datos.nombre.trim()) fallos.nombre = 'Ponle un nombre al proyecto.'
    if (!datos.resumen.trim()) fallos.resumen = 'El resumen es lo único que se lee en la tarjeta.'
    if (datos.resumen.length > 200) fallos.resumen = 'El resumen es de una línea: hasta 200 caracteres.'
    if (Object.keys(fallos).length > 0) { setErrores(fallos); return }

    setGuardando(true)
    try {
      await alGuardar(datos)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <>
      <div className="modal-fondo" onClick={alCerrar} />
      <div className="modal-caja">
        <div className="modal">
          <div className="modal-cabecera">
            <h2>{editando ? 'Editar proyecto' : 'Publicar un proyecto'}</h2>
            <button className="cerrar" onClick={alCerrar} aria-label="Cerrar">×</button>
          </div>

          <form onSubmit={enviar}>
            <label>
              Nombre *
              <input maxLength={150} placeholder="Nombre del proyecto" {...campo('nombre')} />
              {errores.nombre && <span className="mensaje-campo">{errores.nombre}</span>}
            </label>

            <label>
              Resumen *
              <input maxLength={200} placeholder="Una línea que resuma el proyecto" {...campo('resumen')} />
              {errores.resumen && <span className="mensaje-campo">{errores.resumen}</span>}
            </label>

            <label>
              Detalle
              <textarea
                rows={4}
                maxLength={2000}
                placeholder="Qué resuelve, en qué etapa está, qué ayuda necesitas"
                {...campo('descripcion')}
              />
            </label>

            <div className="dos-columnas">
              <label>
                Herramientas (separadas por comas)
                <input
                  list="herramientas-sugeridas"
                  placeholder={HERRAMIENTAS_SUGERIDAS.slice(0, 3).join(', ')}
                  {...lista('herramientas')}
                />
                <datalist id="herramientas-sugeridas">
                  {HERRAMIENTAS_SUGERIDAS.map((h) => <option key={h} value={h} />)}
                </datalist>
              </label>

              <label>
                Estado *
                <select {...campo('estado')}>
                  {Object.entries(ESTADOS).map(([valor, texto]) => (
                    <option key={valor} value={valor}>{texto}</option>
                  ))}
                </select>
              </label>
            </div>

            <div className="dos-columnas">
              <label>
                Sede
                <select {...campo('sede')}>
                  {SEDES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </label>

              <label>
                Visibilidad
                <select {...campo('visibilidad')}>
                  <option value="PUBLICO">Visible en la vitrina</option>
                  <option value="PRIVADO">Solo para mí</option>
                </select>
              </label>
            </div>

            <label>
              Enlace al repositorio o al trabajo
              <input type="url" placeholder="https://…" {...campo('urlRepositorio')} />
            </label>

            <label>
              Archivos adjuntos (separados por comas)
              <input placeholder="informe.pdf, prototipo.fig" {...lista('archivosAdjuntos')} />
            </label>

            <div className="acciones">
              <button className="principal" type="submit" disabled={guardando}>
                {guardando ? 'Guardando…' : (editando ? 'Guardar cambios' : 'Publicar')}
              </button>
              <button className="secundario" type="button" onClick={alCerrar}>Cancelar</button>
            </div>
          </form>
        </div>
      </div>
    </>
  )
}
