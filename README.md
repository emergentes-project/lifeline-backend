# Lifeline Backend

Backend modular de Lifeline, desarrollado con Spring Boot y Java 21.

## Base del proyecto

| Componente | Configuración |
|---|---|
| Spring Boot | 4.1.1, versión estable ofrecida por Spring Initializr al generar el proyecto |
| Java | 21 |
| Construcción | Maven mediante el wrapper incluido |
| Coordenadas Maven | `com.upc.pe:lifeline-backend:0.0.1-SNAPSHOT` |
| Paquete raíz | `com.upc.pe.lifeline` |
| Aplicación | `LifelineApplication` |
| Base de datos | PostgreSQL |

Dependencias iniciales: Spring Web MVC, Spring Data JPA, PostgreSQL Driver,
Validation y Lombok. El proyecto se generó con Spring Initializr.

## Compilar

Desde la raíz del repositorio, con JDK 21 disponible:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd -DskipTests compile
```

El wrapper descarga Maven y las dependencias en su primera ejecución.

Para construir el JAR ejecutable:

```powershell
.\mvnw.cmd -DskipTests package
```

El resultado se escribe en `target/lifeline-backend-0.0.1-SNAPSHOT.jar`.

### Certificados de Maven en Windows

Si la descarga de dependencias devuelve `PKIX path building failed` y Windows
ya reconoce el certificado de la conexión, Maven puede usar el almacén de
certificados de confianza del sistema durante la sesión de PowerShell:

```powershell
$env:MAVEN_OPTS = "$env:MAVEN_OPTS -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
.\mvnw.cmd -DskipTests package
```

Esta opción mantiene la comprobación de certificados HTTPS. Se utilizó para
construir la base inicial en el entorno de desarrollo actual.

## Conexión con PostgreSQL y ejecución

La conexión se configura mediante variables de entorno:

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC. Valor predeterminado: `jdbc:postgresql://localhost:5432/lifeline`. |
| `DB_USERNAME` | Usuario de PostgreSQL. Obligatorio. |
| `DB_PASSWORD` | Contraseña de PostgreSQL. Obligatorio. |

Ejemplo para PowerShell; sustituir los valores de usuario y contraseña:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/lifeline'
$env:DB_USERNAME = 'tu_usuario'
$env:DB_PASSWORD = 'tu_contrasena'

.\mvnw.cmd spring-boot:run
```

El arranque requiere una base PostgreSQL accesible y las variables configuradas.
La compilación y la construcción del JAR pueden realizarse sin conexión a la base.
Spring Boot utiliza el puerto 8080 de forma predeterminada.

La configuración principal está en `src/main/resources/application.yml`.
`ddl-auto: none` conserva el control del esquema: las tablas y sus cambios se
incorporarán al construir cada bounded context a partir de la estructura acordada.
Las credenciales se suministran mediante variables de entorno.

## Organización inicial

```text
src/main/java/com/upc/pe/lifeline/
├── LifelineApplication.java
├── iam/
│   └── package-info.java
├── casemanagement/
│   └── package-info.java
└── shared/
    └── package-info.java
```

- `iam`: cuentas, roles, autenticación y perfiles.
- `casemanagement`: recepción de consultas, casos, asignaciones e historial.
- `shared`: componentes técnicos y contratos compartidos del backend.

La siguiente etapa desarrolla las capas `domain`, `application`, `infrastructure`
e `interfaces` dentro de los contextos y sus entidades y repositorios PostgreSQL.
Los endpoints se incorporan con los flujos correspondientes.
