// Vitrina de proyectos: la pantalla principal.
// Desde aquí se publica, se filtra, se abre el detalle y se pide contacto.
import { useEffect, useMemo, useState } from 'react'
import {
  listarProyectos, publicarProyecto, editarProyecto, eliminarProyecto,
  solicitarColaboracion, colaboracionesEnviadas,
} from './api'
import { claimsDelToken } from './auth'
import { escuelaDe } from './catalogo'
import Aviso, { useAviso } from './Aviso'
import DetalleProyecto from './DetalleProyecto'
import FiltrosVitrina from './FiltrosVitrina'
import FormularioProyecto from './FormularioProyecto'
import TarjetaProyecto from './TarjetaProyecto'

const SIN_FILTROS = { texto: '', escuela: '', carrera: '', herramienta: '', estado: '' }

export default function Vitrina() {
  const [proyectos, setProyectos] = useState(null)
  const [miOid, setMiOid] = useState(null)
  const [error, setError] = useState(null)
  const { texto: aviso, avisar } = useAviso()

  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [abiertoId, setAbiertoId] = useState(null)
  const [editando, setEditando] = useState(null)   // null cerrado · {} nuevo · proyecto si edita

  // A quién le pedí contacto ya, para no ofrecer dos veces lo mismo.
  const [yaPedidos, setYaPedidos] = useState(new Set())

  // El oid dice qué proyectos son propios. Sale del token, no del perfil, que es editable.
  useEffect(() => {
    claimsDelToken().then((c) => setMiOid(c.oid)).catch(() => setMiOid(null))
  }, [])

  useEffect(() => { recargar() }, [])

  function recargar() {
    setError(null)
    listarProyectos().then(setProyectos).catch((e) => { setProyectos([]); setError(e.message) })
    // Una solicitud rechazada no bloquea para siempre: solo cuentan las que siguen en pie.
    colaboracionesEnviadas()
      .then((enviadas) => setYaPedidos(new Set(
        enviadas.filter((s) => s.estado !== 'RECHAZADA').map((s) => s.solicitadoId))))
      .catch(() => setYaPedidos(new Set()))
  }

  const guardar = async (datos) => {
    try {
      if (datos.id) {
        await editarProyecto(datos.id, datos)
        avisar('Proyecto actualizado.')
      } else {
        await publicarProyecto(datos)
        avisar('Proyecto publicado en la vitrina.')
      }
      setEditando(null)
      recargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const borrar = async (proyecto) => {
    if (!confirm(`¿Eliminar "${proyecto.nombre}"? No se puede deshacer.`)) return
    try {
      await eliminarProyecto(proyecto.id)
      setAbiertoId(null)
      avisar('Proyecto eliminado.')
      recargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const pedirContacto = async (proyecto) => {
    const mensaje = prompt(
      `Cuéntale a ${proyecto.autor?.nombre ?? 'quien publicó este proyecto'} por qué quieres `
      + `escribirle, sobre "${proyecto.nombre}":`,
      'Hola, me interesa tu proyecto y me gustaría sumarme.')
    if (mensaje === null) return

    try {
      await solicitarColaboracion({
        solicitadoId: proyecto.propietarioId,
        proyectoId: proyecto.id,
        mensaje,
      })
      setYaPedidos(new Set([...yaPedidos, proyecto.propietarioId]))
      avisar('Solicitud enviada. Si acepta, sus datos aparecerán en tu perfil.')
    } catch (e) {
      setError(e.message)
    }
  }

  const visibles = useMemo(() => filtrar(proyectos ?? [], filtros), [proyectos, filtros])
  const abierto = proyectos?.find((p) => p.id === abiertoId) ?? null

  if (proyectos === null) return <div className="cargando">Cargando la vitrina…</div>

  return (
    <section>
      <div className="titulo-pagina">
        <div>
          <h1>Vitrina de proyectos</h1>
          <p>Lo que se está construyendo ahora en Duoc UC.</p>
        </div>
        <button className="principal" onClick={() => setEditando({})}>Publicar proyecto</button>
      </div>

      {error && <div className="error">{error}</div>}

      <FiltrosVitrina
        filtros={filtros}
        alCambiar={setFiltros}
        proyectos={proyectos}
        coinciden={visibles.length}
      />

      {visibles.length === 0 ? (
        <div className="vacio">
          <div className="logo" />
          <h3>{proyectos.length === 0 ? 'Aún no hay proyectos publicados' : 'Ningún proyecto coincide'}</h3>
          <p className="ayuda">
            {proyectos.length === 0
              ? 'Publica el tuyo y serás el primero en aparecer.'
              : 'Prueba con menos filtros, o publica el primero con ese perfil y deja que te encuentren.'}
          </p>
          <div className="acciones" style={{ justifyContent: 'center', marginTop: 20 }}>
            {proyectos.length > 0 && (
              <button className="secundario" onClick={() => setFiltros(SIN_FILTROS)}>Limpiar filtros</button>
            )}
            <button className="principal" onClick={() => setEditando({})}>Publicar proyecto</button>
          </div>
        </div>
      ) : (
        <div className="grilla">
          {visibles.map((p) => (
            <TarjetaProyecto
              key={p.id}
              proyecto={p}
              esMio={p.propietarioId === miOid}
              yaSolicitado={yaPedidos.has(p.propietarioId)}
              alAbrir={() => setAbiertoId(p.id)}
              alPedirContacto={() => pedirContacto(p)}
            />
          ))}
        </div>
      )}

      {abierto && (
        <DetalleProyecto
          proyecto={abierto}
          esMio={abierto.propietarioId === miOid}
          yaSolicitado={yaPedidos.has(abierto.propietarioId)}
          alPedirContacto={() => pedirContacto(abierto)}
          alEditar={() => { setEditando(abierto); setAbiertoId(null) }}
          alBorrar={() => borrar(abierto)}
          alCerrar={() => setAbiertoId(null)}
        />
      )}

      {editando && (
        <FormularioProyecto
          proyecto={editando.id ? editando : null}
          alGuardar={guardar}
          alCerrar={() => setEditando(null)}
        />
      )}

      <Aviso texto={aviso} />
    </section>
  )
}

/**
 * Los filtros son opcionales y se combinan. Escuela y carrera son excluyentes: el mismo
 * desplegable pone uno u otro. No hay filtro de sede porque hoy la plataforma tiene una sola.
 */
function filtrar(proyectos, filtros) {
  const buscado = filtros.texto.trim().toLowerCase()

  return proyectos.filter((p) => {
    const texto = `${p.nombre} ${p.resumen ?? ''} ${p.descripcion ?? ''}`.toLowerCase()
    return (!buscado || texto.includes(buscado))
      && (!filtros.escuela || escuelaDe(p.autor?.carrera) === filtros.escuela)
      && (!filtros.carrera || p.autor?.carrera === filtros.carrera)
      && (!filtros.herramienta || p.herramientas?.includes(filtros.herramienta))
      && (!filtros.estado || p.estado === filtros.estado)
  })
}
