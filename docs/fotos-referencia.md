# Fotos de referencia (spec 011, paso 14)

Solo **Home** (`homeRoot`) y **tarjeta de rutina** (`routineCard:<id>`).
Nada de editar rutina, sesión ni detalle (van en futuros specs).

## Estado

BLOQUEADO a falta de dependencia: Roborazzi (o Shot) no está declarado y el
agente no añade dependencias Gradle (ver `AGENTS.md`, restricción Gradle).
Las líneas propuestas están abajo; las confirma Android Studio.

## Dorados previstos

- `app/src/androidTest/screenshots/home.png` — `HomeContent` con el estado
  fake de `HomeNavTest` (2 rutinas + dock), tema oscuro (`MainActivity`
  fuerza oscuro), Pixel 6 API 34.
- `app/src/androidTest/screenshots/routine-card.png` — una `RoutineCard`
  (misma `r1` del fake) a ancho completo.

## Regeneración (cuando la dep esté aprobada)

1. `./gradlew connectedDebugAndroidTest -Proborazzi.record=true` (o
   `recordScreenshots` en Shot) en emulador Pixel 6 API 34.
2. Revisar el diff de píxeles a mano: si el cambio es legal (botes, copy),
   aceptar los dorados nuevos y commitearlos; si no, corregir el código.
3. Repetir en físico y comparar: los dorados mandan, el dispositivo no.

## Fragilidad conocida (ver `§9` del spec)

Modo oscuro, tamaño de fuente del sistema y densidad cambian píxeles:
cambio legal obliga a regenerar. Fijar en el dispositivo de referencia:
tema oscuro, fuente normal, 420dpi aprox. de Pixel 6.

## Tests previstos (crear tras aprobar la dep)

- `src/androidTest/.../feature/home/HomeScreenshotTest.kt`
  (`home_muestra_igual_que_la_foto`)
- `src/androidTest/.../feature/home/RoutineCardScreenshotTest.kt`
  (`tarjeta_igual_que_la_foto`)

## Líneas Gradle propuestas (pendientes de confirmación)

En `gradle/libs.versions.toml` (versión a fijar por Android Studio):

```toml
roborazzi = "<versión que confirme Android Studio>"
roborazzi-gradle = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
```

En `app/build.gradle.kts`:

```kotlin
alias(libs.plugins.roborazzi.gradle)
androidTestImplementation(libs.roborazzi.compose)
```

Alternativa equivalente: Shot (`com.karumi:shot` + plugin `shot`).
