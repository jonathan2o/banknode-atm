# BankNode-ATM

Sistema de Cajero Automático (ATM) desarrollado con **Java 17**, **Spring Boot 3.2.x**, **Spring Data JPA** con base de datos **H2 en memoria** e interfaz responsiva en **Thymeleaf + Bootstrap 5**.

---

## 🏛️ Arquitectura y Principios SOLID

1. **Single Responsibility Principle (SRP)**:
   - **`Cliente`**: Entidad JPA para la persistencia del cliente, saldo, estado de seguridad y métodos atómicos (`consultarSaldo()`, `consignar()`, `retirar()`).
   - **`Cajero`**: Entidad JPA que gestiona el inventario de billetes ($10k, $20k, $50k, $100k) y el algoritmo de verificación y entrega de efectivo.
   - **`ClienteService`**: Lógica de negocio de autenticación, control de intentos fallidos, bloqueo de 10 minutos y transferencias.
   - **`CajeroService`**: Coordinación transaccional entre el saldo de la cuenta y el arqueo físico de billetes del cajero.
   - **`AtmController`**: Orquestación web MVC y control de sesión.

2. **Open/Closed Principle (OCP)** y **Dependency Inversion Principle (DIP)**:
   - El controlador depende de interfaces de servicio (`ClienteService`, `CajeroService`).

3. **Interface Segregation Principle (ISP)**:
   - Repositorios independientes con interfaces limpias de Spring Data JPA.

---

## 📁 Estructura del Proyecto

```text
banknode-atm/
├── pom.xml                                               # Dependencias Maven (Web, JPA, Thymeleaf, H2)
├── README.md                                             # Documentación técnica y guía de usuario
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/banknode/atm/
    │   │   ├── BanknodeAtmApplication.java               # Punto de entrada y carga de datos semilla (3 clientes)
    │   │   ├── controller/
    │   │   │   └── AtmController.java                    # Controlador de las 3 pantallas y acciones
    │   │   ├── exception/
    │   │   │   └── AtmException.java                     # Excepción de dominio y reglas de negocio
    │   │   ├── model/
    │   │   │   ├── Cajero.java                           # Inventario de billetes ($10k, $20k, $50k, $100k)
    │   │   │   └── Cliente.java                          # Entidad JPA con métodos de saldo, consignar y retirar
    │   │   ├── repository/
    │   │   │   ├── CajeroRepository.java                 # Persistencia H2 para el cajero
    │   │   │   └── ClienteRepository.java                # Búsqueda de clientes por documento
    │   │   └── service/
    │   │       ├── CajeroService.java                    # Contrato de operaciones de efectivo
    │   │       ├── ClienteService.java                   # Contrato de autenticación y transferencias
    │   │       └── impl/
    │   │           ├── CajeroServiceImpl.java
    │   │           └── ClienteServiceImpl.java
    │   └── resources/
    │       ├── application.properties                    # H2 en memoria (jdbc:h2:mem:banknodedb)
    │       ├── static/
    │       │   ├── css/atm.css                           # Diseño moderno estilo kiosco de cajero
    │       │   └── js/atm.js                             # Temporizador de inactividad de 180s y atajos
    │       └── templates/
    │           ├── disponibilidad.html                   # Pantalla 1: Arqueo de billetes y total disponible
    │           ├── login.html                            # Pantalla 2: Login con documento y PIN de 4 dígitos
    │           └── operaciones.html                      # Pantalla 3: Saldo, retiros, transferencias y salida
    └── test/
        └── java/com/banknode/atm/
            └── BanknodeAtmApplicationTests.java          # Pruebas automatizadas de todas las reglas
```

---

## 🔒 Reglas de Negocio Implementadas

1. **Bloqueo por 3 Intentos Fallidos de PIN**:
   - Al tercer PIN incorrecto consecutivo, la cuenta se bloquea exactamente por **10 minutos** (`bloqueadoHasta = now + 10 min`).
   - Si el usuario intenta entrar mientras está bloqueada, el sistema informa los minutos restantes. Al transcurrir el tiempo, se desbloquea de forma automática.
   - Un PIN exitoso reinicia inmediatamente el contador de intentos fallidos.
2. **Dispensación Inteligente de Billetes**:
   - El cajero valida existencias de billetes de $100k, $50k, $20k y $10k.
   - El retiro solo se procesa si el cajero dispone de la combinación exacta para el monto solicitado (múltiplos de $10.000 COP).
3. **Transferencias Atómicas**:
   - Se valida la existencia del documento destino y fondos suficientes en la cuenta origen.
4. **Auto-Cierre de Sesión por Inactividad (180 segundos)**:
   - La pantalla de operaciones cuenta con un reloj regresivo visible (`03:00`).
   - Si el usuario no realiza ninguna acción durante 180 segundos, la sesión se destruye automáticamente y redirige al login con notificación.

---

## 👥 Cuentas de Prueba Precargadas

Al iniciar la aplicación, `BanknodeAtmApplication.java` inicializa automáticamente:

| Documento | Titular | PIN | Saldo Inicial (COP) |
| :---: | :---: | :---: | :---: |
| **1001** | Carlos Mendoza | `1234` | $1.500.000 |
| **1002** | Laura Gómez | `4321` | $850.000 |
| **1003** | Andrés Restrepo | `9876` | $2.300.000 |

*Inventario inicial del Cajero*: 100 billetes de $10k, 50 de $20k, 50 de $50k y 40 de $100k = **$8.500.000 COP** en efectivo.

---

## 🖥️ Pantallas del Cajero

1. **Pantalla 1 - Disponibilidad de Billetes (`/disponibilidad`)**:
   Muestra el arqueo de billetes desglosado por cada una de las 4 denominaciones y el total en efectivo.
2. **Pantalla 2 - Autenticación / Login (`/login`)**:
   Ingreso seguro con número de documento y PIN de 4 dígitos.
3. **Pantalla 3 - Menú de Operaciones (`/operaciones`)**:
   Consulta de saldo en tiempo real, retiro rápido con botones de atajo, retiro personalizado, transferencia a otras cuentas, botón de salida y cuenta regresiva de 180 segundos de inactividad.

---

## 🚀 Comandos de Ejecución por Terminal

Desde el directorio del proyecto `banknode-atm/`:

### 1. Ejecutar las Pruebas Automatizadas
```powershell
mvn clean test
```

### 2. Iniciar el Servidor de Aplicaciones
```powershell
mvn spring-boot:run
```

### 3. Empaquetar y Ejecutar JAR
```powershell
mvn clean package
java -jar target/banknode-atm-1.0.0.jar
```

Acceso al navegador web: **[http://localhost:8080/](http://localhost:8080/)**
Consola H2 en memoria: **[http://localhost:8080/h2-console](http://localhost:8080/h2-console)** (JDBC URL: `jdbc:h2:mem:banknodedb`)
