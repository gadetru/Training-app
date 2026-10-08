# Revisión de tarde — BORRADOR (spec 011, paso 15)

Hora exacta: **pendiente** (fijarla aquí cuando se decida).

## Qué corre en cada sitio

- **CI (puerta de merge, `blindaje.yml`):** `./gradlew assembleDebug` +
  `./gradlew testDebugUnitTest` (botes, mapper, reglas, prefill y fotos
  Roborazzi contra dorados). Un rojo bloquea el merge.
- **Emulador Pixel 6 API 34 (`emulator-5554`):**
  `./gradlew connectedDebugAndroidTest` (DAO, migración, smokes) +
  comprobación visual edge-to-edge (cabeceras `statusBarsPadding`, docks
  `navigationBarsPadding`, nada bajo hora ni barra inferior).
- **Móvil físico (automático vía `adb` + `mobile-mcp`):** lo mismo que en
  emulador: `connectedDebugAndroidTest` + visual edge-to-edge.

## Regla de auto-reparación del agente (ver `§4` del spec)

Hasta dejar todo en verde, con tope de **dos intentos** por vía; sin verde
se para y reporta, sin tocar el spec.
