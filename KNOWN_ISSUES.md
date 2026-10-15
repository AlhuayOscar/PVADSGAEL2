# Estado de pruebas

## Trayectoria visual de lanzadores en cámara Viewpoint

**No funciona todavía / pendiente de confirmar.** La trayectoria visual del misil sigue apareciendo en cámara con Viewpoint. El cambio de `ViewpointGaelCompat.lua` intenta ocultar el arco previsto de Gael, pero el comportamiento observado indica que no resuelve el problema.

Hace falta identificar qué renderizador dibuja la línea que aún se ve y probar la corrección dentro del juego. Hasta entonces, no considerar resuelto este problema. La trayectoria y la detección de impactos del proyectil real deben conservarse.
