# Instrucciones de trabajo de DIXIT

## Registro obligatorio de cada intervención

Por petición expresa del propietario (unificación de DEV-006), antes de cerrar cada intervención actualiza **solo `docs/CHANGELOG_DEV.md`**. Este fichero reúne el historial de desarrollo y el razonamiento arquitectónico/técnico.

Registra fecha e identificador, problema y evidencia, comportamiento esperado, causa técnica, solución y por qué, alternativas relevantes, archivos, impacto sobre reglas/datos/API/frontend/módulos, verificaciones y resultados reales, riesgos, límites, pendientes, estado final y rama/commits/PR cuando existan. Una entrada breve basta para cambios triviales. No basta con enumerar archivos ni con el mensaje del chat.

El antiguo documento externo `../# DIXIT — Software Architecture & SDD Sp.md` se ha fusionado con el changelog y retirado por indicación del propietario. No lo recrees ni mantengas un registro paralelo. Conserva las entradas históricas y distingue verificaciones ejecutadas de inspecciones e hipótesis. Las decisiones arquitectónicas importantes aprobadas se documentan además en `docs/adr/` cuando corresponda.

## Alcance e integración

- Trabaja sobre un problema cada vez y conserva arquitectura, modelo y reglas salvo aprobación expresa.
- Todo trabajo, incluidos documentación y configuración, se realiza en una rama propia `fix/*` o `feature/*` creada desde `develop`, con el nombre acordado. No hagas cambios ni commits directamente en `develop` o `main`.
- La entrega de cada tarea termina mediante PR de su rama `fix/*` o `feature/*` hacia `develop`, conservando los commits existentes y verificando la subida e integración. Publica únicamente la rama explícita de la tarea en el repositorio propio autorizado.
- Indica SIEMPRE este recorrido en los prompts de traspaso, propuestas y cierres: `fix/*` o `feature/*` → PR → `develop`. Identifica la rama concreta cuando se conozca y distingue los pasos pendientes de los completados.
- El paso de `develop` a `main` lo realiza exclusivamente el propietario. Los agentes no deben hacer merge, commits ni push a `main`, ni crear PR hacia `main` por iniciativa propia.
- No publiques versiones o despliegues sin aprobación. El permiso de push e integración en `develop` no autoriza esas acciones ni el paso a `main`.

## Historial público y copias locales

- Las ramas `local/received-baseline` y `local/prepublication-history` conservan historia privada con contratos universitarios: no las publiques.
- Publica solo ramas revisadas mediante nombres explícitos. No uses push de todas las ramas ni mirror.
- Conserva las exclusiones de contratos, `.env`, credenciales privadas, dependencias, builds, logs y copias temporales. Mantén las atribuciones y entregables técnicos académicos.
