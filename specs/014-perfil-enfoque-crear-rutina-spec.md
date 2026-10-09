# Perfil: enfoque sin cortes + Crear Rutina vinculado
Estado: Borrador
Depende de: Ninguno (paralelo a 013; sigue a 001/002 ya en main)
Fecha de creación: 2026-10-09
Descripción: Que Hipertrofia/Fuerza & Potencia/Resistencia quepan legibles sin apilarse cortados, y que el botón Crear Rutina + de la vista previa abra crear rutina como el de Home.
## 1. Objetivo
Los 3 enfoques se leen enteros sin cortes. El botón de la vista previa crea rutina.
## 2. Alcance (entra / no entra)
Entra:
- Rejilla de enfoque que se ajusta (salto de línea o scroll horizontal) sin cortar texto.
- Botón de vista previa navega a crear rutina nueva (misma ruta que Home).
- Mantener los 3 valores y su guardado tal cual.
No entra:
- Cambiar valores de `TrainingGoal`, niveles, métricas, avatar, sesión en vivo.
- Cambio de esquema Room, DTOs, catálogo.
- Spec 013.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/profile` (`ProfileScreen.kt` chips + preview) y navegación (`MainActivity.kt` ruta `routineEdit` nuevo). Home ya navega con `onCreate` (`HomeScreen.kt`). Sin datos nuevos: `TrainingGoal` (`domain/model/Profile.kt`). Sin Room ni catálogo.
## 4. Requisitos funcionales + no-funcionales
- Los 3 enfoques caben legibles a 360dp sin corte vertical de texto.
- Selección única con mismo guardado en `Profile`.
- Botón preview abre `routineEdit` nuevo (id null) igual que el CTA de Home.
- Edge-to-edge intacto; diálogos/sheets sin retoques de insets.
## 5. Criterios de aceptación verificables
- Resistencia se lee entera sin partirse en columnas.
- Cambiar de enfoque lo marca y se guarda igual que antes.
- Pulsar Crear Rutina + de la preview abre el constructor vacío.
- A 360dp no hay corte ni solape de chips.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `ProfileScreen`: chips en contenedor flexible (salto de línea o fila con scroll) con mismo `onGoal`; preview recibe `onCreate` y lo llama en su botón.
- `MainActivity`/nav: reutilizar la ruta `routineEdit` sin id (mismo destino que Home), sin pantallas nuevas.
- No se toca: Room, DTOs, `CATALOG_TAG`, iconos, resto de perfil/home.
## 7. Plan de tareas
1. Layout flexible de enfoque en `feature/profile/ProfileScreen.kt`.
2. Vincular botón preview a crear rutina (`ProfileScreen.kt` + navegación en `MainActivity.kt` si falta).
3. Verificación = `./gradlew assembleDebug` OK + prueba en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin `CatalogMappingTest`: no toca mapper).
- Emulador Pixel 6 API 34 + físico, automático, a 360dp: chips legibles, preview navega, sin solapes bajo barras.
## 9. Riesgos / No romper
- No cambiar `applicationId`, dependencias Gradle, ni valores de `TrainingGoal`.
- No romper guardado de perfil ni CTA de Home ni dock/navegación.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Unificar CTA preview con TopBar de Home en componente común — detectado en `HomeScreen.kt:268` vs `ProfileScreen.kt:493`.
## Preguntas abiertas
Ninguna (interrogatorio respondido 2026-10-09).
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Resistencia legible entera sin cortes.
- [ ] Cambio de enfoque marca y guarda igual.
- [ ] Preview Crear Rutina + abre constructor vacío.
- [ ] Sin cortes ni solapes a 360dp.
- [ ] `./gradlew assembleDebug` OK.
- [ ] Probado en emulador Pixel 6 API 34 + físico automático.
