# Guía breve del propietario

## Abrir y jugar

Demo: https://dixit-demo-20261110-f.onrender.com

Abre el enlace y utiliza **Create account** o **Log in**. No necesitas ejecutar
la aplicación en tu ordenador; Render y Supabase siguen funcionando cuando lo
apagas. Tras un rato sin visitas, el plan gratuito duerme el servidor: espera
varios minutos al volver a abrirlo. No es un servicio con disponibilidad garantizada.

La identidad de tu administrador está en el archivo local protegido
`~/.local/share/dixit-review/owner-admin-reference.txt`. La contraseña la elegiste
tú con entrada oculta y no está guardada por el agente ni en Git. Conserva tu
contraseña por un medio privado. Para recuperarla, pide al programador seguir el
procedimiento con copia reciente de [Supabase](supabase.md); no pegues claves ni
contraseñas en el chat.

## Mantener el coste en cero

Los servicios elegidos son Free. No añadas tarjeta, upgrades, discos, dominios,
planes o addons de pago para resolver una suspensión. Las cuotas son compartidas
con otros servicios de tu cuenta. Si se agotan, puede dejar de funcionar o no
permitir nuevas construcciones. Consulta [los límites de Render](https://render.com/docs/free)
y el panel Billing. Supabase Free también puede pausar proyectos inactivos;
comprueba su panel si la aplicación no consigue arrancar tras una pausa prolongada.

## Actualizar o hacer mantenimiento

**No pulses Manual Deploy, Restart o Rollback sobre la aplicación activa ni
reanudes A, B o la sonda antigua.** El programador debe seguir
[el procedimiento operativo](render-free.md), que comprueba la parada antes de
arrancar el reemplazo. Cada actualización puede cambiar la URL, según lo acordado.

Antes del mantenimiento, abre la URL actual y espera a que termine de despertar.
En tu terminal puedes cerrar nuevas partidas con:

```sh
/home/nico/.local/share/dixit-tools/close-demo-for-update
```

Introduce tu usuario administrador y contraseña cuando se soliciten; la contraseña
queda oculta. Exige `enabled: true` y `activeGameCount: 0`. Si hay partidas activas,
deja que terminen. El programador debe comprobar copia/restauración y parada antes
de sustituir el servicio. Si solo se hizo mantenimiento sin sustituirlo, el helper
`open-demo-after-update` reabre el servicio actual con la misma autenticación.
Los launchers locales apuntan al servicio F; deben actualizarse al cambiar de URL.

Los pushes y la CI de GitHub no publican automáticamente la aplicación. La
publicación sigue siendo manual y supervisada; main y releases son tuyos.

## Copias y fallos

Las copias son manuales: eres responsable de una copia cifrada/restaurada antes
de cambios y en días con actividad real, siguiendo supabase.md. Mantén clave de
cifrado y JWT separados de la copia y conserva una copia protegida independiente
del ordenador; esa custodia externa no se ha verificado. No hay backup gestionado
gratuito asumido ni subida de dumps a GitHub.

Si cae el proceso, las cuentas y el historial están en Supabase. Las partidas
interrumpidas se cancelan según la política aceptada: no se recuperan temporizadores
ni conexiones en vivo. Si falla un reemplazo por protección de instancia única,
el programador debe acreditar el cierre del anterior y reintentar; no desactivar
la protección. Captura únicamente el mensaje de error, sin secretos.

La validación visual completa heredada de Fase 4 y las pruebas desde otro dispositivo
físico/red siguen limitadas. Esta guía no las da por realizadas.
