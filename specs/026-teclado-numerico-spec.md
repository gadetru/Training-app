# Teclado numérico propio tipo calculadora
Estado: aprobado
Depende de: specs/020-ceros-numericos-spec.md (reutiliza su limpieza de ceros), specs/013-sesion-vivo-fixes-spec.md (el modal de serie que se adapta) y specs/023-duracion-rutina-spec.md (el campo de duración en modo MANUAL)
Fecha de creación: 2026-10-19
Descripción: Sustituir el teclado del sistema por un panel numérico propio (sin letras, estilo calculadora) para editar kg, reps y descanso en editar rutina y en el modal de serie de la sesión, incluida la duración manual de la rutina.
## 1. Objetivo
Que escribir números en la app sea rápido y con la estética propia: un panel con teclas grandes 0-9, coma, ±, borrar y OK, sin las letras del teclado del sistema. Ver `docs/02-Ideas-preguntas.md:27-29`. Se reutiliza la limpieza de ceros del spec 020 y no se toca la capa de datos.
## 2. Alcance (entra / no entra)
Entra:
- Panel numérico propio con teclas 0-9, coma, ±, borrar y OK.
- Celdas de kg, reps y descanso de editar rutina (`CellInput`).
- Campo de duración en modo MANUAL de la cabecera de rutina (misma pantalla).
- Campos kg, reps y pausa del modal de serie de la sesión en vivo.
- Reglas de teclado puras con tests JVM y smokes instrumentados.
No entra:
- Perfil (edad, estatura, peso), buscador del picker y campos de texto.
- Room, entidades, migraciones, red, catálogo y DTOs.
- Gradle, dependencias, iconos nuevos, `applicationId`, keystore/`versionCode`.
- Cabecera MetaCard colapsada al abrir: va al spec 027.
- Teclas extra (limpiar todo, vibración, teclado físico): idea para el futuro.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Hoy las celdas usan el IME del sistema: `KeyboardType.Number/Decimal` en `feature/routines/RoutineEditScreen.kt:1038-1052` (`CellInput` en `:1133-1202`), duración MANUAL en `:482-504` y modal de serie en `feature/workout/WorkoutScreen.kt:1268-1290` (`SetEditDialog` en `:1241-1360`).
- La limpieza de texto ya es única y pura: `core/ui/NumberInput.kt:7-47` (spec 020).
- `RoutineEditContent` (`RoutineEditScreen.kt:192-214`) ya guarda estado de pantalla (`metaExpanded`, `showNameDialog`): ahí vive el estado del panel.
- Flujo intacto `Screen → ViewModel (StateFlow) → Repository → Room`: el panel solo entrega texto a los mismos callbacks (`onSetFieldChange`, `onDurationChange`, `onConfirm`). Sin Room, sin migración, sin Retrofit.
- Tests que hoy teclean con el IME y habrá que adaptar: `Workout013InteractionTest.kt:157-189` y `NumberInputSmokeTest.kt:65-72`.
## 4. Requisitos funcionales + no-funcionales
- Al pulsar una celda numérica se abre el panel propio; el teclado del sistema no aparece nunca en esas celdas.
- Teclas: 0-9, coma (solo kg), ± (solo kg), borrar (último carácter) y OK (confirma y cierra).
- kg admite un separador decimal y signo; reps y descanso solo dígitos.
- Se mantienen la limpieza de ceros (spec 020) y el borrador con confirmación al perder el foco, al pulsar OK o por debounce (hoy `FIELD_DEBOUNCE_MS = 800`, `RoutineEditScreen.kt:118`).
- ± cicla sin signo → + (lastre) → - (ayuda) → sin signo; el valor se ve en la celda en vivo.
- El panel respeta edge-to-edge: `navigationBarsPadding()`, nada bajo la barra del sistema, y la celda activa se trae a la vista al abrirse.
- En el modal de serie el panel se embebe bajo los campos; OK cierra el panel, no sustituye a Guardar/Cancelar.
- Sin dependencias nuevas: botes de `ui/theme` para colores/tamaños; ⌫ y OK como texto (no hay icono Backspace en `material-icons-core`).
- Botones y teclas con `testTag` para los smokes.
## 5. Criterios de aceptación verificables
1. Pulsar una celda de kg/reps/descanso en editar rutina abre el panel propio y no el teclado del sistema.
2. Teclear dígitos actualiza la celda en vivo; en reps y descanso la tecla coma no hace nada.
3. En kg, coma inserta un único separador decimal y ± cicla sin signo → + → - → sin signo.
4. Borrar elimina el último carácter sin cerrar el panel; OK cierra confirmando el valor.
5. La limpieza de ceros se mantiene: teclear 0, 1, 2 muestra 12 (spec 020).
6. El modal de serie de la sesión usa el panel para kg, reps y pausa con las mismas reglas.
7. El campo de duración MANUAL de la cabecera de rutina usa el panel.
8. El panel queda por encima de la barra de navegación y la celda activa se ve al abrirlo.
9. Los smokes instrumentados que teclean números siguen en verde tras adaptarlos al panel.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Nuevo `core/ui/NumericKeypadRules.kt`: función pura `applyKey(texto, tecla, admiteDecimal, admiteSigno)` que delega en `NumberInput` (`sanitizeInt`/`sanitizeDecimal`); ± solo en kg; testeable en JVM sin Compose.
- Nuevo `core/ui/NumericKeypad.kt`: panel Compose de 4 o 5 filas con las teclas; reutiliza `AppColors`/`AppDimens`/`AppTextSizes`; `testTag` por tecla (`keypad0`, `keypadComa`, `keypadSigno`, `keypadBorrar`, `keypadOk`); el panel se ancla abajo con `navigationBarsPadding()` y aplica `imePadding()` si hubiera IME residual.
- `feature/routines/RoutineEditScreen.kt`: `CellInput` y el campo de duración MANUAL pasan de `BasicTextField` con IME a celda pulsable que muestra el texto; `RoutineEditContent` guarda `activeField` (qué celda edita el panel) y muestra el panel; el contenido de scroll gana hueco inferior mientras el panel está abierto.
- `feature/workout/WorkoutScreen.kt`: `SetEditDialog` reemplaza `SetEditField` por celdas pulsables y embebe el mismo panel bajo los campos; los botones Guardar/Cancelar no cambian.
- ViewModel/Repository/DAO/DTO sin cambios; la UI sigue viendo solo `domain/model`. Catálogo y sets (`weightKg` con signo, `loadNote`, `restSeconds`) intactos.
## 7. Plan de tareas
1. `core/ui/NumericKeypadRules.kt`: reglas puras de tecla → texto (delegan en `NumberInput`).
2. `app/src/test/java/com/mytrainingplan/app/core/ui/NumericKeypadRulesTest.kt`: JVM de dígitos, coma, ±, borrar y ceros.
3. `core/ui/NumericKeypad.kt`: panel Compose con testTags por tecla.
4. `feature/routines/RoutineEditScreen.kt`: celda pulsable + panel en `RoutineEditContent`; incluir duración MANUAL y ajustar scroll/insets.
5. `feature/workout/WorkoutScreen.kt`: `SetEditDialog` con celdas pulsables y panel embebido.
6. Actualizar `Workout013InteractionTest.kt:157-189` y `NumberInputSmokeTest.kt:65-72` a pulsar teclas del panel.
7. Smoke nuevo `feature/routines/NumericKeypadSmokeTest.kt` (rutina + modal): abrir panel, teclear, borrar, ± y OK.
8. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración; `CatalogMappingTest` como regresión, el mapper no se toca).
- `./gradlew testDebugUnitTest` verde (reglas nuevas + regresión de 020/023/025).
- `./gradlew connectedDebugAndroidTest` verde en emulador Pixel 6 API 34 y en físico (si MIUI rechaza el `-g`, `adb install` manual + `am instrument`).
- Prueba manual en dispositivo: abrir panel en una celda, teclear kg con coma y ±, borrar, OK y comprobar propagación de series; repetir en el modal de serie y en la duración MANUAL.
## 9. Riesgos / No romper
- Que no se abra el IME: sin foco de texto en las celdas; si algún `BasicTextField` conserva foco, `LocalSoftwareKeyboardController.hide()`.
- Mantener propagación de series (025), prefill y limpieza de ceros (020/023).
- Edge-to-edge: panel con `navigationBarsPadding()`; no solapar con el pie `SaveFooter` (018) ni con el scroll.
- `AlertDialog` gestiona insets solo; el panel no debe salirse del diálogo en pantallas pequeñas (scroll interno si hace falta).
- No escribir en Room por cada pulsación: se conserva el borrador + debounce (800 ms).
- No cambiar `applicationId`, keystore/`versionCode`, Gradle/dependencias ni schema Room.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Cabecera MetaCard colapsada al abrir (siempre, nuevas y existentes): `specs/027-...` (detectado en `docs/02-Ideas-preguntas.md:20-21` y `RoutineEditScreen.kt:214`).
- Teclado propio en Perfil (edad, estatura, peso): `ProfileScreen.kt:327-345`.
- Tecla C para limpiar todo y respuesta háptica por tecla.
- Valorar un teclado compacto/reordenado según campo (kg vs reps vs pausa).
## Preguntas abiertas
- Resueltas: panel inferior fijo en rutina y embebido en el modal; teclas 0-9, coma, ±, borrar, OK; ± solo kg; sin cambios de datos; verificación estándar en emulador + físico.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] 1. Panel propio en celdas de rutina y sin teclado del sistema.
- [ ] 2. Dígitos en vivo; coma bloqueada en reps/descanso.
- [ ] 3. Coma única en kg y ± cíclico sin signo → + → - → sin signo.
- [ ] 4. Borrar sin cerrar; OK confirma y cierra.
- [ ] 5. Ceros a la izquierda siguen prohibidos (0→12 muestra 12).
- [ ] 6. Modal de serie usa el panel (kg/reps/pausa) con las mismas reglas.
- [ ] 7. Duración MANUAL de la cabecera usa el panel.
- [ ] 8. Panel por encima de la barra de navegación y celda activa visible.
- [ ] 9. Smokes adaptados (`Workout013InteractionTest`, `NumberInputSmokeTest`) en verde.
- [ ] 10. `./gradlew assembleDebug` OK.
- [ ] 11. `./gradlew testDebugUnitTest` verde (`CatalogMappingTest` incluido).
- [ ] 12. `connectedDebugAndroidTest` verde en emulador Pixel 6 API 34.
- [ ] 13. Prueba en móvil físico (o anotado como bloqueado si no hay dispositivo).
