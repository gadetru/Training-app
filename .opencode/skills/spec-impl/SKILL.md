---
name: spec-impl
description: Implementa un spec aprobado de specs/ paso a paso en rama local aparte, con pausa por paso (commit manual del usuario) y checklist final sin auto-marcar
---

Implementa el spec indicado paso a paso. Uso: `/spec-impl specs/<NNN>-<nombre>-spec.md` (ej: `/spec-impl specs/001-mi-cambio-spec.md`).

### 0. Gate de aprobado (bloqueante, antes de nada)
1. Lee el fichero del spec con `read`. Si `$ARGUMENTS` viene vacío, pregunta qué spec implementar y detente hasta tener respuesta.
2. Busca con `grep` la línea `^Estado:\s*Aprobado` (case-insensitive) dentro del spec.
3. Si NO existe match exacto: detente de inmediato. Informa `Spec no aprobado: falta 'Estado: Aprobado'. Edítalo a mano para desbloquear.` No crees rama ni toques código.
4. No valen aprobaciones en chat, ni `approved` en otro formato, ni secciones libres. Solo esa línea. Nunca cambies tú el `Estado`.

### 1. Preparar trabajo en rama local
1. Detecta el contexto (solo lectura): `git branch --show-current` y `git status --porcelain`. Parte siempre desde la rama actual como base.
2. Deriva `<nombre>` del spec: nombre del fichero sin `specs/` ni `-spec.md`, saneado (minúsculas, espacios/_ a `-`, solo `[a-z0-9-/.]`). Conserva el prefijo numérico del spec (ej: `001-mi-cambio`).
3. Rama de trabajo: el mismo `<nombre>` del spec, sin prefijos `feat/` ni `fix/`. Rama final: `<nombre>` (ej: `001-mi-cambio`).
4. Crea la rama y cambia a ella desde la raíz del proyecto:
   `git switch -c <rama>`
   Esto arrastra automáticamente los cambios sin commitear a la nueva rama. No hagas `stash` automático.
5. Si `<rama>` ya existe, usa `git switch <rama>` y avisa de la reutilización. Si el `switch` falla por conflicto con cambios sin commitear, detente e informa sin forzar ni commitear.
6. Prohibido `git worktree add`, `.worktrees/`, el parámetro `workdir` y `cd`. Todo el código ocurre en la raíz del proyecto, en la rama de trabajo creada. Los commits los haces tú a mano: el agente nunca ejecuta `git add` ni `git commit`.

### 2. Bucle por pasos (pausa por paso, commit manual)
Lee `## 7. Plan de tareas` del spec y ejecuta en orden, un paso cada vez:
1. Relee la sección de diseño del spec (`## 6`) y los gotchas de `AGENTS.md` antes de cada paso: capas `UI → ViewModel (StateFlow) → Repository → Room`, UI solo ve `domain/model` (mapping en `data/mapper`), Retrofit solo rellena DB, `@Upsert` nunca `@Insert(REPLACE)`, catálogo solo toca `source=CATALOG`, IDs UUID para lo creado por usuario (catálogo conserva `biceps/barbell-curl`), `updatedAt` + `deleted` con borrado lógico, migraciones con versión + `Migration` (`exportSchema=true`, schemas en `app/schemas/`), `weightKg` con signo + `loadNote` + `restSeconds` por serie, no cambiar `applicationId`, no `applicationIdSuffix ".debug"`.
2. Si la tarea toca una librería, framework, SDK o API (Kotlin, Compose, Room, Hilt, Coil, Retrofit), usa el MCP `context7`: `resolve-library-id` y luego `query-docs` con el ID exacto, y cita ID + URL.
3. Implementa SOLO esa tarea. No avances tareas futuras ni agrupes pasos.
4. Verifica en la raíz del proyecto: `./gradlew assembleDebug` OK. Si toca mapper/DTO, añade `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"`. Nota: `gradle/wrapper/gradle-wrapper.jar` falta en el repo — si el wrapper falla, avisa y detente (abrir en Android Studio primero regenera el jar). Sin emulador: la prueba en dispositivo se hace a mano con `./gradlew installDebug` en móvil físico.
5. Informa: qué cambió (`fichero:línea`), diff resumido, resultado de la verificación y `Desviaciones vs spec:` (`<§X del spec> vs <fichero:línea>` o `ninguna`; van para tu enmienda manual, el agente nunca toca el spec).
6. Pausa sin opciones: detente e informa `Revisa los cambios y haz commit a mano; respóndeme en el chat para seguir, corregir o parar.` Continúa solo con tu respuesta en texto libre (seguir, corrección o parar). Sin herramienta `question`, sin opciones.
7. Prohibido commitear: los commits los haces tú a mano. El agente nunca ejecuta `git add` ni `git commit`.
8. Regla anti-invención: si una firma, tipo o API del spec no existe en el código o en la versión declarada (no compila, no está en el sources.jar), no lo reinventes: un intento de alternativa mínima y, si tampoco encaja, para ese punto, anótalo en `Desviaciones vs spec` y sigue con lo demás.

### 3. Checklist final (en el mismo spec, sin auto-marcar)
1. Al terminar todos los pasos (o al parar), añade al final del MISMO fichero del spec la sección final `Checklist verificación` si no existe (los specs creados con `/spec` ya la traen; solo créala si falta).
2. Un `- [ ]` por cada criterio de la sección `Criterios de aceptación` + uno para `assembleDebug` y otro para test/prueba en móvil si aplica. Ejemplo:
   `- [ ] El filtro de ejercicios muestra solo CUSTOM al activarlo`
   `- [ ] assembleDebug OK`
   `- [ ] prueba en móvil físico (installDebug)`
3. Reglas estrictas:
   - Nunca escribas `- [x]`. Solo tú marcas los checks a mano tras evaluarlos.
   - Nunca cambies `Estado:` a `Implementado` ni nada parecido. Eso lo haces tú a mano.
   - No crees ficheros `*-checks.md` separados ni anexos en otro sitio.

### 4. Cierre
Informa siempre: rama de trabajo creada, lista de commits de la rama (`git log --oneline`), path del spec con la checklist añadida (cambio sin commitear, para tu commit manual), lista final de `Desviaciones vs spec` acumuladas y `Marca los checks y el Estado a mano cuando los verifiques en móvil físico.`

### 5. Política anti-bloqueo (la misma de `/verifier`)
1. Máximo 2 intentos por vía ante el mismo error: a la segunda repetición se abandona esa vía y se prueba otro camino. Sin tercer intento por la misma vía.
2. Comandos pesados (`assembleDebug`, búsquedas amplias): un solo intento por paso, con timeout corto, sin reintentos en bucle ni polling.
3. Si una vía atrapa recursos (build colgado, proceso que no termina, espera larga), se corta la espera, se anota `bloqueado: <motivo>` y se cambia de vía o se salta al siguiente paso.
4. Ningún paso queda a medias sin explicar: todo bloqueo se reporta en el informe del paso.
