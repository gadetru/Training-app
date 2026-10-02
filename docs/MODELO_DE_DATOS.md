# Modelo de datos (Room)

Distinción clave: **lo planificado** (rutinas) y **lo realizado** (sesiones) son tablas separadas. Una rutina dice qué se quiere hacer; una sesión guarda lo que realmente se hizo.

## Jerarquía

```
Routine (un día de entrenamiento)
 └── RoutineExercise (un ejercicio dentro de ese día)
      └── PlannedSet (una serie planificada: reps, peso, descanso)

WorkoutSession (un entreno realizado, normalmente a partir de una Routine)
 └── SetEntry (una serie realizada)
```

Por qué así: el descanso, el peso y las repeticiones viven en la **serie planificada de ese ejercicio en ese día**, no en el ejercicio. La sentadilla del Día A y la del Día B son filas distintas y no comparten valores.

## Catálogo

### ExerciseEntity

| Campo | Tipo | Notas |
|---|---|---|
| `id` | String (PK) | Del catálogo: `biceps/barbell-curl`. Propios: UUID. |
| `source` | enum | `CATALOG` o `CUSTOM` |
| `slug` | String | |
| `name` | String | En el idioma descargado |
| `muscle` | String | |
| `bodyPart` | String | |
| `equipment` | String | |
| `category` | String | |
| `secondaryMuscles` | List<String> | Guardado como JSON (`TypeConverter`) |
| `instructions` | List<String> | Guardado como JSON (`TypeConverter`) |
| `gifUrl` | String? | Solo la URL; Coil cachea la imagen |
| `updatedAt`, `deleted` | | Solo relevantes en `CUSTOM` |

## Planificación

### RoutineEntity

`id` (UUID), `name`, `position`, `createdAt`, `updatedAt`, `deleted`

### RoutineExerciseEntity

`id` (UUID), `routineId`, `exerciseId`, `position`, `note`, `updatedAt`, `deleted`

### PlannedSetEntity

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | |
| `routineExerciseId` | UUID | |
| `setNumber` | Int | Orden dentro del ejercicio |
| `targetReps` | Int | |
| `weightKg` | Double | **Con signo:** `+` lastre, `0` peso corporal, `-` ayuda |
| `restSeconds` | Int | Descanso de **esta** serie |
| `loadNote` | String? | Por ejemplo, "goma amarilla" |
| `updatedAt`, `deleted` | | |

## Registro de entrenos

### WorkoutSessionEntity

`id` (UUID), `routineId` (nullable), `startedAt`, `endedAt`, `updatedAt`, `deleted`

### SetEntryEntity

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | |
| `sessionId` | UUID | |
| `exerciseId` | String | |
| `plannedSetId` | UUID? | De dónde salió, si venía de una rutina |
| `setNumber` | Int | |
| `reps` | Int | Realizadas |
| `weightKg` | Double | Con signo, igual que en la planificación |
| `loadNote` | String? | |
| `restSeconds` | Int? | Descanso aplicado |
| `updatedAt`, `deleted` | | |

## Comportamiento de las series planificadas

1. Al añadir un ejercicio a un día se indica el número de series y se crean esas filas, **copiando los valores de la primera** (reps, peso, descanso, nota).
2. Al editar un campo de una fila, el valor **se propaga a las filas siguientes que aún no se hayan editado a mano**. Una fila editada a mano pasa a ser independiente y ya no se sobrescribe. (Este estado de "editada a mano" es solo de la pantalla, no se guarda en la base de datos.)
3. Al añadir un ejercicio a un día nuevo **no se copian** los valores de otros días: arranca con los valores por defecto de los ajustes.
4. Al iniciar una sesión, cada serie se rellena con los valores **de la última sesión** en que se hizo ese ejercicio en ese día; si no existe, con los planificados.

### Ejemplo

Sentadilla en el Día A:

| Serie | Reps | Peso | Descanso | Nota |
|---|---|---|---|---|
| 1 | 10 | 50 kg | 120 s | |
| 2 | 5 | 100 kg | 240 s | |

Dominadas con ayuda:

| Serie | Reps | Peso | Descanso | Nota |
|---|---|---|---|---|
| 1 | 5 | -15 kg | 150 s | goma amarilla |

## Notas para el futuro

- Accesorios reutilizables (por ejemplo, "goma amarilla = -15 kg") como tabla propia: se eligen de una lista en vez de escribir la nota cada vez.
- Todas las entidades del usuario llevan `updatedAt` y `deleted` para poder sincronizarlas con el servidor en la Fase 2.
