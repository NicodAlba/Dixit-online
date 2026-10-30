# Supabase PostgreSQL: operación de la demo

Decisión aprobada el 30 de octubre de 2026. El propietario creó **Dixit** en
**NicodAlba**, plan **Free/Nano**, región **North EU / Stockholm (eu-north-1)**.
PostgreSQL **17.11** se confirmó por SQL antes de modificarlo. La base postgres
no tenía tablas public ni esquema/roles dixit. El estado y resultados exactos se
registran en DEV-059; no existe todavía publicación de la aplicación.
La etiqueta main/PRODUCTION de Supabase no es una modificación de Git main.

## Acceso y secretos

La conexión directa IPv6 no es accesible desde esta máquina; el endpoint de
**Session pooler / 5432 / IPv4** sí. Obtener host/usuario de Connect del panel;
no inventar el hostname según la región. No usar Transaction pooler / 6543,
no contratar addon IPv4 y no usar claves API en lugar de credenciales SQL.

El usuario del pooler es `dixit_runtime.PROJECT_REF` y el de migración
`dixit_migrator.PROJECT_REF`; el rol SQL `DIXIT_DB_ROLE` es **dixit_runtime**, sin
el sufijo. JDBC no contiene usuario o contraseña en la URL. React sigue usando
REST/STOMP de Dixit; no se ha añadido SDK Auth/Realtime/Storage de Supabase.

`DIXIT_DB_SSL_MODE=verify-full` verifica CA y hostname. La CA pública ofrecida
por Database Settings se descarga fuera de Git y se configura mediante
`DIXIT_DB_SSL_ROOT_CERT`, legible para el proceso. No basta con `require` o
una SSL factory que ignore validación. Runtime y Flyway admiten endpoints/CA
separados con `DIXIT_MIGRATION_DB_URL`, `DIXIT_MIGRATION_DB_SSL_MODE` y
`DIXIT_MIGRATION_DB_SSL_ROOT_CERT`; heredan los valores runtime si se omiten.
La futura red del hosting debe comprobarse antes de elegir su endpoint.

La configuración privada local está en un archivo ignorado `.env.supabase.local`
con permisos 0600. Spring no lo lee automáticamente; el operador utiliza los
scripts que lo cargan como datos, sin ejecutarlo como shell. El archivo de
bootstrap del propietario también está ignorado/0600 y nunca se usa en el
proceso de aplicación. No introducir secretos en chat, argumentos, logs,
frontend, imágenes o Git. JWT es privado, estable y Base64 de al menos 64 bytes;
no se obtiene de Supabase ni se regenera en cada arranque.

Hikari usa máximo 5/mínimo 0, checkout 30 s, connectTimeout pgJDBC 10 s y keepalive.
Se verifican conexiones preparadas/transacciones con la aceptación real. La
prueba de recuperación corta conexiones concretas de la propia cuenta runtime;
no es una prueba de pérdida física de red o continuidad de partidas activas.

## Roles e inicialización

No ejecutar Spring con postgres. Los roles `dixit_migrator` y `dixit_runtime`
son LOGIN separados, sin SUPERUSER/CREATEDB/CREATEROLE/REPLICATION/BYPASSRLS ni
herencia de otros roles. Límites iniciales 2 y 8 conexiones respectivamente;
la app sólo mantiene como máximo 5. El migrador es dueño del esquema dixit,
no de toda la base gestionada. Runtime no tiene CREATE en BD/public/dixit,
ni escritura de catálogos o historial Flyway.

Para un nuevo proyecto vacío autorizado, crear ambos roles desde la cuenta
propietaria, elegir contraseñas fuertes mediante entrada oculta (psql
`\password`), dar CONNECT y conceder CREATE de BD al migrador sólo mientras
crea su esquema dixit. Revocar ese CREATE después. No cambiar la contraseña
propietaria para conectar el backend. No introducir contraseñas claras en el
SQL Editor: la preparación ejecutada utilizó verificadores SCRAM locales,
SQL por stdin y pgpass temporal protegido, sin valores en comandos compartidos.

El esquema se prepara sin USAGE/default grants para PUBLIC, anon, authenticated
o service_role. V1–V3 permanecen intactas: Hibernate validate, nunca create-drop,
update, Flyway clean o baseline automático. V2 añade 36 imágenes, 12 badges y
2 autoridades, no usuarios/partidas. Reiniciar no debe reinsertar esos datos.

**Data API está desactivada**, guardada y comprobada tras recargar el panel.
No hay esquemas consultables por esa vía. Además se comprueban permisos efectivos
sobre esquema/tablas de anon/authenticated/service_role y ausencia de tablas
Dixit en publicaciones Realtime. No se confía sólo en ocultar claves o en RLS;
no se añaden políticas indiscriminadas a las transacciones JPA.

## Validación explícita contra el proyecto de prueba

No ejecutar estos comandos en una base con usuarios reales. Crean cuentas y
partidas ficticias con prefijos security_/restart_, incluidas identidades ADMIN
de prueba con contraseñas aleatorias. No son el administrador del propietario.
No se eliminan silenciosamente después: antes de publicar hay que acordar su
retirada preservando la evidencia/copia. No usar Flyway clean para retirarlas.

Requisitos: Java 21, build React/JAR prod actual y herramientas PostgreSQL 17.
Los scripts requieren configuración privada, referencia explícita de proyecto y
reconocimiento de datos de prueba. Rechazan GitHub Actions/CI antes de leer
configuración. Las PR siguen usando exclusivamente PostgreSQL aislado.

```sh
python3 scripts/operations/supabase_check.py \
  --config /absolute/path/.env.supabase.local \
  --pg-bin /absolute/path/postgresql17/bin \
  --project-ref YOUR_PROJECT_REF --phase migrate --acknowledge-test-data
```

`migrate` exige esquema sin tablas y prueba arranque real/Flyway/Hibernate y
catálogos sin cuentas. Se ejecuta sólo una vez en el destino vacío autorizado.
Las fases siguientes usan el mismo comando cambiando `--phase`:

- `permissions`: versión, V1–V3, catálogos, privilegios efectivos y Realtime.
- `acceptance`: suite explícita de REST/STOMP, privacidad, concurrencia, amistades,
  invitaciones, cuatro participantes/ocho turnos, dos narraciones por actor,
  reposición, autores/votos/historial y estadísticas/logros. Después, reinicios.
- `cancellation`: repetición focalizada de las seis pruebas de reconexión y
  cancelación con **30000 ms**, la política de producción. El fixture de CI
  aislada conserva 2000 ms; esa aceleración puede vencer con roundtrips remotos.
- `restart`: dos arranques del JAR y cuentas/JWT/lobby/historial persistidos;
  arranque adicional con comparación completa de filas de 16 tablas/secuencia,
  sin reinserciones y prueba de recuperación de conexiones propias del pool.

Logs/reportes privados permanecen en target, fuera de Git. Una ejecución con
fallos no se presenta como verde por repetir sólo el caso: DEV-059 diferencia
el primer 34/35 y los 6/6 posteriores con el plazo real. No hacen falta pruebas
de carga masivas en Free. La duración desde esta máquina no acredita capacidad
o latencia del futuro hosting; la aceptación visual continúa pendiente.

## Copia cifrada y restauración aislada

**Responsable: propietario/operador de la demo.** Ejecutar antes de cada cambio
de esquema/app y después de cada sesión de demo. Mantener inicialmente las
últimas siete copias verificadas; no borrar la última válida. No hay tarea
periódica automática ni backup de pago configurado por estos scripts.

```sh
python3 scripts/operations/supabase_restore.py \
  --config /absolute/path/.env.supabase.local \
  --pg-bin /absolute/path/postgresql17/bin \
  --project-ref YOUR_PROJECT_REF \
  --backup-dir /private/owner/backups \
  --key-file /separate/private/location/backup-passphrase.key \
  --acknowledge-test-data
```

El script usa pg_dump custom por TLS y rol migrador, sólo esquema dixit con
ownership/ACL. Cifra mediante GnuPG/AES256 con clave local protegida (0600),
elimina el plaintext inicial, descifra la copia y contrasta SHA256. Restaura en
un PostgreSQL 17 nuevo y aislado en loopback, con roles locales distintos en sus
credenciales; compara todas las filas de las 16 tablas, Flyway y secuencia.
Arranca el JAR prod contra la copia con JWT estable y pasa sus comprobaciones
de reinicio. Detiene/elimina únicamente su clúster temporal. Nunca restaura
sobre Supabase. El archivo cifrado y metadatos de verificación quedan fuera de
Git y de artefactos públicos, en directorio 0700.

En esta preparación el destino es ~/.local/share/dixit-backups/supabase y la
clave está separada en ~/.config/dixit/backup-passphrase.key. La clave JWT está
en la configuración privada, no en el dump. El propietario debe conservar copia
segura de ambas claves y del archivo cifrado fuera del disco/proveedor original
(por ejemplo, soporte ya disponible), sin contratar almacenamiento. Sin esa
custodia independiente, perder el equipo puede perder backup y claves juntos.

Un dump no revierte de forma segura cualquier versión de app: validar primero
compatibilidad de esquema, cuenta/historial y rollback. No sobrescribir una base
pública para ensayar. La ejecución real y sus límites se acreditan en DEV-059;
escribir este procedimiento por sí solo no configura copias periódicas.

## Límites gratuitos y publicación

Consulta oficial 30 de octubre de 2026: Free $0, 500 MB de BD, CPU compartida/
500 MB RAM, 5 GB egress, dos proyectos activos y pausa tras una semana inactiva.
No utilizar cuotas de Auth/Storage como capacidad de Dixit: no usamos esos
servicios. Nano tiene referencia de 60 conexiones de BD/200 de pooler; reservar
margen para el propio proveedor y revisar uso real. No activar addons o Pro.
[Precios](https://supabase.com/pricing),
[conexiones](https://supabase.com/docs/guides/database/connecting-to-postgres),
[recursos](https://supabase.com/docs/guides/platform/compute-and-disk),
[Data API](https://supabase.com/docs/guides/api/securing-your-api).

Spring, presencia y plazos siguen en una instancia. Persistir no recupera sockets,
temporizadores o partidas activas automáticamente. Hosting de aplicación,
política tras caída, primera publicación, HTTPS/proxy/WebSockets públicos,
retirada de fixtures, bootstrap del administrador propietario y validación
visual/red real necesitan sus pasos/autorizaciones antes de difusión.

Recorrido: feature/supabase-postgresql → PR → develop; main es del propietario.
