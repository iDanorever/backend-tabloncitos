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
