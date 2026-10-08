# Arquitectura

## Visión general

La app sigue un enfoque **offline-first**: la interfaz solo lee de la base de datos local (Room/SQLite). La red solo sirve para rellenar o actualizar esa base de datos.

```
┌──────────────────────────── Móvil (Android) ────────────────────────────┐
│                                                                         │
│   UI (Compose)  ──►  ViewModel  ──►  Repository  ──►  Room (SQLite)     │
│                                          │                              │
│                                          └──►  Retrofit (red)           │
└──────────────────────────────────────────┼──────────────────────────────┘
                                           │
                    Fase 1: jsDelivr (catálogo estático, fork propio)
                    Fase 2: API Spring Boot + MySQL (cuentas y sincronización)
```

## Capas

| Capa | Responsabilidad | No debe |
|---|---|---|
| **UI** (`feature/*`) | Pantallas Compose y su estado. | Contener lógica de negocio ni acceder a Room/Retrofit. |
| **ViewModel** | Exponer el estado de la pantalla (`StateFlow`) y recibir eventos. | Conocer detalles de Compose ni de la base de datos. |
| **Repository** (`data/repository`) | Única puerta de acceso a los datos. Combina local y remoto. | Exponer entidades de Room ni DTOs a la UI. |
| **Local** (`data/local`) | Entidades, DAOs y base de datos Room. | Hacer llamadas de red. |
| **Remote** (`data/remote`) | Interfaces Retrofit y DTOs. | Escribir en la base de datos. |
| **Domain** (`domain/model`) | Modelos limpios que usa la app. | Depender de Android, Room o Retrofit. |

Flujo de mapeo: `DTO / Entity  ⇄  Modelo de dominio`. La UI solo ve modelos de dominio.

## Estructura de carpetas

Un único módulo Gradle (`app`), organizado por capas y con las pantallas agrupadas por funcionalidad.

```
app/src/main/java/<paquete>/
├── TrainingApp.kt              # @HiltAndroidApp
├── MainActivity.kt             # @AndroidEntryPoint
├── core/
│   ├── di/                     # Módulos de Hilt
│   ├── network/                # Retrofit, URL base y tag del catálogo
│   └── ui/                     # Tema y componentes Compose reutilizables
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt       # List<String> <-> JSON
│   │   ├── entity/
│   │   └── dao/
│   ├── remote/
│   │   ├── ExerciseApi.kt
│   │   └── dto/                # Espejo del JSON de la API
│   ├── mapper/                 # DTO -> Entity -> Modelo de dominio
│   └── repository/
├── domain/
│   └── model/                  # Exercise, Routine, PlannedSet, WorkoutSession...
└── feature/
    ├── exercises/              # Listado, filtros, detalle, ejercicios propios
    ├── routines/               # Crear y editar rutinas y series planificadas
    ├── workout/                # Registrar una sesión en vivo + cronómetro
    └── history/                # Sesiones pasadas
```

Cada carpeta de `feature/` contiene su `Screen` (Compose) y su `ViewModel`.

## Flujo del catálogo de ejercicios

1. El código define una constante con el tag del catálogo (`CATALOG_TAG`).
2. Al abrir la app se compara con el último tag guardado en DataStore.
3. Si es distinto (o es el primer arranque), se descarga `/api/<lang>/exercises.json`, se hace **upsert** en Room y se guarda el tag.
4. Si no hay red, la app sigue con lo que hay en SQLite.

Los GIFs no se guardan en SQLite: solo su URL. Coil los cachea en disco conforme se ven.

## Reglas y convenciones

- **IDs:** todo lo que crea el usuario usa **UUID** generado en el cliente. Los ejercicios del catálogo conservan su `id` de la API (por ejemplo, `biceps/barbell-curl`).
- **Sincronización futura:** las entidades del usuario llevan `updatedAt` y `deleted` (borrado lógico) desde el primer día.
- **Actualizar el catálogo:** usar `@Upsert`, nunca `@Insert(onConflict = REPLACE)`, porque REPLACE borra la fila y puede arrastrar filas relacionadas.
- **Migraciones:** `exportSchema = true`; cada cambio de esquema sube la versión y lleva su `Migration`. `fallbackToDestructiveMigration()` solo en desarrollo.
- **Ejercicios del catálogo y propios:** comparten tabla y se distinguen con `source` (`CATALOG` / `CUSTOM`). La actualización del catálogo solo toca los `CATALOG`.
- **Código síncrono y legible:** nombres de métodos descriptivos; evitar abstracciones que no hagan falta todavía (sin casos de uso ni módulos Gradle adicionales hasta que duelan).
- **Identidad de la app:** el `applicationId` no se cambia nunca una vez repartida la app; la keystore de firma se guarda fuera del repositorio con copia de seguridad.

## Decisiones tomadas

| Decisión | Elegido |
|---|---|
| Plataforma | Solo Android (sin iOS por ahora) |
| Lenguaje del front | Kotlin + Jetpack Compose |
| Almacenamiento en el móvil | Room sobre SQLite |
| Catálogo | Fork propio de ExerciseGymGifsDB vía jsDelivr, con tag fijo |
| Backend | Java + Spring Boot, a partir de la Fase 2 |
| Base de datos del servidor | MySQL, a partir de la Fase 2 |
| Desarrollo | Android Studio + emulador Pixel 6 API 34 (`emulator-5554`), verificación automática con `connectedDebugAndroidTest` |

## Pendiente de decidir

- Nombre definitivo de la app y `applicationId` / paquete.
- Cómo repartir las versiones entre amigos (APK manual, GitHub Releases, Firebase App Distribution).
- Fecha y forma de arrancar la Fase 2.
