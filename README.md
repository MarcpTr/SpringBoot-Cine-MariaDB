# SpringBoot-Cine-MariaDB

API REST desarrollada con **Spring Boot** para consultar información de películas utilizando **TMDB (The Movie Database)** como fuente externa.

La aplicación incorpora persistencia en **MariaDB**, caché en memoria mediante **Caffeine**, actualización asíncrona de películas y un sistema de gestión de errores centralizado.


## Tecnologías

- Java 17
- Spring Boot 3.4.5
- Spring Web
- Spring WebFlux
- Spring Data JPA
- Hibernate
- MariaDB
- Caffeine
- Jakarta Validation
- Lombok
- Maven
- TMDB API

## Arquitectura

El proyecto sigue una arquitectura por capas:

```text
src/main/java/com/marcptr/cine
│
├── controller
│   └── TmdbController
│
├── service
│   ├── MovieService
│   ├── MovieAsyncService
│   ├── SearchService
│   └── TrendService
│
├── repository
│   ├── MovieRepository
│   └── SearchRepository
│
├── model
│   ├── Movie
│   ├── Search
│   └── ...
│
├── integration
│   └── tmdb
│       ├── client
│       │   └── TmdbClient
│       └── dto
│
├── dto
│   ├── common
│   ├── request
│   └── response
│
├── config
│   ├── CacheConfig
│   ├── AsyncConfig
│   └── Messages
│
└── exception
    └── GlobalExceptionHandler
```

El flujo general de una petición es:

```text
Cliente HTTP
     │
     ▼
Controller
     │
     ▼
Service
     │
     ├──────────────► Caffeine
     │
     ├──────────────► Repository ─────► MariaDB
     │
     └──────────────► TmdbClient ─────► TMDB
```

La aplicación intenta utilizar primero los datos locales para reducir el número de peticiones a TMDB.

---

## Funcionamiento

### Obtener una película

Endpoint:

```http
GET /api/data/movie/{id}
```

Ejemplo:

```http
GET /api/data/movie/550
```

El servicio genera una clave utilizando el ID de TMDB y el idioma:

```text
550-es-ES
```

A continuación sigue este proceso:

```text
                  ┌───────────────┐
                  │ Solicitud     │
                  │ de película   │
                  └───────┬───────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │ Caffeine Cache  │
                 └───────┬─────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
            existe                no existe
              │                     │
              ▼                     ▼
        comprobar edad       ┌───────────────┐
              │              │   MariaDB     │
              │              └───────┬───────┘
              │                      │
              │              ┌───────┴───────┐
              │              │               │
              │            existe          no existe
              │              │               │
              │              ▼               ▼
              │         actualizar       consultar
              │          caché            TMDB
              │                              │
              └──────────────┬───────────────┘
                             ▼
                       Respuesta API
```

### Política de actualización de películas

Las películas almacenadas tienen un `updatedAt`, que permite decidir cuándo es necesario consultar de nuevo TMDB.

| Antigüedad | Comportamiento |
|---|---|
| Menos de 7 días | Se devuelve el registro almacenado |
| 7-10 días | Se devuelve el registro y se inicia un refresco asíncrono |
| Más de 10 días | Se consulta TMDB antes de responder |
| Película marcada como no encontrada y < 1 día | Se devuelve `404` sin consultar TMDB |
| Película marcada como no encontrada y > 1 día | Se vuelve a intentar consultar TMDB |

Este comportamiento permite reducir considerablemente las peticiones externas.

### Actualización asíncrona

Cuando una película tiene entre 7 y 10 días de antigüedad, `MovieService` utiliza `MovieAsyncService`.

El método está marcado con:

```java
@Async
```

Por tanto, la petición HTTP no tiene que esperar a que termine la actualización.

El proceso es:

```text
Cliente
   │
   ├── recibe película almacenada
   │
   └── continúa inmediatamente
             │
             ▼
       MovieAsyncService
             │
             ▼
           TMDB
             │
             ▼
          MariaDB
             │
             ▼
       Caffeine Cache
```

Además, `refreshInProgress` evita iniciar varias actualizaciones simultáneas para la misma película e idioma.

---

## Búsqueda de películas

Endpoint:

```http
GET /api/data/search?query=batman&page=1
```

Parámetros:

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `query` | String | Sí | Texto de búsqueda |
| `page` | Integer | No | Página, entre 1 y 500 |

Ejemplo:

```http
GET /api/data/search?query=batman&page=1
```

La búsqueda utiliza `@Cacheable`:

```java
@Cacheable(
    value = "search",
    key = "#query + '-' + #page + '-' + #lang"
)
```

El flujo es:

```text
GET /search
     │
     ▼
Caffeine
     │
     ├── encontrado ──► respuesta
     │
     └── no encontrado
              │
              ▼
           MariaDB
              │
              ├── encontrado ──► respuesta
              │
              └── no encontrado
                       │
                       ▼
                      TMDB
                       │
                       ▼
                    MariaDB
                       │
                       ▼
                    respuesta
```

La caché de búsquedas tiene una duración de 6 horas.

---

## Películas en tendencia

Endpoint:

```http
GET /api/data/trending
```

Parámetros:

| Parámetro | Tipo | Valores | Descripción |
|---|---|---|---|
| `period` | Enum | `DAY`, `WEEK` | Periodo de tendencias |
| `page` | Integer | >= 1 | Página |

Ejemplo:

```http
GET /api/data/trending?period=DAY&page=1
```

Para tendencias semanales:

```http
GET /api/data/trending?period=WEEK&page=1
```

El controlador selecciona el servicio correspondiente:

```text
DAY
 └── TrendService.getTrendingDay()

WEEK
 └── TrendService.getTrendingWeek()
```

Las tendencias no se almacenan en MariaDB en este flujo. Se utilizan cachés Caffeine específicas.

```text
tmdbTrendingDay
    └── 20 minutos

tmdbTrendingWeek
    └── 4 horas
```

---

## Integración con TMDB

La comunicación externa está centralizada en:

```text
integration/tmdb/client/TmdbClient.java
```

Se utiliza `WebClient` para realizar las peticiones HTTP.

El cliente implementa operaciones para:

```text
/search/movie
/movie/{id}
/trending/movie/day
/trending/movie/week
```

Las respuestas de TMDB se mantienen como `JsonNode`, permitiendo almacenar el JSON prácticamente sin modificarlo.

Para obtener una película se solicitan también:

```text
videos
credits
```

mediante:

```text
append_to_response=videos,credits
```

### Gestión de errores de TMDB

Los errores externos se transforman en excepciones propias:

```text
TMDB 404
   └── TmdbNotFoundException

TMDB 4xx
   └── TmdbException(TMDB_CLIENT)

TMDB 5xx
   └── TmdbException(TMDB_SERVER)

Error de conexión
   └── TmdbException(TMDB_CONNECTION)

Error desconocido
   └── TmdbException(TMDB_UNKNOWN)
```

Las peticiones de películas y tendencias tienen un timeout de 5 segundos.

---

## Caché

La configuración está en:

```text
config/CacheConfig.java
```

Actualmente existen estas cachés:

| Caché | Tamaño máximo | Expiración |
|---|---:|---|
| `movies` | 1000 | 6 h desde el último acceso |
| `search` | 1000 | 6 h desde escritura |
| `tmdbTrendingDay` | 500 | 20 min |
| `tmdbTrendingWeek` | 500 | 4 h |
| `trends` | 100 | 15 min |

Se utiliza:

```text
Caffeine
```

junto con:

```java
@EnableCaching
```

Esto permite utilizar anotaciones como:

```java
@Cacheable
```

en los servicios.

---

## Persistencia

La aplicación utiliza:

```text
Spring Data JPA
        │
        ▼
     Hibernate
        │
        ▼
      MariaDB
```

### Tabla `movies`

El modelo `Movie` contiene:

```text
id
movieId
content
lang
updatedAt
lastAccessedAt
notFound
```

`content` se almacena como JSON:

```java
@JdbcTypeCode(SqlTypes.JSON)
private JsonNode content;
```

La clave primaria combina el ID de TMDB y el idioma.

Por ejemplo:

```text
550-es-ES
550-en-US
550-fr-FR
```

Esto permite almacenar diferentes versiones lingüísticas de una misma película.

### Tabla `searches`

El modelo `Search` almacena las respuestas de búsquedas realizadas:

```text
id
query
page
content
lang
updatedAt
lastAccessedAt
```

La respuesta de TMDB también se almacena como JSON.

---

## Internacionalización

El idioma utilizado para TMDB se obtiene del `Locale` de la petición:

```java
locale.getLanguage() + "-" + locale.getCountry()
```

Por ejemplo:

```text
es-ES
en-US
fr-FR
```

Los mensajes de error están internacionalizados mediante:

```text
messages.properties
messages_es_ES.properties
```

La resolución de mensajes se realiza mediante `MessageResolver`.

---

## Validación

Los parámetros de entrada utilizan Jakarta Validation.

Por ejemplo, una búsqueda requiere:

```java
@NotBlank
private String query;
```

y limita la página:

```java
@Min(1)
@Max(500)
private Integer page = 1;
```

Las tendencias requieren un periodo válido:

```java
@NotNull
private Period period = Period.DAY;
```

Los errores de validación son gestionados centralmente por `GlobalExceptionHandler`.

---

## Formato de respuesta

Las respuestas correctas utilizan `ApiResponse`.

Ejemplo:

```json
{
  "success": true,
  "data": {
    "content": {}
  },
  "error": null
}
```

Los errores siguen el mismo formato general:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TMDB_NOT_FOUND",
    "message": "Movie not found",
    "details": null
  }
}
```

Esto permite que el cliente pueda comprobar siempre:

```text
success
data
error
```

independientemente del endpoint.

---

## Configuración

La aplicación utiliza variables de entorno para la configuración sensible.

Ejemplo de configuración:

```properties
spring.application.name=Cine
server.port=8082

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver}

spring.jpa.hibernate.ddl-auto=update

tmdb.base-url=${TMDB_BASE_URL}
tmdb.api-key=${TMDB_API_CLIENT}
```

Por tanto, antes de ejecutar la aplicación hay que configurar al menos:

```text
DB_URL
DB_USERNAME
TMDB_BASE_URL
TMDB_API_CLIENT
```

La URL de la base de datos debe tener el formato correspondiente al driver de MariaDB, por ejemplo:

```text
jdbc:mariadb://localhost:3306/cine
```

---

## Requisitos

Necesitas:

- Java 17 o superior
- Maven, o utilizar el Maven Wrapper incluido
- MariaDB
- Una API Key de TMDB

Comprueba Java:

```bash
java -version
```

---

## Instalación

Clonar el repositorio:

```bash
git clone <URL_DEL_REPOSITORIO>
cd SpringBoot-Cine-MariaDB
```

Configurar MariaDB y crear la base de datos:

```sql
CREATE DATABASE cine;
```

Configurar las variables de entorno.

Linux/macOS:

```bash
export DB_URL="jdbc:mariadb://localhost:3306/cine"
export DB_USERNAME="root"
export TMDB_BASE_URL="https://api.themoviedb.org/3"
export TMDB_API_CLIENT="TU_API_KEY"
```

En Windows PowerShell:

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/cine"
$env:DB_USERNAME="root"
$env:TMDB_BASE_URL="https://api.themoviedb.org/3"
$env:TMDB_API_CLIENT="TU_API_KEY"
```

También puedes utilizar un archivo de configuración local si prefieres no establecer las variables manualmente.

---

## Ejecutar el proyecto

Con Maven Wrapper:

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Con Maven instalado:

```bash
mvn spring-boot:run
```

La aplicación estará disponible en:

```text
http://localhost:8082
```

---

## Compilar

```bash
./mvnw clean package
```

El JAR generado estará en:

```text
target/
```

Para ejecutarlo:

```bash
java -jar target/cine-0.0.1-SNAPSHOT.jar
```

---

## Ejemplos de uso

### Obtener una película

```bash
curl "http://localhost:8082/api/data/movie/550"
```

### Buscar películas

```bash
curl "http://localhost:8082/api/data/search?query=batman&page=1"
```

### Tendencias diarias

```bash
curl "http://localhost:8082/api/data/trending?period=DAY&page=1"
```

### Tendencias semanales

```bash
curl "http://localhost:8082/api/data/trending?period=WEEK&page=1"
```

---

## Flujo completo de una petición

Por ejemplo, para:

```http
GET /api/data/movie/550
```

el proceso es:

```text
1. TmdbController recibe la petición
                │
                ▼
2. Obtiene el Locale
                │
                ▼
3. MovieService.getMovie(550, "es-ES")
                │
                ▼
4. Busca "550-es-ES" en Caffeine
                │
        ┌───────┴────────┐
        │                │
     existe           no existe
        │                │
        │                ▼
        │        Busca en MariaDB
        │                │
        │        ┌───────┴────────┐
        │        │                │
        │     existe           no existe
        │        │                │
        │        │                ▼
        │        │           TmdbClient
        │        │                │
        │        │                ▼
        │        │               TMDB
        │        │                │
        │        │                ▼
        │        │             MariaDB
        │        │                │
        └────────┴────────────────┘
                         │
                         ▼
                    Caffeine
                         │
                         ▼
                    ApiResponse
                         │
                         ▼
                       JSON
```

El objetivo es que las peticiones repetidas no tengan que llegar hasta TMDB.

---

## Ventajas del diseño

### Reducción de peticiones externas

La caché evita consultar TMDB repetidamente para los mismos datos.

### Persistencia

Los datos importantes sobreviven a un reinicio de la aplicación gracias a MariaDB.

### Caché en dos niveles

Para películas y búsquedas se utiliza una combinación de:

```text
Caffeine
   ↓
MariaDB
   ↓
TMDB
```

### Actualización progresiva

Las películas no se actualizan todas de forma inmediata. Dependiendo de su antigüedad, se utilizan los datos existentes o se programa una actualización.

### Actualizaciones asíncronas

El refresco de películas entre 7 y 10 días se ejecuta mediante `@Async`, evitando bloquear la respuesta del usuario.

### Separación de responsabilidades

Cada capa tiene una función concreta:

```text
Controller   → HTTP
Service      → lógica de negocio
Repository   → persistencia
TmdbClient   → API externa
Model        → datos
DTO          → entrada/salida HTTP
Config       → configuración transversal
Exception    → gestión de errores
```

---

## Estructura de paquetes

```text
com.marcptr.cine
│
├── config
│   ├── AsyncConfig.java
│   ├── CacheConfig.java
│   ├── DataInitializer.java
│   └── Messages.java
│
├── controller
│   └── TmdbController.java
│
├── dto
│   ├── common
│   ├── request
│   └── response
│
├── exception
│   ├── GlobalExceptionHandler.java
│   └── tmdb
│
├── integration
│   └── tmdb
│       ├── client
│       └── dto
│
├── model
│   ├── Movie.java
│   ├── Search.java
│   └── enums
│
├── repository
│   ├── MovieRepository.java
│   └── SearchRepository.java
│
├── service
│   ├── MovieService.java
│   ├── MovieAsyncService.java
│   ├── SearchService.java
│   └── TrendService.java
│
└── utils
    └── MessageResolver.java
```