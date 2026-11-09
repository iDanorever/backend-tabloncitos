SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS pedido_detalles;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS promociones;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;
DROP TABLE IF EXISTS mesas;
DROP TABLE IF EXISTS empleados;
DROP TABLE IF EXISTS empleado_bajas;
DROP TABLE IF EXISTS usuarios;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Tabla de Usuarios y Roles (RBAC)
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    dni VARCHAR(20),
    sede VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    otp_generado VARCHAR(10),
    estado_solicitud_otp VARCHAR(20)
);

CREATE TABLE empleado_bajas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fuente VARCHAR(20) NOT NULL,
    id_original BIGINT NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    dni VARCHAR(20),
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    sede VARCHAR(100),
    motivo VARCHAR(255) NOT NULL,
    eliminado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de empleados administrados desde el panel
CREATE TABLE empleados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    dni VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    sede VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    otp_generado VARCHAR(10),
    estado_solicitud_otp VARCHAR(20)
);

-- 3. Tabla de Mesas del Restaurante
CREATE TABLE mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_mesa INT NOT NULL,
    zona VARCHAR(50),
    estado VARCHAR(20) DEFAULT 'LIBRE',
    reserva_nombre VARCHAR(120),
    reserva_telefono VARCHAR(30),
    reserva_fecha_hora DATETIME,
    reserva_personas INT,
    ocupada_desde DATETIME,
    mozo_responsable VARCHAR(100),
    ultima_comanda_enviada DATETIME
);

-- 4. Tabla de Categorías de Platos y Bebidas
CREATE TABLE categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- 5. Tabla de Productos (Platos, Menús, Bebidas)
CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    categoria_id INT,
    nombre VARCHAR(150) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    estacion_kds VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE,
    tiene_imagen BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE promociones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    tipo_beneficio VARCHAR(20) NOT NULL,
    valor_beneficio DECIMAL(10,2) NOT NULL,
    producto_id INT NOT NULL,
    precio_original DECIMAL(10,2) NOT NULL,
    dias_semana VARCHAR(100) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    canales VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- 6. Tabla Cabecera: Pedido / Venta (Maestro)
CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT NULL,
    mozo_id INT NOT NULL,
    cajero_id INT NULL,
    tipo_servicio VARCHAR(30),
    estado_pedido VARCHAR(30) DEFAULT 'EN_COLA',
    subtotal DECIMAL(10,2) NOT NULL,
    igv DECIMAL(10,2) NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    cliente_nombre VARCHAR(150),
    cliente_documento VARCHAR(20),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_preparacion DATETIME,
    fecha_fin_preparacion DATETIME,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (mozo_id) REFERENCES usuarios(id),
    FOREIGN KEY (cajero_id) REFERENCES usuarios(id)
);

-- 7. Tabla Detalle: Detalle de Pedido
CREATE TABLE pedido_detalles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(255),
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);