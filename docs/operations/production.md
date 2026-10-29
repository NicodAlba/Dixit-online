# Preparación local de producción

Estado: aplicación única + PostgreSQL 17/Flyway aprobados para preparación local.
No hay proveedor ni publicación aprobados. Presupuesto máximo 0 €; propuestas y
límites en [production-proposal.md](production-proposal.md). CI sin CD automático.

## Configuración

Java 21, Node de frontend/.nvmrc, Maven wrapper y PostgreSQL 17 (mismo major en
pruebas y futuro proveedor). Usar el último parche soportado; evidencia inicial
con 17.11. Flyway 11.7.2 administrado por Boot 3.5.5. pgJDBC se fija en 42.7.14, parche
de seguridad de la dependencia recién introducida; se evita el 42.7.7 heredado del
BOM sin actualizar Spring Boot. [Aviso oficial](https://jdbc.postgresql.org/security/).

`.env.prod.example` enumera placeholders. Spring **no** carga ese archivo: el
operador debe inyectar valores mediante entorno/gestor seguro, sin guardarlos
en Git, builds, frontend o logs. No generar la clave JWT en cada arranque.

- `DIXIT_DB_URL`: JDBC PostgreSQL sin credenciales en URL.
- `DIXIT_DB_USERNAME`, `DIXIT_DB_PASSWORD`: conexión de aplicación.
- `DIXIT_DB_ROLE`: identificador simple del rol real; normalmente igual al usuario,
  pero un pooler puede requerir un nombre de conexión distinto.
- `DIXIT_MIGRATION_DB_USERNAME`, `DIXIT_MIGRATION_DB_PASSWORD`: rol DDL distinto.
- `DIXIT_JWT_SECRET`: Base64 de al menos 64 bytes aleatorios privados, estable entre
  reinicios. Guardar copia segura por separado de los dumps de datos.
- `DIXIT_ALLOWED_ORIGINS`: orígenes HTTPS exactos separados por comas, sin slash
  final ni wildcard. HTTP loopback únicamente para pruebas aisladas.
- `DIXIT_DB_SSL_MODE`: por defecto `verify-full`; certificado/hostname válidos.
  La CI y la prueba PostgreSQL local usan `disable` solo en su loopback aislado.
- `DIXIT_TRUSTED_PROXY_REGEX`: direcciones del proxy autorizado confirmadas con el
  proveedor. Tomcat native procesa cabeceras de proxies confiables; por defecto
  solo loopback. No usar una expresión que confíe en cualquier cliente.
- `PORT`: puerto del proceso, predeterminado 8080.

Crear previamente dos roles LOGIN privados. El rol migrador necesita crear/ser
propietario del esquema dixit en la base elegida; runtime no debe ser superusuario,
propietario del esquema/base o tener CREATE. Revocar privilegios heredados que
lo permitan. Ambos necesitan conexión a la base; Flyway otorga al runtime solo
lecturas de catálogos, DML de tablas de aplicación y uso de la secuencia.
El nombre `dixit` es un esquema dedicado; en Supabase no exponerlo a Data API y
revocar permisos anon/authenticated. Confirmar conexión/pool/SSL sin addon de pago.
No usar una conexión de producción para ejecutar los scripts de CI.

## Compilar y arrancar

Primero ejecutar las verificaciones de [ci.md](ci.md). Desde la raíz:

```sh
./mvnw --batch-mode --no-transfer-progress -DskipTests -Pproduction-package package
java -jar target/LI-02-2425-dixit-0.1.0.BUILD-SNAPSHOT.jar --spring.profiles.active=prod
```

Requiere el build React previo. El perfil Maven excluye `data.sql` y el perfil
MySQL académico del JAR; no cambia las pruebas de desarrollo. El perfil Spring
prod exige configuración válida antes de crear beans de datos/autenticación,
aplica Flyway V1–V3, valida JPA y no ejecuta semillas académicas. El arranque no
inserta usuarios, partidas ni logros concedidos. El paquete de producción selecciona prod por defecto, incluso sin argumento;
no sobrescribirlo con perfiles de desarrollo. El paquete normal usa dev;
CI activa test mediante una configuración que solo existe en src/test/resources. Una migración fallida impide arrancar; no activar baseline-on-migrate,
clean o ddl-auto=update para sortearla.

Spring sirve únicamente las rutas React enumeradas en SpaConfiguration; APIs,
SockJS, gestión y recursos inexistentes conservan su tratamiento propio. Las
vistas UI públicas contienen código estático; la autorización de datos/acciones
permanece en REST/STOMP. `/actuator/health` devuelve solo estado; consola H2 y
otros endpoints de gestión no son públicos. OpenAPI queda desactivado en prod.

Detener mediante SIGTERM. Spring permite hasta 20 s para peticiones pendientes;
no equivale a esperar una partida completa o restaurar sesiones. No se cancelan
filas al arrancar ni se reconstruyen automáticamente los plazos del anfitrión.

## Pruebas PostgreSQL y artefacto

CI crea un servicio PostgreSQL 17.11 desechable con roles aleatorios, ejecuta las
migraciones y repite pruebas REST/STOMP, concurrencia y una partida de cuatro
participantes/ocho turnos. `PostgresPersistenceIT` comprueba 52 apariciones,
32 cartas históricas con autor, 24 votos, 20 cartas en mano y consultas de
estadísticas/logros. También comprueba privilegios, catálogo y FK reales.

Para repetir localmente, preparar una base **exclusivamente desechable** y cargar
las variables anteriores sin imprimir valores, más
`DIXIT_TEST_POSTGRES_URL` igual a `DIXIT_DB_URL`. Entonces:

```sh
./mvnw --batch-mode --no-transfer-progress -Dspring.profiles.active=prod \
  -Dtest.reports.dir=target/postgres-surefire-reports \
  -Dtest=PostgresPersistenceIT,GameActionPermissionsTest,GamePhasePrivacyTest,FriendRelationshipsTest,GameCancellationTest,ChatAccountIdentityTest,StompPermissionsTest test
./mvnw --batch-mode --no-transfer-progress -DskipTests -Pproduction-package package
python3 scripts/ci/verify_production.py
```

El script verifica ausencia de semillas en el JAR, fallo sin clave, salud mínima,
rutas, exclusión de APIs del fallback, orígenes SockJS y dos arranques del proceso
con persistencia de cuenta, JWT, lobby e historial completado. Los tokens de
fixtures permanecen solo en `target`, fuera de Git y de artefactos publicados.
No verifica continuidad de una partida activa, navegador, proxy externo o red móvil.

## Actualización, rollback y copia

Antes de publicar: acordar ventana sin partidas activas y mecanismo para impedir
nuevas; ese mantenimiento y la recuperación tras caída aún no están implementados.
No actualizar con partidas activas ni activar escalado/solapamiento de instancias.
Despliegue manual de commit aprobado, con CI verde, revisión de migraciones y
copia previa. El mecanismo del proveedor y el origen definitivo requieren aprobación.

Preferir migraciones aditivas y commits de app compatibles. Rollback de aplicación
no revierte datos ni una migración destructiva. Ante incompatibilidad, detener,
valorar corrección hacia delante o restaurar una copia en otra base y validar
antes de redirigir tráfico. No ejecutar Flyway clean ni borrar historia para arrancar.

Plan de backup gratuito: pg_dump formato custom por TLS antes de cambios y después
de cada sesión de demo, copia cifrada fuera de Git/proveedor y retención inicial
7 copias. Credenciales por entorno protegido/pgpass con permisos restrictivos,
nunca en argumentos o historial del shell. pg_restore se prueba en otra base con
el mismo major y roles, comprobando todas las tablas, secuencia, cuentas/historia
y arranque de la app. La CI hace esa prueba con sus datos sintéticos mediante
`verify_restore.py`; no configura copias periódicas del futuro servicio público.
El plan gratuito elegido podría no ofrecer backups automáticos ni recuperación
larga. Acordar RPO/RTO reales con el propietario antes de admitir datos reales.

La clave estable conserva sesiones JWT entre reinicios; el cambio de contraseña
sigue sin revocación global inmediata. Rotar la clave invalida todos los tokens.

No hay administrador predeterminado. Procedimiento propuesto antes de necesitar
el primero: herramienta local de una sola ejecución con conexión autorizada,
contraseña elegida por el propietario con entrada oculta y confirmada, BCrypt,
transacción y bloqueo de authority ADMIN; crear solo si no existe administrador,
obteniendo ID de la secuencia. No SQL con contraseña plana, endpoint público o
contraseña inventada. Esa herramienta de bootstrap queda pendiente de implementar
antes de crear el primer administrador; no es necesaria para registrar jugadores.

Faltan elección/aprobación de proveedor, creación del entorno, HTTPS/proxy real,
posible Dockerfile y prueba de imagen, política de activas, bootstrap admin,
backups operativos, primer despliegue supervisado y validación pública/visual.
Recorrido de esta entrega: `feature/production-foundation` → PR → `develop`.
Main/releases corresponden al propietario; no hay URL pública autorizada.
