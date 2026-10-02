# Training App (nombre provisional)

App Android para planificar rutinas y registrar entrenos de fuerza. Funciona sin conexión, no lleva anuncios y está pensada para controlar al detalle cada serie.

> **Estado:** en diseño. Todavía no hay código; este README y los documentos de `docs/` fijan las decisiones de partida.

## Principios

- **Offline-first:** todo lo que ve y guarda el usuario vive en SQLite en el móvil. La red es un extra, no una dependencia.
- **Tecnología gratuita y open source** 
- **Plan por serie:** cada serie de un ejercicio tiene sus propias repeticiones, peso y descanso, de forma independiente en cada día de entrenamiento.
- **Lastre y gomas:** el peso admite signo (positivo = lastre, negativo = ayuda) y una nota libre (por ejemplo, "goma amarilla").

## Stack y herramientas

### App Android (Fase 1)

| Pieza | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose |
| Navegación | Navigation Compose |
| Base de datos local | Room (sobre SQLite) |
| Inyección de dependencias | Hilt |
| Red | Retrofit + kotlinx.serialization |
| Imágenes y GIFs | Coil (con soporte GIF) |
| Ajustes | DataStore |
| Tareas en segundo plano | WorkManager (más adelante) |
| Build | Gradle (Kotlin DSL + catálogo de versiones) |

### Catálogo de ejercicios

- Fork propio del repositorio **ExerciseGymGifsDB**: API estática (JSON + GIFs) servida por el CDN jsDelivr.
- URL base: `https://cdn.jsdelivr.net/gh/<usuario>/ExerciseGymGifsDB@<tag>`
- Siempre se apunta a un **tag fijo**, nunca a una rama.
- Idiomas disponibles: `es` y `en`.

### Backend (Fase 2)

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Java (OpenJDK) + Spring Boot |
| Acceso a datos | Spring Data JPA |
| Base de datos | MySQL |
| Migraciones | Flyway |
| Documentación de la API | springdoc-openapi |
| Seguridad | Spring Security + JWT |

### Herramientas de desarrollo

- Android Studio, probando directamente en un móvil físico (sin emulador).
- Git y GitHub.
- Gradle Wrapper (`gradlew`), que fija la versión de Gradle del proyecto.

## MVP (Fase 1)

1. Ver el catálogo de ejercicios y filtrarlo por músculo y equipamiento.
2. Ver el detalle de un ejercicio (GIF e instrucciones).
3. Crear ejercicios propios que no estén en el catálogo.
4. Crear una rutina (un día de entrenamiento) con ejercicios y, para cada uno, sus series planificadas: repeticiones, peso y descanso por serie.
5. Iniciar una sesión desde una rutina y registrar cada serie (repeticiones, peso con signo y nota).
6. Cronómetro de descanso dentro de la pantalla, con el tiempo de esa serie.
7. Historial de sesiones.

Quedan **fuera del MVP**: cuentas de usuario, sincronización, gráficas de progreso, cronómetro con la pantalla bloqueada, accesorios reutilizables (gomas predefinidas).

## Fases

| Fase | Contenido |
|---|---|
| **1** | App Android local con catálogo descargado de la API estática. Sin cuentas ni servidor propio. |
| **2** | Backend Spring Boot + MySQL, cuentas de usuario y sincronización (copia de seguridad y multidispositivo). |
| **3** | Cronómetro en segundo plano, gráficas de progreso, accesorios reutilizables y, si se decide publicar, revisión de licencia de los medios. |

## Documentación

- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md): capas, estructura de carpetas, flujos y reglas.
- [`docs/MODELO_DE_DATOS.md`](docs/MODELO_DE_DATOS.md): entidades de Room y comportamiento de las series.

## Compilar y ejecutar

Requisitos: Android Studio, Android SDK y un móvil con **depuración USB** (o inalámbrica) activada.

## Importante!

**Archivos de configuración de Gradle:** el esqueleto del proyecto (Gradle, el wrapper y las versiones de 
plugins y librerías) lo crea y mantiene Android Studio, no el agente. No crear, regenerar ni modificar por tu cuenta 
`gradlew`, `gradlew.bat`, la carpeta `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml` ni los `build.gradle.kts` (el de la raíz y el de `app`). Si una tarea necesita una dependencia
o un plugin nuevo, indícalo y propón la línea exacta a añadir, pero no la escribas en esos archivos: las versiones las confirma
el desarrollador desde la documentación oficial o desde Android Studio. El agente trabaja únicamente sobre el código Kotlin,
los recursos y la documentación.

## Distribución

Para que una versión actualice a la anterior (y no borre datos):

- Mismo `applicationId` siempre.
- Firmar siempre con la misma keystore (con copia de seguridad fuera del repositorio).
- Subir `versionCode` en cada versión.
- Si cambia el esquema de Room, subir la versión de la base de datos y escribir su migración.
