# ADR-0006 — Aplicación única y PostgreSQL versionado

- Fecha: 2026-10-30.
- Estado: aprobado expresamente por el propietario para preparación y pruebas
  locales de una aplicación Spring+React con PostgreSQL 17 y Flyway. Hosting,
  recursos externos, publicación y política de recuperación siguen pendientes.
- Contexto/evidencia: DEV-055/056 y propuesta en docs/operations/production-proposal.md.

## Decisión

Mantener una instancia Java 21/Spring Boot 3.5.5 que sirve el build React 18 en
su JAR, con una base PostgreSQL separada. REST sigue ejecutando acciones y vistas
por actor; STOMP/SockJS con broker simple invalida estados. JWT/BCrypt, servicios,
JPA y las cinco ADR anteriores se conservan. No se añaden Supabase Auth/Realtime,
Redis, microservicios o escalado horizontal.

PostgreSQL 17 usa pgJDBC y Flyway OSS, Flyway gestionado por Boot 3.5.5 y override puntual pgJDBC 42.7.14 por los
avisos de seguridad oficiales vigentes, sin actualización general de dependencias.
Migraciones inmutables: esquema inicial acorde a JPA, catálogos sin cuentas y
privilegios del rol runtime. Hibernate valida; no crea o actualiza el esquema de
producción. H2 y data.sql académicos se conservan para desarrollo/pruebas y no
son evidencia suficiente de compatibilidad PostgreSQL.

El perfil prod valida configuración esencial antes de crear datasources/JWT:
clave privada estable, PostgreSQL, credenciales separadas de migración/runtime,
orígenes explícitos y prohibición de create-drop/semillas/consola/SQL detallado.
El paquete de producción excluye data.sql y el perfil MySQL académico.

## Alternativas y consecuencias

Frontend estático separado aporta CDN y disponibilidad de Home mientras duerme
Spring, a cambio de dos entregas/orígenes y coordinación adicional. Una aplicación
reduce esa operación para la primera demo. H2 persistente o reemplazar únicamente
la URL JDBC no acreditan dialecto, restricciones, secuencias o bloqueos.

Flyway mantiene el esquema dixit; el rol de aplicación no tiene DDL, escritura de
catálogos ni acceso a la tabla de control. El arranque usa además credenciales de
migración separadas: la revisión y autorización del cambio de esquema forman
parte de cada publicación supervisada; estas credenciales no se incorporan al JAR.
Aislar también el proceso de migración será una decisión operativa posterior si
el proveedor autorizado lo permite o exige.

Persistir partidas no reconstruye timers/presencia/WebSockets. No se modifican
partidas históricas durante el arranque. No se promete recuperación automática ni
continuidad durante reinicio, caída, actualización o sustitución de instancia.
La política de activas debe acordarse antes del despliegue; no se solapan instancias
jugables independientes bajo un balanceador.

El presupuesto aprobado es 0 €. Render/Neon/Supabase son candidatos comparados,
no proveedores elegidos; sus cuotas/suspensión impiden prometer un servicio siempre
disponible. Docker queda propuesto si se aprueba Render, cuyo runtime no incluye
Java; no es requisito de esta decisión ni está implementado por esta ADR.

Recorrido: **feature/production-foundation → PR → develop**. Main, releases y
publicación permanecen bajo control del propietario. La deuda visual DEV-053/054
continúa explícita; pruebas HTTP y de base de datos no la sustituyen.

Actualización de proveedor aprobada el 30 de octubre de 2026: ADR-0007 elige
Supabase Free. La comparación histórica anterior no implica mantener abierta
esa elección; hosting de aplicación/publicación siguen pendientes.
