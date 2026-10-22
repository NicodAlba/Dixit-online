# Registro de desarrollo

## 2026-10-22 — DEV-001 — Versión inicial local

- **Problema y evidencia:** repositorio local en `main`, sin commits, sin remotos y sin identidad Git. `.gitignore` se excluía a sí mismo y excluía los lockfiles; no cubría las dependencias de raíz, `.env`, copias `.bak` ni `.DS_Store`.
- **Solución:** preparar una instantánea del estado recibido antes de corregir código; completar exclusiones y conservar los archivos originales en disco. Configurar únicamente la identidad local facilitada por el usuario: NicodAlba <nicolasdealba05@gmail.com>.
- **Archivos modificados:** `.gitignore` y este registro. El primer commit incorpora el código, recursos, documentación y entregables recibidos.
- **Justificación:** disponer de una base recuperable, manteniendo atribuciones y procedencia académica: https://github.com/gii-is-DP1/dp1-2025-2026-li-02. Se conservan README, desarrolladores y licencia declarados en `pom.xml`, D1/D2/D3 y diagramas.
- **Impacto:** solo versionado y documentación; no cambia el funcionamiento, modelo, reglas, permisos ni contratos. No se añade remoto ni se publica nada.
- **Verificación y resultados:** Java 21 disponible; comprobado el estado real de Git y los archivos candidatos. Existe `frontend/package-lock.json` y se conserva; `package-lock.json` de raíz no existe en el estado recibido (no se genera uno por iniciativa propia). `frontend/.env` y `frontend/package-lock.json.bak` quedan fuera del historial. Se mantienen las credenciales y clave de ejemplo académicas presentes en configuración y fixtures; no son nuevas credenciales privadas ni una configuración apta para publicación.
- **Limitaciones y trabajo pendiente:** la instantánea no acredita que la aplicación funcione. Verificar el fallo funcional elegido en una instancia aislada. Los informes académicos de cobertura son históricos y se conservan, sin atribuirles resultados actuales.
- **Estado final:** instantánea inicial revisada y lista para el primer commit local, antes de modificar código funcional.
