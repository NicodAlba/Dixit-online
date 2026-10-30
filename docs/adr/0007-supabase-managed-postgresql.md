# ADR-0007 — Supabase Free como PostgreSQL gestionado

- Fecha: 2026-10-30.
- Estado: elección aprobada expresamente; proyecto creado por el propietario.
- Base: ADR-0006 y DEV-059. No autoriza crear app hosting ni publicación pública.

## Decisión

Supabase se usa exclusivamente para PostgreSQL de Dixit. Se conserva la aplicación
única Spring+React, usuarios y permisos Spring/JWT/BCrypt, REST y STOMP/SockJS.
No se utiliza Auth, Realtime, Storage o Edge Functions de Supabase ni acceso
directo desde React. Presupuesto 0 €, sin upgrades/addons.

El proyecto Dixit es Free/Nano, región Stockholm, PostgreSQL 17.11 confirmado
antes de migrar. No se cambia el major aprobado ni V1–V3. Esquema dixit, rol
migrador propietario de ese esquema y rol runtime limitado; ninguno requiere
ser postgres/superusuario o crear otros esquemas/bases. Runtime conserva
lecturas de catálogos y DML/secuencia existentes, sin escritura de Flyway/DDL.

Desarrollo no tiene conectividad al endpoint directo IPv6; se usa el pooler
compartido de sesión IPv4/5432 con TLS verify-full y CA del proveedor. El nombre
de conexión incluye la referencia del proyecto; el rol SQL de V3 sigue siendo
un identificador simple distinto del nombre de conexión. Conexión/CA de migración
pueden separarse de runtime; una futura red IPv6 podrá usar endpoint directo
previa comprobación. Se bloquea transaction pooling 6543 no verificado.

Data API desactivada y ajuste persistido comprobado en panel; roles API sin
USAGE/tablas Dixit, sin publicaciones Realtime. No se confía en ocultar claves
ni se añade RLS indiscriminado que altere las transacciones JDBC.

## Consecuencias

CI ordinaria sigue aislada y nunca recibe secretos Supabase. La aceptación
gestionada se ejecuta explícitamente desde la máquina del operador contra el
proyecto de prueba, con datos ficticios, configuración privada y sin publicar
servicio alguno. Evidencia, fallos y repeticiones focalizadas en DEV-059.

Free puede pausarse y carece de las copias de un plan de pago. El operador usa
pg_dump verificado/cifrado fuera de Git y pg_restore en PostgreSQL 17 aislado,
con roles/ACL, huellas de todas las tablas, secuencia y arranque del JAR.
No se sobrescribe Supabase ni se promete continuidad o recuperación de activas.
Responsabilidad/frecuencia en docs/operations/supabase.md; comprobar ejecución
real antes de declarar copias operativas.

Esta decisión sustituye únicamente el estado «proveedor candidato» de ADR-0006.
Render sigue siendo candidato para la aplicación. Main/releases, política tras
caída y primera publicación requieren decisión del propietario.

Recorrido: feature/supabase-postgresql → PR → develop.
