# Instrucciones de trabajo de DIXIT

## Registro obligatorio de cada intervención

Por petición expresa del propietario, antes de cerrar cada intervención:

1. Actualiza `docs/CHANGELOG_DEV.md` con fecha e identificador, problema y evidencia, solución, archivos modificados, justificación, impacto, verificaciones y resultados, limitaciones, pendientes y estado final.
2. Actualiza también el documento externo `../# DIXIT — Software Architecture & SDD Sp.md` (en el entorno del propietario: `/home/nico/Desktop/AMUERTE/1dixit/# DIXIT — Software Architecture & SDD Sp.md`). Debe contener las explicaciones de todo lo realizado, incluidos Git, pruebas, commits y publicación. No basta con el mensaje del chat. Conserva el contenido existente y añade o actualiza las entradas correspondientes.
3. El documento externo es local: no lo copies al repositorio ni lo publiques. Si no existe o no está accesible en otro entorno, informa de la limitación y conserva el registro versionado; no afirmes haberlo actualizado.

## Historial público y copias locales

- Las ramas `local/received-baseline` y `local/prepublication-history` conservan historia privada con contratos universitarios: no las publiques.
- Publica solo ramas revisadas mediante nombres explícitos. No uses push de todas las ramas ni mirror.
- Conserva las exclusiones de contratos, `.env`, credenciales privadas, dependencias, builds, logs y copias temporales. Mantén las atribuciones y entregables técnicos académicos.
