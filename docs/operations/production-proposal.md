# Primera publicación: propuesta pendiente de autorización

Actualizado el 30 de octubre de 2026. Presupuesto estricto **0 €**.
Supabase Free está elegido y el propietario ha creado el proyecto Dixit.
No hay que volver a elegir proveedor PostgreSQL. El estado real de conexión y
validación está en [supabase.md](supabase.md); Docker y CI en [docker.md](docker.md)
y [ci.md](ci.md). No existe una URL pública de la aplicación.

## Arquitectura y entorno propuesto

Navegador → una aplicación Spring Boot/React → PostgreSQL Supabase.
Java 21/Boot 3.5.5, REST/servicios/JPA, BCrypt/JWT y STOMP/SockJS simple se conservan.
Una sola instancia jugable; no Supabase Auth/Realtime/Storage/Edge Functions,
Redis, broker externo o escalado horizontal.

Base: Supabase Free/Nano, North EU Stockholm eu-north-1, PostgreSQL 17.11
verificado en el proyecto. App candidata: Render Free, Docker, región europea
próxima disponible (Frankfurt si se confirma), HTTPS onrender.com gratuito,
React servido por el mismo proceso. Render aún no está aprobado ni creado.
La latencia entre regiones y capacidad deben medirse en el entorno autorizado.

Supabase Free ofrece 500 MB de base, CPU compartida/500 MB RAM, 5 GB de egress
y pausa tras una semana inactiva. La referencia de Nano es 60 conexiones de BD
y 200 clientes de pooler, incluyendo consumos internos; confirmar uso/límites
en panel al operar. Hikari máximo 5/mínimo 0 y conexión Flyway separada. No se
cuentan usuarios de Supabase Auth como capacidad de jugadores Dixit. Free no
aporta los backups diarios de Pro; las copias propias son necesarias.
[Precios](https://supabase.com/pricing),
[recursos](https://supabase.com/docs/guides/platform/compute-and-disk),
[conexiones](https://supabase.com/docs/guides/database/connecting-to-postgres).

Render Free: 0,1 CPU/512 MB, 750 horas por workspace/mes; duerme tras
15 minutos sin tráfico entrante (incluidos mensajes WebSocket) y despertar ronda
un minuto. Disco efímero, logs y rollback limitado a dos despliegues anteriores;
puede reiniciarse sin aviso. No hay SSH, jobs puntuales o disco persistente Free.
Admite WebSockets; HTTPS/SockJS y capacidad deben comprobarse en el proxy real.
[Recursos](https://render.com/docs/compute-plans),
[condiciones Free](https://render.com/docs/free),
[WebSockets](https://render.com/docs/websocket).

Para mantener 0 €: sin método de pago ni upgrades; controlar cuotas de builds y
egress en panel. Agotarlas puede suspender servicio o builds. Tráfico externo alto,
incluida la BD externa, también puede causar suspensión; no se autoriza pagar para
levantarla. Sin addon IPv4, dominio o backups de pago. Render Free se propone como
demo ocasional; no acredita disponibilidad continua ni continuidad de partidas.

## Publicación manual propuesta

Elegir un commit concreto de develop con CI real correcta, incluidos imagen y
restore. El propietario conserva main/releases. No activar auto-deploy: Render
documenta modo **Off**, distinto de desplegar automáticamente tras CI.
[Despliegues](https://render.com/docs/deploys).

Antes de crear el servicio deben acordarse recuperación tras caída y operación
sin solapar procesos. Render utiliza reemplazos con cero downtime por defecto;
ese comportamiento no acredita una sola instancia con nuestro broker. Hay que
verificar el procedimiento de suspensión/parada y despliegue/resume del plan
antes de la primera actualización. Si no puede acreditarse la parada del proceso
anterior, no publicar una actualización confiando en el rolling por defecto.

Configurar fuera de Git las variables de [production.md](production.md), CA
legible/montada, roles limitados, JWT estable, PORT y origen HTTPS exacto;
confirmar proxy confiable. No copiar secretos a capas Docker ni frontend.
Mantener Data API desactivada y comprobar permisos del esquema dixit.

El mantenimiento se prepara conforme al contrato de
[demo-maintenance.md](demo-maintenance.md): cerrar nuevas creaciones/inicios,
esperar cero activas, copia protegida, detener único proceso, migraciones
revisadas y arranque cerrado. Tras salud/funcionalidad se reabre expresamente.
El contrato ha sido aprobado; su implementación/evidencia se entrega por
feature/demo-maintenance, no se presupone por existir este documento.

Verificar HTTPS, recursos/rutas React, registro/login, amistades/invitaciones,
partida de cuatro, privacidad, reposición/historial, reconexión/cancelación,
estadísticas/logros y persistencia. Acceso desde otra red/dispositivo cuando
sea posible y validación visual pendiente de Fase 4. La prueba HTTP no la sustituye.
Revisar permisos y atribuciones de código/ilustraciones/fuentes antes de difusión.

Rollback: volver a un artefacto compatible con el esquema; revertir código no
revierte datos ni DDL. Preferir migraciones aditivas. En incompatibilidad,
corrección hacia delante o restauración validada en otra base, sin sobrescribir
la base pública como prueba. Procedimiento de copias/restauración en supabase.md.

## Autorizaciones todavía necesarias

Proveedor/servicio de aplicación gratuito y creación del entorno; política
explícita de activas tras caída; origen/mecanismo definitivo del despliegue;
primera publicación supervisada. Preparación y proyecto de BD no autorizan URL
pública o CD. CD con aprobación manual se valorará después de publicar y verificar
la primera versión, nunca por cada push a develop.

Recorrido de cada unidad: feature/* o fix/* → PR → develop. No cambios en main.
