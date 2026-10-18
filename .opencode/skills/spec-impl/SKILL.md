---
name: spec-impl
description: Implementa un spec aprobado de specs/ paso a paso en rama local aparte, con verificación rápida por paso, pausa por paso (commit manual del usuario), checklist final sin auto-marcar y actualización de docs/estado.md
---

Implementa el spec indicado paso a paso. Uso: `/spec-impl specs/<NNN>-<nombre>-spec.md` (ej: `/spec-impl specs/001-mi-cambio-spec.md`).

Las reglas técnicas del proyecto (arquitectura, Room, edge-to-edge, restricción y ejecución de Gradle) viven en `AGENTS.md`. Esta skill no las copia: las referencia. Si algo de aquí contradice a `AGENTS.md`, manda `AGENTS.md`.

**Regla de git (vale para toda la skill):** el agente nunca ejecuta `git add`, `git commit`, `git stash` ni `git push`. Los commits los haces tú a mano.

### 0. Gate de aprobado (bloqueante, antes de nada)
1. Lee el fichero del spec con `read`. Si `$ARGUMENTS` viene vacío, pregunta qué spec implementar y detente hasta tener respuesta.
2. Busca con `grep` la línea `^Estado:\s*Aprobado` (case-insensitive) dentro del spec.
3. Si NO existe match exacto: detente de inmediato. Informa `Spec no aprobado: falta 'Estado: Aprobado'. Edítalo a mano para desbloquear.` No crees rama ni toques código.
4. No valen aprobaciones en chat, ni `approved` en otro formato, ni secciones libres. Solo esa línea. Nunca cambies tú el `Estado`.

### 1. Preflight y rama local
1. Preflight (solo lectura, una vez): comprueba que existen `gradle/wrapper/gradle-wrapper.jar` y `local.properties`. Si falta alguno, detente e informa: `Falta <fichero>: abre el proyecto en Android Studio para regenerarlo.` No crees rama.
2. Detecta el contexto (solo lectura): `git branch --show-current` y `git status --porcelain`. Parte siempre desde la rama actual como base.
3. Deriva `<nombre>` del spec: nombre del fichero sin `specs/` ni `-spec.md`, saneado (minúsculas, espacios/_ a `-`, solo `[a-z0-9-/.]`). Conserva el prefijo numérico (ej: `001-mi-cambio`).
4. Rama de trabajo: el mismo `<nombre>`, sin prefijos `feat/` ni `fix/`.
5. Crea la rama y cambia a ella desde la raíz del proyecto: `git switch -c <rama>`. Esto arrastra los cambios sin commitear a la nueva rama.
6. Si `<rama>` ya existe, usa `git switch <rama>` y avisa de la reutilización. Si el `switch` falla por conflicto con cambios sin commitear, detente e informa sin forzar.
7. Prohibido `git worktree add`, `.worktrees/`, el parámetro `workdir` y `cd`. Todo el código ocurre en la raíz del proyecto, en la rama de trabajo.

### 2. Bucle por pasos (pausa por paso, commit manual)
Lee `## 7. Plan de tareas` del spec y ejecuta en orden, un paso cada vez:

1. **Relee antes de cada paso:** la sección de diseño del spec (`## 6`) y, de `AGENTS.md`, las secciones `Arquitectura`, `Datos Room`, `Restricción Gradle` y `Ejecución de Gradle`, más `Edge-to-edge` si el paso toca vistas. Si el spec contradice a `AGENTS.md` o al código, avisa y sigue al código.
2. **Librerías:** si la tarea toca una librería, framework, SDK o API (Kotlin, Compose, Room, Hilt, Coil, Retrofit), usa el MCP `context7`: `resolve-library-id` y luego `query-docs` con el ID exacto, y cita ID + URL.
3. **Implementa SOLO esa tarea.** No avances tareas futuras ni agrupes pasos. No toques el esqueleto Gradle ni añadas dependencias: si el spec lo exige, detente y propón la línea exacta.
4. **Verifica** siguiendo `Ejecución de Gradle` de `AGENTS.md` (un solo Gradle a la vez, `--console=plain` con salida a `build/agent.log`, timeout de 10 min, sin reintentos al superarlo):
   - Por defecto: `./gradlew :app:compileDebugKotlin`.
   - Si el paso toca mapper/DTO: añade `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"`.
   - Si el paso toca `res/` o `AndroidManifest.xml`: usa `./gradlew assembleDebug` en lugar de `compileDebugKotlin`, porque este no detecta errores de recursos.
   - Pruebas en dispositivo (`connectedDebugAndroidTest`, `adb`, `mobile-mcp`): solo si el paso o el spec lo piden. Antes, `adb devices`. Sin dispositivo en estado `device`, no lances nada: anota `bloqueado: sin dispositivo`.
   - Si la verificación se bloquea (timeout, sin dispositivo, build colgado), anota `bloqueado: <motivo>`, no reintentes por la misma vía y sigue al informe. La verificación pasa a ti, desde Android Studio.
5. **Informa:** qué cambió (`fichero:línea`), diff resumido, resultado de la verificación (`OK`, `FALLO` o `bloqueado: <motivo>`) y `Desviaciones vs spec:` (`<§X del spec> vs <fichero:línea>` o `ninguna`; van para tu enmienda manual, el agente nunca toca el spec).
6. **Pausa sin opciones:** detente e informa `Revisa los cambios y haz commit a mano; respóndeme en el chat para seguir, corregir o parar.` Continúa solo con tu respuesta en texto libre (seguir, corrección o parar). Sin herramienta `question`, sin opciones.
7. **Regla anti-invención:** si una firma, tipo o API del spec no existe en el código o en la versión declarada (no compila, no está en el sources.jar), no lo reinventes: un intento de alternativa mínima y, si tampoco encaja, para ese punto, anótalo en `Desviaciones vs spec` y sigue con lo demás.

### 3. Verificación final y checklist (en el mismo spec, sin auto-marcar)
1. **Verificación final (solo si se completaron todos los pasos):** `./gradlew assembleDebug` y `./gradlew testDebugUnitTest`, con las reglas de `Ejecución de Gradle` de `AGENTS.md`, una vez cada uno. Si alguno queda `bloqueado`, se anota en el informe de cierre. Si se paró antes de terminar, no se lanza salvo que lo pidas.
2. Añade al final del MISMO fichero del spec la sección `Checklist verificación` si no existe (los specs creados con `/spec` ya la traen; solo créala si falta).
3. Un `- [ ]` por cada criterio de la sección `Criterios de aceptación` + uno para `assembleDebug`, otro para `testDebugUnitTest` y otro para prueba en móvil físico si aplica. Ejemplo:
   `- [ ] El filtro de ejercicios muestra solo CUSTOM al activarlo`
   `- [ ] assembleDebug OK`
   `- [ ] testDebugUnitTest OK`
   `- [ ] prueba en móvil físico`
4. Reglas estrictas:
   - Nunca escribas `- [x]`. Solo tú marcas los checks a mano tras evaluarlos.
   - Nunca cambies `Estado:` a `Implementado` ni nada parecido. Eso lo haces tú a mano.
   - No crees ficheros `*-checks.md` separados ni anexos en otro sitio.

### 4. Estado del proyecto (cambios sin commitear)
Al cerrar (todos los pasos hechos, o parada con trabajo hecho):
1. En `docs/estado.md`, añade una entrada del spec bajo `## Implementados, pendientes de merge` (créala si falta). Debe decir `implementado, pendiente de merge`, qué ficheros y piezas aporta (breve) y el resultado de la verificación tal como salió, incluido lo `bloqueado`. Nunca escribas "mergeado".
2. En `AGENTS.md`, bloque `Estado (resumen)`: actualiza solo la línea `Siguiente` si el spec implementado era el siguiente. No muevas el spec a "Hechas": eso lo haces tú al mergear.
3. No crees otros ficheros de estado.

### 5. Cierre
Informa siempre: rama de trabajo, lista de commits de la rama (`git log --oneline`), path del spec con la checklist añadida, cambios en `docs/estado.md` y `AGENTS.md` (todos sin commitear, para tu commit manual), lista final de `Desviaciones vs spec` acumuladas, verificaciones `bloqueadas` si las hubo, y `Marca los checks y el Estado a mano cuando los verifiques (móvil físico incluido).`

### 6. Política anti-bloqueo
1. Máximo 2 intentos por vía ante el mismo error: a la segunda repetición se abandona esa vía y se prueba otro camino. Sin tercer intento por la misma vía.
2. **Timeout de Gradle: cero reintentos.** Si `assembleDebug`, `compileDebugKotlin` o `testDebugUnitTest` superan los 10 minutos, se ejecuta `./gradlew --stop`, se anota `bloqueado: build > 10 min` y se pasa al informe. No hay segundo intento ni polling.
3. Búsquedas amplias y otros comandos pesados: un solo intento por paso, sin reintentos en bucle.
4. Si una vía atrapa recursos (proceso que no termina, espera larga), se corta la espera, se anota `bloqueado: <motivo>` y se cambia de vía o se salta al siguiente paso.
5. Ningún paso queda a medias sin explicar: todo bloqueo se reporta en el informe del paso.
