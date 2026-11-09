<div align="center">

  <!-- Tecnologías Principales -->
  <img src="https://img.shields.io/badge/Java-17-%23ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-%236DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MySQL-8.0-%234479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  
  <!-- Frontend & Estilos -->
  <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML5">
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript">
  <img src="https://img.shields.io/badge/Tailwind_CSS-38BDF8?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS">

  <!-- Herramientas y Despliegue -->
  <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white" alt="Git">
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven">

</div>

<br>

# 🍗 Tabloncitos — Sistema de Gestión de Restaurante & KDS

**Tabloncitos** es un sistema de gestión integral de alta fidelidad para restaurantes de pollos y parrillas, diseñado bajo una arquitectura limpia y táctil. Su objetivo principal es maximizar la eficiencia operativa conectando en tiempo real al **administrador**, los **mozos** y la **cocina**.

---

## 🚀 Arquitectura & Tech Stack

### **Backend (Core System)**
* **Lenguaje & Framework:** Java 17 + Spring Boot 3
* **Persistencia & BD:** Spring Data JPA / JDBC / MySQL 8.0 (MariaDB / XAMPP)
* **Seguridad & Acceso:** Autenticación OTP y Control de Acceso Basado en Roles (RBAC)
* **API REST:** Estructura JSON para la comunicación reactiva entre clientes frontend y backend

### **Frontend (UI Táctil de Alta Eficiencia)**
* **Estructura & Estilos:** HTML5, JavaScript (ES6+) y Tailwind CSS con paleta industrial en Modo Oscuro (*Stitch UI Design*)
* **Motor Gráfico & Iconografía:** Google Material Symbols & Fonts (*Plus Jakarta Sans*, *JetBrains Mono*)
* **Micro-Interacciones:** Audio-alerts con Web Audio API y sincronización reactiva por polling

---

## 🗄️ Modelo de Datos (MySQL / MariaDB)

El sistema maneja una arquitectura de pedidos en esquema **Maestro / Detalle** y relaciones de integridad estricta:

* **`usuarios` / `empleados`**: Credenciales, roles (`ADMIN`, `MESERO`, `COCINERO`) y estado de baja lógica (*soft-delete*).
* **`mesas`**: `id`, `numero_mesa`, `zona` (*Salón Principal*, *Terraza*, *VIP*), `estado` (`LIBRE`, `OCUPADA`, `SERVIDA`, `CUENTA_PEDIDA`, `RESERVADA`).
* **`productos` & `promociones`**: Catálogo con código PLU, precios unitarios, categoría y estado de disponibilidad.
* **`pedidos` (Maestro)**: Cabecera de comanda (`#COM-XXXX`), ID de mesa, mozo asignado, estado (`ABIERTA`, `EN_COCINA`, `DESPACHADA`) y total acumulado.
* **`pedido_detalles` (Detalle)**: Ítems asociados, cantidad, precio unitario, subtotal y especificaciones/notas táctiles de cocina (*kds_remarks*).

---

## ⚙️ Funcionalidades por Bloques y Módulos

### 🛡️ Bloque 1 & 2: Administración, Seguridad & Personal
* **Autenticación & RBAC:** Control de acceso y derivación de vistas según el rol activo.
* **Gestión de Personal:** Alta, edición y bajas lógicas de empleados manteniendo la trazabilidad histórica de transacciones.
* **Configuración de BD:** Gestión segura de restricciones de clave foránea (`SET FOREIGN_KEY_CHECKS`) para mantenimiento y scripts de arranque automáticos.

### 🍽️ Bloque 3: Operación de Salón & Atención al Cliente
* **11. Mapa de Disponibilidad de Mesas:** Grilla interactiva en tiempo real sincronizada con la base de datos para la gestión visual del salón y cambios de estado.
* **12. Toma de Pedidos (Comandas Maestro/Detalle):** Interfaz táctil de alta velocidad para la selección de platos por categoría, personalización de guarniciones/términos de cocción, cálculo de subtotal/IGV en vivo y transmisión directa a cocina.
* **13. Monitor de Entrega en Salón:** Pantalla de alertas visuales y sonoras para notify al mozo cuando una bandeja está lista en el pasaplatos, con registro e historial de tiempo de servicio.
* **15. Mi Rendimiento:** Dashboard táctico para mozos con métricas clave de eficiencia y comandas servidas en la jornada.

### 🍳 Bloque 4: Producción & Cocina (KDS)
* **16. Cola de Cocina General (KDS):** Pantalla operativa para cocineros con comandas organizadas por tiempo de espera (alertas SLA), control táctil por ítem (`Pendiente`, `En Cocción` / `En Fuego`, `Listo`) y despacho directo hacia el pasaplatos de salón.
* **19. Dashboard de Rendimiento de Cocina:** Panel analítico para monitorear tiempos promedio de salida, mermas e incidencias de producción.

## 🔌 Pruebas de Endpoints y Backend (API REST)

Como parte del desarrollo y validación de los servicios backend en Spring Boot (con conexión a MySQL/MariaDB), se documentaron y probaron las rutas principales de consultas e inserciones (`GET`/`POST`).

| Documentación de Endpoints | Evidencia de Pruebas Postman / Consola |
| :---: | :---: |
| [Ver Documentación txt](./trabajo-antes-backend/Documentacion_Endpoints.txt) | Pruebas de conectividad y respuestas JSON |

### Capturas de Pruebas:

<div align="center">
  <img src="./trabajo-antes-backend/prueba1.png" width="45%" alt="Prueba Endpoint 1" />
  <img src="./trabajo-antes-backend/prueba2.png" width="45%" alt="Prueba Endpoint 2" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./trabajo-antes-backend/prueba3.png" width="45%" alt="Prueba Endpoint 3" />
  <img src="./trabajo-antes-backend/prueba4.png" width="45%" alt="Prueba Endpoint 4" />
</div>


---

## 🖥️ Evidencia Visual de Pantallas e Interfaz (Frontend)

Se desarrollaron y validaron 14 pantallas interactivas alineadas a la arquitectura visual táctil (*Stitch UI Design*) para cubrir los flujos de administración, salón y cocina:

<div align="center">
  <img src="./pruebas-frontend/prueba1.1.png" width="45%" alt="Pantalla 1" />
</div>

<div align="center">
  <img src="./pruebas-frontend/pruebas1.21.png" width="45%" alt="Pantalla 2" />
  <img src="./pruebas-frontend/pruebas1.22.png" width="45%" alt="Pantalla 3" />
</div>

<div align="center">
  <img src="./pruebas-frontend/pruebas1.23.png" width="45%" alt="Pantalla 4" />
  <img src="./pruebas-frontend/prueba1.2.png" width="45%" alt="Pantalla 5" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.3.png" width="45%" alt="Pantalla 6" />
  <img src="./pruebas-frontend/prueba1.4.png" width="45%" alt="Pantalla 7" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.5.png" width="45%" alt="Pantalla 8" />
  <img src="./pruebas-frontend/prueba1.6.png" width="45%" alt="Pantalla 9" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.7.png" width="45%" alt="Pantalla 10" />
  <img src="./pruebas-frontend/prueba1.8.png" width="45%" alt="Pantalla 11" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.9.png" width="45%" alt="Pantalla 12" />
  <img src="./pruebas-frontend/prueba1.10.png" width="45%" alt="Pantalla 13" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.11.png" width="45%" alt="Pantalla 14" />
  <img src="./pruebas-frontend/prueba1.12.png" width="45%" alt="Pantalla 15" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.13.png" width="45%" alt="Pantalla 16" />
  <img src="./pruebas-frontend/prueba1.14.png" width="45%" alt="Pantalla 17" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.15.png" width="45%" alt="Pantalla 18" />
  <img src="./pruebas-frontend/prueba1.16.png" width="45%" alt="Pantalla 19" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.17.png" width="45%" alt="Pantalla 20" />
  <img src="./pruebas-frontend/prueba1.18.png" width="45%" alt="Pantalla 21" />
</div>

<div align="center" style="margin-top: 10px;">
  <img src="./pruebas-frontend/prueba1.19.png" width="45%" alt="Pantalla 22" />
  <img src="./pruebas-frontend/prueba1.20.png" width="45%" alt="Pantalla 23" />
</div>

---

## 🛠️ Instalación y Puesta en Marcha

### **Requisitos Previos**
* Java JDK 17 o superior
* Apache Maven 3.x
* MySQL 8.0 o XAMPP (MariaDB)

### **Pasos para la Ejecución**

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/iDanorever/Tabloncitos.git](https://github.com/iDanorever/Tabloncitos.git)
   cd Tabloncitos
   
2. **RUN**

 debes activar el XAMPP y activar el mysql y Apache una vez creado entrar al phpmyadmi de mySQL y ejecutar en SQL esto:
```bash
CREATE DATABASE db_tabloncitos;
y luego ejecutar en la terminal de Visual Estudio:
```bash
    .\mvnw spring-boot:run


---

---

## 🔒 Control de Integridad y Registro de Entorno (Auditoría de Autoría)

Este repositorio y su estructura de archivos contienen **marcas de trazabilidad de compilación (*Environment Fingerprinting*)** en sus scripts de inicialización y manejadores de sesión.

* **Registro de Despliegue y Clonación:** La ejecución del script de arranque (`mvnw` / controladores `Spring Boot`) fuera del entorno de desarrollo registrado (`iDanorever`) genera un reporte de telemetría local y registro de IP de origen al momento de compilar y levantar los recursos del sistema.
* **Licencia de Uso Académico Privado:** Queda estrictamente prohibida la clonación, modificación o presentación de este proyecto por parte de terceros en evaluaciones o entregas académicas. Cualquier instancia duplicada detectada sin la firma de autoría original será notificada automáticamente al repositorio principal.
