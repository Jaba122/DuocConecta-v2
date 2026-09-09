# Guía del equipo · DuocConecta

Cómo operar el proyecto y qué mirar cuando algo falla.

`CLAUDE.md` explica **qué** hay construido y **por qué** se decidió así. Esta guía explica **cómo**
se opera y **qué hacer cuando se rompe**. Si pierdes una tarde con algo, agrégalo aquí.

---

## 1. Comandos que se usan a diario

```bash
# Local
docker compose up -d                         # PostgreSQL
mvn clean install                            # compila y corre las 35 pruebas
set -a && source .env && set +a              # carga las variables ANTES de levantar
mvn -pl bff-web spring-boot:run              # un servicio

# Nube (AWS Academy Learner Lab)
make verificar     # qué habilita el lab en esta sesión. Correr primero
make crear         # ECR, cluster, RDS, ALB, API Gateway. Es idempotente
make desplegar     # construye las 4 imágenes, las sube y actualiza el servicio
make iniciar       # enciende la base y levanta las tareas
make apagar        # baja las tareas y detiene la base
make urls          # dónde está todo
```

> **`make apagar` al terminar la jornada, siempre.** El crédito del laboratorio es de **$50 y si se
> agota se pierde el entorno completo**. El ALB no se apaga solo: sigue cobrando ~$0,55 por día.

> **El laboratorio dura 4 horas.** Al renovarlo cambian las credenciales pero los recursos siguen
> ahí. La base vuelve a encenderse sola a los 7 días de detenida.

**Diferencia que importa:** `make desplegar` **sí** construye las imágenes; `./infra/aws.sh desplegar`
**no** —solo registra la task definition y actualiza el servicio—. Si cambiaste código Java y
corriste el segundo, desplegaste la imagen vieja.

---

## 2. Dónde mirar cuando algo falla

Hay **tres** lugares, y conviene recorrerlos en este orden:

### a) El navegador

Herramientas de desarrollo → **Red**, hacer clic en la petición que falló y mirar **el cuerpo de la
respuesta**, no solo el código. El cuerpo dice quién generó el error:

| El cuerpo dice | Lo generó | Qué significa |
|---|---|---|
| `{"type":"about:blank","detail":"..."}` | La aplicación | Todo normal: el `detail` explica el motivo |
| `Invalid CORS request` (texto plano) | Spring, antes de la seguridad | El origen no está en `CORS_ORIGENES` (§4) |
| `{"message":"Forbidden"}` | El API Gateway | El token no pasó el validador del gateway |
| `<Error><Code>AccessDenied` (XML) | S3 | La petición cayó en la ruta por defecto del gateway |

La respuesta trae también la cabecera **`X-Request-Id`**. Anótala: con ella se busca en CloudWatch.

### b) El registro de accesos del API Gateway

```bash
aws logs tail /aws/apigateway/duocconecta --follow
```

Cada línea trae la ruta que coincidió, el estado que devolvió el gateway y el que devolvió el
backend. Si `estado` y `integracion` difieren, el rechazo fue del gateway y no llegó a la
aplicación.

### c) CloudWatch, los cuatro servicios

```bash
aws logs tail /ecs/duocconecta --follow
aws logs tail /ecs/duocconecta --follow --filter-pattern "a3f19c2b"   # un identificador concreto
```

Los cuatro contenedores escriben al **mismo** grupo, cada uno en su corriente. Por eso existe el
identificador de correlación.

---

## 3. Seguir una petición de punta a punta

Cada petición recibe un identificador que aparece **en todas las líneas de log**:

```
INFO [a3f19c2b] --- [bff-web]      ... GET /api/v1/bff/vitrina
INFO [a3f19c2b] --- [ms-proyectos] ... GET /api/v1/proyectos
```

- Sale de la cabecera `X-Request-Id`, o del `X-Amzn-Trace-Id` que pone el ALB, o se genera.
- Se devuelve al navegador en `X-Request-Id`.
- **El BFF lo reenvía** a los microservicios, así que una misma petición queda marcada igual en las
  cuatro corrientes. Buscarlo en CloudWatch devuelve el recorrido completo.

Para probarlo a mano:

```bash
curl -s -o /dev/null -D - -H 'X-Request-Id: mi-prueba' http://localhost:8080/api/v1/bff/vitrina
```

**Qué se registra:** siempre las peticiones que fallan —con el usuario y los permisos que
realmente tenía, que es lo que decide si `@PreAuthorize` deja pasar— y las que tardan más de 1 s
aunque salgan bien. El resto solo en `DEBUG`.

**Subir el detalle sin reconstruir imágenes:**

```bash
LOG_NIVEL=DEBUG make desplegar
```

---

## 4. Las trampas ya pagadas

Cada una costó al menos un ciclo de despliegue. No hace falta volver a pagarlas.

### CORS y la cabecera `Origin` en los POST

**Síntoma:** los GET funcionan, los POST devuelven 403, y **ningún servicio registra nada**.

Que el frontend comparta dominio con la API no quiere decir que CORS no intervenga: en un POST el
navegador manda `Origin` igual, aunque sea mismo origen. Si ese origen no está en `CORS_ORIGENES`,
Spring responde 403 con `Invalid CORS request` **antes** de la cadena de seguridad, así que no pasa
por ningún manejador de excepciones.

`aws.sh desplegar` ya agrega el dominio del API Gateway derivándolo de `API_ID`. **Ante un 403 sin
rastro en los registros, mira el cuerpo de la respuesta antes que nada.**

### El security group con puertos escritos a mano

**Síntoma:** el contenedor arranca perfecto, no hay un solo error en los registros, y el único
signo es un health check en `Target.Timeout`.

El security group abría los puertos con una lista fija (`for p in 8080 8081 8082`). Al sumar
`ms-contacto` en el 8083, el ALB no podía alcanzarlo: los paquetes se descartaban antes de llegar.
Ahora los puertos salen de `SERVICIOS`. **Si un servicio nuevo no responde al health check pero sus
registros están limpios, sospecha del security group antes que de la aplicación.**

### El período de gracia del health check

ECS mata la tarea entera si **cualquiera** de los cuatro target groups la ve enferma al vencer la
gracia. Con tres servicios corriendo Flyway al arrancar, 240 s no alcanzaban y la última en levantar
se llevaba puestas a las otras tres. Está en **420 s**, y el timeout del health check en **15 s**
(con el recolector serie y CPU compartida, una pausa de GC hace que `/actuator/health` tarde más de
los 5 s por defecto).

### Docker caído produciendo imágenes viejas en silencio

Si el demonio de Docker se cae a mitad de `make desplegar`, algunos servicios se reconstruyen y
otros no, **y el despliegue igual termina**. Verifica siempre después:

```bash
aws ecr describe-images --repository-name duocconecta/bff-web \
  --image-ids imageTag=latest --query 'imageDetails[0].imagePushedAt'
```

---

## 5. Qué significa cada código

| Código | Cuándo | En simple |
|---|---|---|
| **400** | La petición no tiene sentido | "Eso que pides no se puede pedir" |
| **401** | Falta el token, venció o no es válido | "No sé quién eres" |
| **403** | Token válido pero sin permiso | "Sé quién eres, y no puedes" |
| **404** | No existe, o su dueño lo ocultó | "Eso no está" |
| **409** | Choca con el estado actual | "Puedes, pero ahora no" |

Un correo de Gmail que se autentica bien en Microsoft recibe **403, no 401**: sabemos quién es, pero
no pertenece a Duoc. Es un problema de permisos, no de identidad.

**Un conflicto de estado nunca se responde con 403.** Decirle "no tienes permiso" a quien sí lo
tiene despista a quien usa la plataforma y a quien la depura.

---

## 6. Deuda conocida

Decidida a conciencia, no olvidada. Si alguien tiene tiempo, este es el orden por valor:

| Qué | Dónde | Por qué se dejó |
|---|---|---|
| **`common-seguridad` sin una sola prueba** | todo el módulo | Es lo más caro de la lista: contiene toda la validación de JWT y no tiene red de seguridad |
| **El frontend sin pruebas** | `frontend-web/` | No hay framework de test declarado en `package.json` |
| **Lombok en dos de cinco módulos** | `ms-proyectos`, `ms-contacto` | Vienen de repositorios aparte. `ms-usuarios` tiene entidad rica escrita a mano; los otros dos, entidades anémicas con `@Setter`/`@Builder` |
| **DTO espejo entre BFF y microservicios** | `bff/dto/` vs `*/dto/` | Mismos campos declarados dos veces. Es el precio de que el BFF no dependa de los microservicios |
| **`exception/` en dos módulos y `service/` en otro** | `ms-usuarios` | `UsuarioNoEncontradoException` vive en `service/` |
| **Un solo CSS global de 458 líneas** | `frontend-web/src/estilos.css` | Con tres pantallas no justifica una librería de componentes |

**Una trampa para el próximo microservicio.** En `common-seguridad/pom.xml`, `springdoc` y
`jakarta.validation-api` están declaradas como **`provided`**, y ese scope **no es transitivo**. Un
módulo nuevo que dependa de `common-seguridad` sin declararlas por su cuenta compila y arranca
igual, pero revienta con `NoClassDefFoundError` la primera vez que alguien mande datos inválidos o
se abra Swagger. Los cuatro módulos de hoy las traen porque copiaron el `pom.xml` de `ms-usuarios`,
no porque el POM lo garantice.

---

## 7. Antes de commitear

- **Nada de secretos.** `.env` está en `.gitignore`; usa `.env.example` como plantilla.
- **`docs/` no se sube**, a propósito: son documentos de trabajo y pesan.
- Todo en **español de Chile**, con trato de **tú**. También los nombres de clases (ver `CLAUDE.md` §5).
- Comentarios **breves**: una o dos líneas. Si necesitas un párrafo, casi siempre falta un nombre mejor.
- `mvn clean install` verde antes de subir.
