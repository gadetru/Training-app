# Logo launcher (miLogo) + splash detallado
Estado: implementado
Depende de: Ninguno
Fecha de creación: 2026-10-18
Descripción: Sustituir el icono por defecto de la plantilla por el logo del rack
(references/plantilla-logo/miLogo.png) en el launcher, con las variantes necesarias
y un splash en Android 12+ que muestre la imagen original con más detalle.

## 1. Objetivo
Que el icono de la app en el teléfono sea el logo del rack (naranja/lima sobre
oscuro) en todas sus formas: cuadrado, redondo, adaptive y themed. Y que al abrir
la app el splash del sistema (Android 12+) muestre la imagen original completa,
no la versión recortada del launcher. La verdad del logo sigue siendo miLogo.png;
el script solo la lee, nunca la modifica.

## 2. Alcance (entra / no entra)
Entra:
- 4 variantes generadas del logo: completa, transparente (solo rack+barbell),
  monochrome (silueta de un tono) y splash (original con margen para el recorte
  circular del sistema).
- PNG legacy `ic_launcher.png` / `ic_launcher_round.png` por densidad
  (48/72/96/144/192).
- Capas adaptive: foreground PNG por densidad (108/162/216/324/432),
  background vector sólido #111316 y monochrome único.
- XMLs adaptativos (`mipmap-anydpi/ic_launcher(.xml)` y `_round`) actualizados.
- Splash API 31+: `values-v31/themes.xml` con fondo oscuro y icon = logo
  detallado. API ≤30 sin cambio posible (sin dependencia nueva).
- Script commitado `scripts/gen-icons.ps1` (PowerShell 5.1 + System.Drawing)
  que lee miLogo.png y escribe los PNG en res/.
- Borrar los restos del robot: webp legacy y los vectores del foreground/background.
No entra:
- Mostrar el logo dentro de la app (vistas Compose).
- Splash personalizado elaborado (nombre + animación) → futuro.
- `versionCode`, release, keystore (bump queda para un spec de distribución).
- Cambiar `applicationId`, manifest o dependencias Gradle.
- Room, catálogo, DTOs, sets (confirmado con el usuario).

## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Spec de recursos, sin Kotlin. El manifest ya apunta a `@mipmap/ic_launcher` y
`ic_launcher_round` (AndroidManifest.xml:13-15): no se toca. `minSdk 26` →
adaptive siempre disponible; los legacy son solo fallback de algunos launchers.
El tema actual es una línea en `values/themes.xml:4` (`Theme.MyTrainingPlan`).
Sin datos, sin features, sin catálogo. Colores alineados con `ui/theme/Color.kt`
(`Bg #111316`, `Orange #FF5E00`, `Volt #CCFF00`).

## 4. Requisitos funcionales + no-funcionales
- El launcher muestra el rack en emulador y físico, en forma cuadrada y circular.
- Nada del dibujo se corta por la máscara circular del launcher: el contenido
  adaptive vive en la "zona segura" (el tercio central del lienzo de 108dp).
- Splash API 31+: la imagen original con detalle, centrada sobre fondo #111316.
- Sin dependencias Gradle nuevas ni build files editados.
- Regenerable: reejecutar el script produce PNGs válidos; si los bytes cambian,
  se commit la última salida (GDI+ no garantiza determinismo perfecto).
- Calidad de escalado: interpolación bicúbica de alta calidad + antialiasing.

## 5. Criterios de aceptación verificables
- CA1: En emulador Pixel 6 API 34 y en el físico, el ícono del launcher es el rack.
- CA2: El dibujo completo (rack + barra) es visible, sin recortes en círculo.
- CA3: Con icono temático (themed icon) activado en el emulador, se ve la
  silueta monochrome coherente (en MIUI puede no existir la opción; solo emulador).
- CA4: En API 31+ el splash de arranque muestra el logo detallado sobre fondo oscuro.
- CA5: `./gradlew assembleDebug` y `testDebugUnitTest` en verde
  (CatalogMappingTest no aplica: no toca mapper/DTO).

## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
Solo `res/` y `scripts/`. Variantes y destino:
1. Completa cuadrada → `mipmap-*/ic_launcher.png` legacy.
2. Redondeada (máscara circular con antialias) → `mipmap-*/ic_launcher_round.png`.
3. Transparente → `mipmap-*/ic_launcher_foreground.png`: se vacía el fondo oscuro
   por inundación desde los bordes (tolerancia de color; NO umbral global, para no
   comer los trazos oscuros internos) y se centra el dibujo en la zona segura.
4. Monochrome → silueta a un tono a partir del alfa de la variante transparente,
   como drawable único.
5. Splash → `drawable-nodpi/splash_logo.png`: la original 1024×1024 sobre un
   lienzo mayor con margen transparente, para que el recorte circular del sistema
   no corte el rack.
Plus: `drawable/ic_launcher_background.xml` rectángulo sólido #111316;
`mipmap-anydpi/ic_launcher(.xml)/_round` apuntando a las capas nuevas;
`values-v31/themes.xml` con `windowSplashScreenBackground` (#111316) y
`windowSplashScreenAnimatedIcon` = splash_logo (solo afecta API 31+; el padre
del tema se mantiene igual).
Gotchas AGENTS.md respetados: no `applicationId`, no `versionCode`, misma
keystore, sin deps nuevas, `gradle-wrapper` intacto.

## 7. Plan de tareas
1. `scripts/gen-icons.ps1`: leer miLogo.png y generar las 5 variantes.
2. Legacy: escribir PNGs 48/72/96/144/192 → `app/src/main/res/mipmap-*/`.
3. Adaptive: escribir foregrounds 108/162/216/324/432 + monochrome único
   → `app/src/main/res/mipmap-*/` y `drawable-nodpi/`.
4. Background sólido → `drawable/ic_launcher_background.xml` (reescrito).
5. XMLs adaptativos → `mipmap-anydpi/ic_launcher.xml` e `ic_launcher_round.xml`.
6. Splash → `drawable-nodpi/splash_logo.png` + `values-v31/themes.xml`.
7. Borrar restos del robot: `mipmap-*/ic_launcher*.webp` y
   `drawable/ic_launcher_foreground.xml` (solo tras estar generados los nuevos
   y en verde).
8. Verificación: `./gradlew assembleDebug` + `./gradlew testDebugUnitTest`,
   instalar en emulador y físico, fotos de launcher, themed (emulador) y splash.

## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` y `./gradlew testDebugUnitTest` en verde.
- Emulador Pixel 6 API 34 + físico Xiaomi: captura del launcher (cuadrado y
  redondo), themed icon en emulador y splash al abrir la app.
- Cada paso de build con `--console=plain > build/agent.log` y timeout de 10 min.

## 9. Riesgos / No romper
- Vaciar el fondo por umbral global puede comer trazos internos → inundación
  desde bordes.
- El splash custom solo funciona en API 31+; en 26-30 no hay efecto (y no crash).
- Máscara circular del launcher/splash → mitigada con la zona segura y el margen.
- MIUI cachea iconos viejos: si persiste el robot, desinstalar/reinstalar.
- Borrar los webp al final: mientras `@mipmap/ic_launcher` siga definido
  en algún sitio el build no rompe, pero el orden seguro es generar → build → borrar.
- No tocar: manifest, `applicationId`, `versionCode`, build files, `references/`.

## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Logo dentro de la app (cabecera de Home o Perfil).
- Splash personalizado con nombre + animación (splash screen API completa).
- Bump de `versionCode` y pipeline de release (keystore fuera del repo).
- Optimizar PNGs finales a WebP con Android Studio si el APK crece.
- Icono dinámico/estacional (múltiples `activity-alias`).

## Preguntas abiertas
- Ninguna bloqueante. Fondo de splash decidido: #111316 (coherente con la app).

## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] CA1: icono rack en emulador API 34 y físico (fotos).
- [x] CA2: dibujo completo sin recortes circulares.
- [x] CA3: themed icon silueta coherente (emulador).
- [x] CA4: splash con logo detallado en API 31+ (emulador y físico si aplica).
- [x] `./gradlew assembleDebug` OK.
- [x] `./gradlew testDebugUnitTest` OK.
- [x] Prueba en móvil (launcher + splash) en emulador y físico.
