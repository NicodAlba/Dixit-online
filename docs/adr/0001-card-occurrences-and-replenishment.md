# ADR-0001 — Apariciones de cartas y reposición con historial

- Fecha: 2026-10-22.
- Estado: aceptado expresamente por el propietario al aprobar 7.C.
- Contexto y evidencia: DEV-012 y ejecución DEV-013 en `../CHANGELOG_DEV.md`.

## Decisión

`Image` identifica una ilustración; `Card` identifica una aparición de esa ilustración en una partida. Una aparición jugada conserva su identidad, turno, imagen, autor y votos y no vuelve a repartirse. Si se reutiliza una imagen, se crea otra `Card`, sin turno, autor o votos heredados.

La prioridad acordada es repartir aleatoriamente entre imágenes nunca repartidas; si no bastan, utilizar las restantes y completar con imágenes de apariciones históricas finalizadas. Se excluyen las imágenes de todas las manos y mesas activas de la partida, así como las ya reservadas por esa reposición. El descarte recién terminado es elegible inmediatamente, incluso para quien acaba de jugarlo. Esta prioridad es una precisión expresa del propietario respecto a R7; no se presupone a partir de su redacción original.

`Game.deck` recoge todas las apariciones (reserva, manos e historial), no solo el mazo disponible. Se conserva el esquema y las 36 imágenes. Una partida normal de cuatro jugadores crea 36 Card inicialmente (24 repartidas y 12 en reserva), consume esas 12 en tres reposiciones y crea otras 16 en cuatro reposiciones: 52 apariciones, 32 históricas y 20 en mano al terminar. Con tres jugadores bastan las 36 iniciales, de las cuales se reparten 33. No hay reposición final.

El avance exige una reposición completa a seis por participante en la misma transacción de la última votación y puntuación. El bloqueo existente de partida serializa esa ruta; se comprueba estado vigente y sucesor antes de avanzar otra vez. Las notificaciones de votos/cierre/reposición se publican al commit con el mismo JSON y destinos actuales.

## Alternativas y consecuencias

Reasignar una Card histórica o borrar su turno/votos destruiría información y su identidad seguiría filtrada por React. Separar carta física y jugada en nuevas entidades permitiría un mazo físico fijo, pero necesitaría una migración y un alcance mayor. Mezclar nuevas e históricas desde la primera reposición contradiría la prioridad ahora acordada.

Crece el número de filas y el tamaño de Game serializado, conservando la forma del contrato. La disponibilidad se calcula por imágenes distintas, nunca por número de filas históricas. Las nuevas apariciones no cuentan para estadísticas/logros hasta convertirse en jugadas con autor y turno; cada nueva jugada real sí cuenta. No se resuelven aquí los defectos preexistentes de estadísticas con IDs Player/User cruzados, privacidad de entidades, autorización general o reconexiones.
