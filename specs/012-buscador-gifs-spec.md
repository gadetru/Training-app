# Buscador que escribe + GIF animado en la ficha
Estado: aprobado
Depende de: specs/010-detalle-ejercicio-spec.md (la ficha ya existe; este spec solo anima su GIF y arregla el buscador del picker nacido en 003)
Fecha de creación: 2026-10-09
Descripción: Arreglar que el buscador de ejercicios no deja escribir, que el texto encuentre también por etiqueta en español y que el GIF grande de la ficha se vea animado. Las miniaturas de la lista quedan quietas.
## 1. Objetivo
Que al abrir la lista se pueda pulsar el buscador y escribir.
Que escribir `pierna` encuentre los ejercicios de pierna.
Que la ficha muestre el GIF moviéndose.
## 2. Alcance (entra / no entra)
Entra:
- Diagnosticar y arreglar que el campo no deja escribir (en emulador y en físico).
- El texto de ayuda desaparece en cuanto se pulsa el campo, antes de escribir.
- El texto libre casa con nombre, etiqueta en español (`Pierna`, `Barra`…) y slug API.
- GIF animado solo en la ficha (hero); miniaturas de la lista quietas.
- Decodificador animado registrado una vez para toda la app.
- Añadir `coil-gif` con permiso del usuario del 2026-10-09 (excepción lote D).
- Sin red: GIF desde caché si existe; si no, bloque de color sin caerse.
No entra:
- Botón Buscar explícito (primero que escriba; queda en futuros).
- Visor a pantalla completa o zoom del GIF.
- Animar miniaturas de la lista, editar rutina o sesión.
- Precargar todos los GIFs para offline.
- Cambio de esquema Room, catálogo, DTOs, ejercicios CUSTOM, reglas de series.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/exercises`: buscador (`ExercisePickerSheet.kt:267-303`) + caja observable (`ExercisesViewModel.kt:52-77`) + ficha con GIF grande (`ExerciseDetailScreen.kt:166-192`). Flujo sin cambios: pantalla → ViewModel con datos observables → repositorio → base local; la red solo rellena (`CatalogSync`). El filtro vive en memoria y lo comparten el falso y el real (`FakeExerciseRepository.kt:53-64`, usado en `RoomExerciseRepository.kt:19`). El GIF está quieto porque solo hay `coil-compose` 2.6.0 sin `coil-gif` (avisado en `ExercisePickerSheet.kt:379`). Los GIF son solo direcciones en base (`CatalogConfig.kt:14-25`, tag fijo `v1.1.0`); Coil los guarda en caché. La ficha abre por ruta con id codificado (`MainActivity.kt:59,222`).
## 4. Requisitos funcionales + no-funcionales
- Pulsar el campo abre el teclado y deja escribir en emulador y en físico.
- El teclado no tapa el campo ni el botón de confirmar.
- Escribir filtra por nombre, etiqueta ES y slug; combinado con los chips.
- Sin resultados se mantiene el aviso actual.
- La ficha anima su GIF; la lista no.
- Sin red con caché se ve (quieto o animado según caché); sin caché no se cae.
- La UI solo ve el modelo de dominio; sin lógica de negocio en pantallas.
## 5. Criterios de aceptación verificables
- Abrir la lista, pulsar el buscador y escribir `sentadilla` muestra su fila.
- Al pulsar el campo, el texto de ayuda desaparece antes de escribir.
- Escribir `pierna` encuentra ejercicios de quads.
- `press` + chip Pecho filtra combinado.
- GIF de la ficha se mueve con red.
- Ficha sin red y con caché muestra el GIF sin caerse.
- Miniaturas de la lista siguen quietas.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- Fotos Home y tarjeta iguales a sus dorados.
- En emulador y en físico nada tapa el campo al escribir.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/exercises`: arreglo mínimo del foco/teclado del buscador (misma forma por fuera); ocultar ayuda al enfocar; el filtro suma etiquetas ES en `applyFilter` (misma firma, sin tocar DAOs ni esquema).
- Coil global: `ImageLoader` con decodificador de GIF registrado una vez (en arranque/inyección), usado por los `AsyncImage` actuales.
- Gradle (excepción lote D autorizada 2026-10-09): proponer línea `coil-gif` con la misma versión que `coil` (2.6.0) en catálogo + `app/build.gradle.kts`.
- Catálogo, sets, Room y borrado lógico: no se tocan.
## 7. Plan de tareas
1. Diagnosticar por qué no escribe (emulador Pixel 6 API 34 + físico vía `adb`/`mobile-mcp`): foco, teclado, recomposición.
2. Arreglo mínimo de escritura en `feature/exercises/ExercisePickerSheet.kt` (`ExercisesViewModel.kt` si hace falta).
3. Ocultar ayuda al enfocar en `SearchBar` (`ExercisePickerSheet.kt:267-303`).
4. Sumar etiquetas ES al filtro en `data/repository/FakeExerciseRepository.kt:53-64`.
5. Añadir `coil-gif` (permiso concedido): `gradle/libs.versions.toml` + `app/build.gradle.kts`.
6. Registrar decodificador GIF global (arranque/inyección, reutilizado por ficha).
7. Comprobar hero animado en ficha y thumbs quietos en lista.
8. Prueba JVM del filtro con etiquetas en su espejo (`src/test/.../data/repository/`).
9. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `connectedDebugAndroidTest` en emulador y físico (toca UI), dorados intactos, teclado sin tapar nada.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (incluye la excepción lote D ya autorizada).
- `./gradlew testDebugUnitTest` verde (incluye la prueba nueva del filtro; `CatalogMappingTest` como regresión, no se toca mapper).
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico de forma automática (picker + ficha).
- En ambos: se escribe, el GIF de la ficha se mueve, fotos iguales, `statusBarsPadding`/`navigationBarsPadding` sin dp extra.
## 9. Riesgos / No romper
- No cambiar `applicationId`; misma keystore + subir `versionCode` al distribuir.
- Sin cambio de esquema Room (sin versión ni migración en este spec).
- `coil-gif` con la misma versión que `coil` (2.6.0); si la versión no existe, alternativa mínima + reporte, sin inventar.
- La UI solo ve `domain/model`; conversiones en `data/mapper` (no se tocan).
- `ModalBottomSheet` gestiona sus bordes sola; prohibido compensar con dp fijos.
- No borrar `Fake*Repository` aún (deuda conocida).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Botón Buscar explícito (escribir + pulsar para lanzar), planteado en la idea inicial.
- Visor de GIF a pantalla completa con zoom en la ficha.
- Precarga de GIFs al sincronizar para offline total.
- Animar miniaturas de la lista (ojo a rendimiento y fotos).
- Catálogo en inglés + reintento manual del picker vacío.
## Preguntas abiertas
- ¿Causa raíz del no-escribe? (se sabrá al diagnosticar en el paso 1).
- ¿Botón Buscar explícito en el siguiente spec tras este?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Abrir la lista, pulsar el buscador y escribir `sentadilla` muestra su fila.
- [ ] Al pulsar el campo, el texto de ayuda desaparece antes de escribir.
- [ ] Escribir `pierna` encuentra ejercicios de quads.
- [ ] `press` + chip Pecho filtra combinado.
- [ ] GIF de la ficha se mueve con red.
- [ ] Ficha sin red y con caché muestra el GIF sin caerse.
- [ ] Miniaturas de la lista siguen quietas.
- [ ] `./gradlew assembleDebug` termina OK.
- [ ] `./gradlew testDebugUnitTest` termina en verde.
- [ ] Fotos Home y tarjeta iguales a sus dorados.
- [ ] En emulador y en físico nada tapa el campo al escribir.
