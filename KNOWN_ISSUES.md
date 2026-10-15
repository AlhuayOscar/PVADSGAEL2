# Estado de pruebas

## Trayectoria visual de lanzadores en cámara Viewpoint

**No funciona todavía / pendiente de confirmar.** La trayectoria visual del misil sigue apareciendo en cámara con Viewpoint y el proyectil puede explotar junto al jugador. El primer cambio de `ViewpointGaelCompat.lua` para ocultar el arco no resolvió el problema.

El ajuste actual sustituye el cuadrado isométrico justo antes de que Gael calcule el impacto, usando el punto de cámara de Viewpoint; la resolución de paredes y la detonación siguen a cargo de Gael. Este ajuste aún debe probarse en juego y no debe considerarse una solución confirmada. La trayectoria y la detección de impactos del proyectil real deben conservarse.
