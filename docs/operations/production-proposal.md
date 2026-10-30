# Propuesta operativa pendiente de aprobación

Consulta oficial: **30 de octubre de 2026**. Presupuesto fijado por el propietario:
**0 €**, sin planes de pago. Este documento propone la siguiente entrega; no
acredita proveedores o despliegues ya implementados. El propietario aprobó
la preparación local de aplicación única + PostgreSQL 17/Flyway; su configuración
y comprobaciones se describen en [production.md](production.md). La CI se describe en [ci.md](ci.md).

## Diagnóstico operativo recibido y preparación realizada

Al comenzar esta fase, la versión usaba H2 create-drop, clave JWT efímera si
faltaba configuración y `data.sql` con cuentas académicas. La preparación local
aprobada ya añade perfil prod estricto, Flyway, PostgreSQL, rutas React, REST
relativo, orígenes y salud mínima; las verificaciones y procedimientos están en
[production.md](production.md). Broker, presencia y plazos siguen siendo de
proceso. Quedan pendientes la política aprobada de reinicio, el proveedor y la
validación de HTTPS/proxy y transporte en un entorno público.

## Empaquetado recomendado

**A: una instancia Spring Boot sirve React, con una base de datos separada.**
Conserva Java 21/Spring Boot 3.5.5, REST/servicios/JPA, JWT/BCrypt y STOMP/SockJS.
Una URL HTTPS evita configurar dos orígenes y reduce despliegues coordinados.
La alternativa B (React estático en CDN y Spring separado) puede servir Home
mientras Spring duerme, pero requiere dos entregas, origen backend configurable,
CORS y coordinación de versiones; no evita el arranque en frío del juego.

Se propone Docker multietapa para Render porque Java no tiene runtime nativo
allí, no por necesidad arquitectónica: build Node + Maven y runtime Java 21 sin
npm/Maven/secretos, usuario sin privilegios y `.dockerignore` estricto. La CI
actual verifica el JAR; cuando exista Dockerfile deberá comprobar también la
imagen construida antes de desplegarla. No se publicará en un registro por defecto.
[Runtime oficial](https://render.com/docs/native-runtimes),
[Docker](https://render.com/docs/docker).

Preparación local implementada: rutas GET/HEAD React explícitas hacia index.html, sin
capturar `/api`, `/ws`, errores ni archivos inexistentes; REST relativo al origen,
SockJS en `/ws`; puerto `PORT`, escucha adecuada al contenedor, cabeceras reenviadas
solo del proxy confiable y HTTPS público. Origen permitido explícito, health
mínimo sin detalles/credenciales y endpoints de gestión restantes privados.
Perfiles dev/test separados de prod; prod exige JDBC PostgreSQL, usuario/clave y
DIXIT_JWT_SECRET Base64 privado de al menos 64 bytes, validando al arrancar sin
imprimir valores. Sin H2 Console, create-drop, SQL detallado o semillas de usuarios.
Terminación ordenada con plazo acotado; no implica continuidad de partidas.

## Dos configuraciones de 0 €

| Aspecto | A. Render Free + Neon Free (recomendación) | B. Render Free + Supabase Free |
| --- | --- | --- |
| Spring y React | Un contenedor/URL HTTPS en Render | Igual |
| Datos | PostgreSQL gestionado Neon | PostgreSQL gestionado Supabase; Spring sigue siendo backend |
| Región propuesta | Frankfurt para app y BD | Frankfurt para app y región Central EU para BD |
| App: RAM/CPU | 512 MB / 0,1 CPU; falta medir carga Java real | Igual |
| Suspensión | App tras 15 min sin tráfico; arranque aproximado de un minuto. BD tras 5 min inactiva | Misma app; BD puede pausarse tras una semana con poca actividad |
| Datos y cuotas BD | Precio consultado: 1 GB/proyecto, 100 CU-h/mes, hasta 2 CU/8 GB; 5 GB de transferencia | 500 MB, CPU compartida/500 MB RAM, 5 GB de salida |
| WebSocket/SockJS | Render admite WS; verificar además fallback HTTP SockJS a través del proxy | Igual |
| Persistencia | Fuera del disco efímero de la app | Igual |
| Copias | Recuperación Neon corta (hasta 6 h o 1 GB de cambios); requiere dumps externos propios | Free sin backups automáticos; requiere dumps externos propios |
| Logs | Render stream; Neon retiene métricas/logs 1 día; sin archivo duradero propio | Render stream y límites del panel Supabase; verificar retención efectiva en cuenta |
| Rollback | Render conserva dos despliegues anteriores; la BD exige estrategia independiente | Igual |
| Coste/extras | 0 en Free; cuotas agotadas pueden suspender servicio. Sin tarjeta, upgrades ni extras | Igual; no contratar IPv4 ni backups de pago |
| Mantenimiento | Dos servicios y pruebas de recuperación; la app puede reiniciarse sin aviso | Añade revisión de Data API, roles y posible reactivación manual de BD |

Datos de planes: [Render Free](https://render.com/docs/free),
[recursos Render](https://render.com/articles/from-side-project-to-production-scaling-your-first-app),
[Neon pricing](https://neon.com/pricing), [Supabase pricing](https://supabase.com/pricing).
La página actual de precios Neon indica **1 GB**; documentación indexada anterior
muestra 0,5 GB. Confirmar cuota real en el panel antes de crear el proyecto.
[Regiones Render](https://render.com/docs/regions),
[Neon](https://neon.com/docs/introduction/regions),
[Supabase](https://supabase.com/docs/guides/platform/regions).
No se ha medido latencia desde España ni capacidad de jugadores simultáneos.

Render ofrece 750 horas gratis/mes por workspace; comparte cuotas de tráfico y
build. Sin método de pago el agotamiento suspende servicios/builds en lugar de
facturar. No se usarán pings artificiales para evitar suspensión. Render puede
reiniciar Free y lo presenta para demos, no producción fiable. Tampoco se propone
su PostgreSQL Free como almacén estable: expira a los 30 días y carece de backups.
No se promete disponibilidad continua a coste cero.

Railway + una BD gratuita externa también conserva el monolito, pero su prueba
es de 5 USD/30 días y luego Free concede 1 USD/mes de consumo. No acredita cubrir
esta JVM de forma sostenida, por lo que no se recomienda para este requisito.
[Condiciones oficiales](https://docs.railway.com/pricing/free-trial).

## Persistencia aprobada para preparación local; proveedor pendiente

Motor aprobado para preparación local: **PostgreSQL 17**, último parche disponible al ejecutar pruebas,
mismo major local y gestionado. Sigue soportado hasta noviembre de 2029 y Neon
lo ofrece en Free. El major real ofrecido por Supabase se debe comprobar antes
de elegirlo. [Soporte PostgreSQL](https://www.postgresql.org/support/versioning/),
[PostgreSQL 17 en Neon](https://neon.com/blog/postgres-17).
H2 sigue siendo útil para pruebas rápidas; no acredita concurrencia o dialecto
PostgreSQL. MySQL también sería viable con trabajo de adaptación y un hosting
aprobado, pero no aporta ventaja para la propuesta gratuita de PostgreSQL.

Driver `org.postgresql:postgresql` 42.7.14 y Flyway OSS 11.7.2 (`flyway-core` y
`flyway-database-postgresql`), compatibles con Boot 3.5.5. El parche de pgJDBC
se fija explícitamente por seguridad; Flyway usa la versión gestionada. [Módulo oficial](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database).

Alcance aprobado y realizado mediante V1–V3 y pruebas PostgreSQL; el bootstrap
de administrador sigue pendiente, como precisa [production.md](production.md):

1. V1: esquema `dixit` dedicado con entidades actuales, tablas de unión, claves,
   índices, restricciones y `entity_sequence` con incremento compatible con JPA.
   Incluir semilla privada de orden de turno y cancelación; no cambiar User/Player,
   apariciones Card/Image, votos, autores o historia. Revisar DDL explícito frente
   a mappings; `ddl-auto=validate`, sin update automático ni baseline silencioso.
2. V2: catálogo de 36 imágenes, authorities y badges necesarios, con IDs/URLs
   estables y secuencia posterior segura. Separar `data.sql` académico en dev/test;
   producción sin cuentas, partidas, logros concedidos ni contraseñas de muestra.
   Versiones ya aplicadas no se editan ni se reinsertan catálogos al reiniciar.
3. Pruebas contra PostgreSQL local aislado: arranque vacío, reinicio sin pérdida,
   constraints/secuencias, login/registro, amistades, invitaciones, juego completo,
   estadísticas y logros. Revisar TIMESTAMPDIFF/LIMIT en HQL y agregaciones con el
   dialecto real. Probar bloqueo pesimista de Game/invitación y User ordenados,
   voto/avance simultáneos, transacciones y eventos después de commit.
4. Rol de migración con DDL y rol runtime mínimo sin DDL. JDBC TLS verificando
   certificado/hostname; Hikari pequeño (propuesta inicial máximo 5, mínimo 0),
   timeouts compatibles con reactivación y medición de conexiones reales.
5. Crear administrador mediante comando local de una sola ejecución y contraseña
   introducida de forma oculta, BCrypt y transacción; jamás contraseña inventada,
   argumento de shell, log, endpoint público o usuario de demo.

Supabase sería solo PostgreSQL: React usa Dixit; sin Auth/Realtime/Storage. Para
JVM persistente, conexión directa si la red admite IPv6; si no, pooler **session**
IPv4 sin addon de pago, validando Flyway/JPA y límites. Esquema dixit fuera de los
expuestos, Data API desactivada si no se necesita y privilegios anon/authenticated
revocados; comprobar también APIs adicionales. No confiar solo en ocultar claves.
[Conexiones](https://supabase.com/docs/guides/database/connecting-to-postgres),
[Data API](https://supabase.com/docs/guides/api/securing-your-api).

## Reinicios, operaciones y autorización pendiente

Recomendación inicial para demo: mantenimiento controlado, sin iniciar nuevas
partidas antes de actualizar y esperando que terminen las activas. Requiere
preparar y probar ese mecanismo. Una caída inesperada sigue pudiendo interrumpir
una partida: no se promete reconstruir timers/presencia/sockets ni se cancelan
filas históricas al arrancar. Política de activas tras caída por acordar antes
de publicar. Alternativas: recuperación explícita supervisada, o proyecto adicional
de recuperación automática con plazos persistidos/reconciliación y pruebas. No
se introduce Redis, broker externo ni escalado horizontal; una sola instancia.
Aunque el proveedor ofrezca despliegue sin cortes, no se permitirá solapar dos
instancias jugables con presencia/broker independientes durante la actualización.

Procedimiento propuesto, todavía no ejecutado:

- Arranque: migraciones controladas, prod validado, un proceso y salud sin datos.
- Actualización: ventana acordada, sin activas, copia, CI verde del commit aprobado,
  migración revisada y publicación manual supervisada; comprobar HTTPS y WS/SockJS.
- Rollback: artefacto/commit previo compatible con esquema. Preferir cambios
  aditivos; volver al código anterior no deshace datos ni migraciones destructivas.
- Backup: `pg_dump` formato custom con TLS, copia cifrada fuera del proveedor/Git,
  antes de cada cambio y después de cada sesión de demo; retención inicial 7 copias.
  Credenciales por entorno/archivo protegido, nunca línea de comandos o logs.
- Restauración: `pg_restore` en BD aislada del mismo major, verificar cuentas,
  historia, autores/votos, constraints/secuencias y app; medir tiempos y pérdida
  máxima real. No sobrescribir la BD pública como prueba. Dump no contiene la
  clave JWT: conservarla separadamente en gestor seguro.

Origen de despliegue propuesto: commit de develop expresamente aprobado, despliegue
manual sin auto-deploy por push. CD posterior solo con aprobación manual, permisos
mínimos y mecanismo acordado. El propietario controla main/releases. No crear
proyectos, configurar cuentas/secretos externos ni publicar hasta autorización.

Antes de difusión: validación funcional pública de cuatro participantes, privacidad,
reconexión/cancelación, persistencia/restauración y comportamiento de activas;
validación visual pendiente de Fase 4 y revisión de permisos/atribuciones de recursos.

Aprobado: empaquetado A + PostgreSQL 17/Flyway para preparación local.
Pendiente de aprobar separadamente: (1) proveedor y límites de demo gratuita,
(2) política de partidas activas, (3) creación de entorno y primera publicación.
Aprobar preparación no autoriza publicar. Cada entrega sigue `feature/*` o `fix/*` → PR → `develop`.
