# Fotos de referencia (spec 011, paso 14)

Solo **Home** (`homeRoot`) y **tarjeta de rutina** (`routineCard:r1`).
Nada de editar rutina, sesión ni detalle (van en futuros specs).

## Estado

DESBLOQUEADO: Roborazzi aprobado y declarado (`roborazzi 1.76.0`,
`robolectric 4.15.1`, plugin `io.github.takahirom.roborazzi`, dorados en
`src/test/screenshots`). Corre en JVM + Robolectric, **sin emulador**: entra
en la puerta de merge.

Desviación vs `§6` del spec: las fotos viven en `src/test/.../feature/home/`
(`HomeScreenshotTest`), no en `src/androidTest/.../feature/` — Roborazzi no
corre en dispositivo.

## Dorados versionados

- `app/src/test/screenshots/...HomeScreenshotTest.home_igual_que_la_foto.png`
- `app/src/test/screenshots/...HomeScreenshotTest.tarjeta_igual_que_la_foto.png`

Viewport fijado a Pixel 6 (`@Config(sdk = [34], qualifiers = "w411dp-h914dp")`):
sin esto Robolectric renderiza enano y el texto se parte en vertical.

## Regeneración (cambio visual legal)

1. `./gradlew recordRoborazziDebug` (regenera los 2 dorados).
2. Revisar los PNG a mano: si el cambio es legal (botes, copy), aceptar y
   commitear los dorados; si no, corregir el código.
3. `./gradlew testDebugUnitTest` en verde antes del merge.

Comparar píxeles: `./gradlew compareRoborazziDebug`;
verificar: `./gradlew verifyRoborazziDebug`.

## Fragilidad conocida (ver `§9` del spec)

Modo oscuro, tamaño de fuente del sistema y densidad cambian píxeles:
cambio legal obliga a regenerar. Las pantallas usan los botes oscuros
directos (`AppColors`), no dependen del `MaterialTheme` claro/oscuro.
