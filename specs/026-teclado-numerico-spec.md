# Teclado numérico propio tipo calculadora
Estado: aprobado
Depende de: specs/020-ceros-numericos-spec.md (reutiliza su limpieza de ceros), specs/013-sesion-vivo-fixes-spec.md (el modal de serie que se adapta) y specs/023-duracion-rutina-spec.md (el campo de duración en modo MANUAL)
Fecha de creación: 2026-10-19
Última revisión: 2026-10-25 (enmienda por feedback de producto; ver "Ajustes aprobados por feedback")
Descripción: Sustituir el teclado del sistema por un panel numérico propio (sin letras, estilo calculadora) para editar kg, reps y descanso en editar rutina y en el modal de serie de la sesión, incluida la duración manual de la rutina.
## 1. Objetivo
Que escribir números en la app sea rápido y con la estética propia: un panel con teclas grandes 0-9, coma, borrar y OK, sin las letras del teclado del sistema. Ver `docs/02-Ideas-preguntas.md:27-29`. Se reutiliza la limpieza de ceros del spec 020 y no se toca la capa de datos.
## 2. Alcance (entra / no entra)
Entra:
- Panel numérico propio con teclas 0-9, coma, borrar y OK.
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
- Teclas: 0-9, coma (solo kg), borrar (último carácter) y OK (confirma y cierra).
- kg admite un único separador decimal, pero no signo desde el panel; reps y descanso solo dígitos.
- El valor se ve en la celda activa actualizado en vivo mientras se teclea (antes de OK) y el número queda centrado en la celda.
- Se mantiene la limpieza de ceros (spec 020). El borrador se confirma al pulsar OK o al cambiar de celda (autoguardado: equivale a OK); no hay debounce.
- Cambiar de celda (o pasar de celda a duración y viceversa) autoguarda el borrador anterior y deja listo el nuevo destino para seguir tecleando.
- El panel respeta edge-to-edge: `navigationBarsPadding()`, nada bajo la barra del sistema, y la celda activa se trae a la vista al abrirse. Aspecto de tarjeta (fondo `Card`, borde sutil y radios/padding propios), sin visor ni etiqueta sobre las teclas.
- En el modal de serie el panel se embebe bajo los campos; OK cierra el panel, no sustituye a Guardar/Cancelar.
- Sin dependencias nuevas: botes de `ui/theme` para colores/tamaños; ⌫ y OK como texto (no hay icono Backspace en `material-icons-core`).
- Botones y teclas con `testTag` para los smokes.
## 5. Criterios de aceptación verificables
1. Pulsar una celda de kg/reps/descanso en editar rutina abre el panel propio y no el teclado del sistema.
2. Teclear dígitos actualiza la celda en vivo; en reps y descanso la tecla coma no hace nada.
3. En kg, coma inserta un único separador decimal; el panel no ofrece signo y el `weightKg` guardado se conserva tal cual (el ± queda fuera por decisión de producto).
4. Borrar elimina el último carácter sin cerrar el panel; OK cierra confirmando el valor.
5. La limpieza de ceros se mantiene: teclear 0, 1, 2 muestra 12 (spec 020).
6. El modal de serie de la sesión usa el panel para kg, reps y pausa con las mismas reglas.
7. El campo de duración MANUAL de la cabecera de rutina usa el panel.
8. El panel queda por encima de la barra de navegación y la celda activa se ve al abrirlo.
9. Los smokes instrumentados que teclean números siguen en verde tras adaptarlos al panel.
10. Cambiar de celda (o a la duración) guarda el valor tecleado sin pulsar OK y deja el foco en el nuevo destino.
11. El número de la celda se muestra centrado horizontalmente.
12. Un test JVM (`testDebugUnitTest`) garantiza actualización en vivo, autoguardado al cambiar de celda y centrado.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Nuevo `core/ui/NumericKeypadRules.kt`: función pura `applyKey(texto, tecla, admiteDecimal)` (sin parámetro de signo) que delega en `NumberInput` (`sanitizeInt`/`sanitizeDecimal`); testeable en JVM sin Compose.
- Nuevo `core/ui/NumericKeypad.kt`: panel Compose con 4 filas de teclas (789/456/123/0,⌫) y fila OK a ancho completo, con aspecto de tarjeta; reutiliza `AppColors`/`AppDimens`/`AppTextSizes`; `testTag` por tecla (`keypad0`…`keypad9`, `keypadComa`, `keypadBorrar`, `keypadOk`; sin `keypadSigno`); el panel se ancla abajo con `navigationBarsPadding()` y aplica `imePadding()` si hubiera IME residual. `core/ui/BlinkingCaret.kt` aporta el caret parpadeante de la celda activa.
- `feature/routines/RoutineEditScreen.kt`: `CellInput` y el campo de duración MANUAL pasan de `BasicTextField` con IME a celda pulsable que muestra el texto centrado; `RoutineEditContent` guarda `activeCell` (`ActiveFieldCell`), `panelDraft` y `editingDuration`, y `commitActiveEdit()` es la única vía de guardado (OK, cambio de celda y cambio a duración); el contenido de scroll gana hueco inferior mientras el panel está abierto.
- `feature/workout/WorkoutScreen.kt`: `SetEditDialog` reemplaza `SetEditField` por celdas pulsables y embebe el mismo panel bajo los campos; `commitActiveField()` vuelca el borrador al cambiar de campo; los botones Guardar/Cancelar no cambian.
- ViewModel/Repository/DAO/DTO sin cambios; la UI sigue viendo solo `domain/model`. Catálogo y sets (`weightKg` con signo en datos, `loadNote`, `restSeconds`) intactos: el panel no introduce signo, pero conserva el valor guardado.
## 7. Plan de tareas
1. `core/ui/NumericKeypadRules.kt`: reglas puras de tecla → texto (delegan en `NumberInput`).
2. `app/src/test/java/com/mytrainingplan/app/core/ui/NumericKeypadRulesTest.kt`: JVM de dígitos, coma, borrar y ceros (sin signo).
3. `core/ui/NumericKeypad.kt`: panel Compose con testTags por tecla.
4. `feature/routines/RoutineEditScreen.kt`: celda pulsable + panel en `RoutineEditContent`; incluir duración MANUAL, ajustar scroll/insets, actualización en vivo, centrado y autoguardado al cambiar de celda.
5. `feature/workout/WorkoutScreen.kt`: `SetEditDialog` con celdas pulsables y panel embebido, con autoguardado al cambiar de campo.
6. Actualizar `Workout013InteractionTest.kt:157-189` y `NumberInputSmokeTest.kt:65-72` a pulsar teclas del panel.
7. Smoke nuevo `feature/routines/NumericKeypadSmokeTest.kt` (rutina + modal): abrir panel, teclear, borrar y OK.
8. Test JVM nuevo `feature/routines/RoutineEditLiveCellTest.kt`: vivo antes de OK, autoguardado al cambiar de celda y número centrado.
9. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde (incluye `RoutineEditLiveCellTest`) + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración; `CatalogMappingTest` como regresión, el mapper no se toca).
- `./gradlew testDebugUnitTest` verde (reglas nuevas + `RoutineEditLiveCellTest` + regresión de 020/023/025).
- `./gradlew connectedDebugAndroidTest` verde en emulador Pixel 6 API 34 y en físico (si MIUI rechaza el `-g`, `adb install` manual + `am instrument`).
- Prueba manual en dispositivo: abrir panel en una celda, teclear kg con coma, borrar, cambiar de celda (comprobar autoguardado y centrado), OK y comprobar propagación de series; repetir en el modal de serie y en la duración MANUAL.
## 9. Riesgos / No romper
- Que no se abra el IME: sin foco de texto en las celdas; si algún `BasicTextField` conserva foco, `LocalSoftwareKeyboardController.hide()`.
- Mantener propagación de series (025), prefill y limpieza de ceros (020/023).
- Edge-to-edge: panel con `navigationBarsPadding()`; no solapar con el pie `SaveFooter` (018) ni con el scroll.
- `AlertDialog` gestiona insets solo; el panel no debe salirse del diálogo en pantallas pequeñas (scroll interno si hace falta).
- No escribir en Room por cada pulsación: el borrador se confirma solo al pulsar OK o al cambiar de celda (sin debounce).
- No cambiar `applicationId`, keystore/`versionCode`, Gradle/dependencias ni schema Room.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Cabecera MetaCard colapsada al abrir (siempre, nuevas y existentes): `specs/027-...` (detectado en `docs/02-Ideas-preguntas.md:20-21` y `RoutineEditScreen.kt:214`).
- Teclado propio en Perfil (edad, estatura, peso): `ProfileScreen.kt:327-345`.
- Tecla C para limpiar todo y respuesta háptica por tecla.
- Valorar un teclado compacto/reordenado según campo (kg vs reps vs pausa).
## Ajustes aprobados por feedback (2026-10-25)
Cambios de producto posteriores a la aprobación original; sustituyen a los requisitos con ± y al debounce:
- Se retira la tecla ±: el kg no admite signo desde el panel. En datos, `weightKg` sigue admitiendo signo y el valor guardado se conserva; editar desde el panel no lo altera.
- El panel se presenta como tarjeta (fondo, borde y radios propios) y sin visor ni etiqueta: el valor se ve en la celda activa con caret parpadeante.
- Los números se muestran centrados en la celda (rutina y modal de serie).
- La celda refleja el borrador en vivo antes de OK y al cambiar de celda se autoguarda como si se hubiera pulsado OK, dejando el foco en la nueva celda.
- Se elimina el debounce de 800 ms (constante `FIELD_DEBOUNCE_MS` retirada).
- Nuevo test JVM `RoutineEditLiveCellTest` como garantía permanente de vivo, autoguardado y centrado.
## Preguntas abiertas
- Resueltas: panel inferior fijo en rutina y embebido en el modal; teclas 0-9, coma, borrar, OK (sin ±, ver ajustes por feedback); coma solo kg; sin cambios de datos; verificación estándar en emulador + físico.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] 1. Panel propio en celdas de rutina y sin teclado del sistema.
- [x] 2. Dígitos en vivo; coma bloqueada en reps/descanso.
- [x] 3. Coma única en kg; sin signo en el panel (decisión de producto).
- [x] 4. Borrar sin cerrar; OK confirma y cierra.
- [x] 5. Ceros a la izquierda siguen prohibidos (0→12 muestra 12).
- [x] 6. Modal de serie usa el panel (kg/reps/pausa) con las mismas reglas.
- [x] 7. Duración MANUAL de la cabecera usa el panel.
- [x] 8. Panel por encima de la barra de navegación y celda activa visible.
- [x] 9. Smokes adaptados (`Workout013InteractionTest`, `NumberInputSmokeTest`) en verde.
- [x] 10. `./gradlew assembleDebug` OK.
- [x] 11. `./gradlew testDebugUnitTest` verde (`CatalogMappingTest` incluido).
- [x] 12. `connectedDebugAndroidTest` verde en emulador Pixel 6 API 34.
- [x] 13. Prueba en móvil físico (o anotado como bloqueado si no hay dispositivo).
- [x] 14. Cambiar de celda/de campo autoguarda sin pulsar OK y enfoca el nuevo destino.
- [x] 15. Número centrado en la celda.
- [x] 16. `RoutineEditLiveCellTest` verde en `testDebugUnitTest` (vivo + autoguardado + centrado).
