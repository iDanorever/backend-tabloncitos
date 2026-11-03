-- 1. Tabla de Usuarios y Roles (RBAC)
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL -- ADMIN, MESERO, COCINERO, CAJERO
);

-- 2. Tabla de Mesas del Restaurante
CREATE TABLE IF NOT EXISTS mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_mesa INT NOT NULL,
    zona VARCHAR(50), -- Salón Principal, Terraza, Mezzanine
    estado VARCHAR(20) DEFAULT 'LIBRE' -- LIBRE, OCUPADA, CUENTA_PEDIDA, RESERVADA
);

-- 3. Tabla de Categorías de Platos y Bebidas
CREATE TABLE IF NOT EXISTS categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- 4. Tabla de Productos (Platos, Menús, Bebidas)
CREATE TABLE IF NOT EXISTS productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    categoria_id INT,
    nombre VARCHAR(150) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    estacion_kds VARCHAR(50), -- Horno/Brasas, Barra, Cocina Fría
    activo BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- 5. Tabla Cabecera: Pedido / Venta (Maestro)
CREATE TABLE IF NOT EXISTS pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT NULL, -- Nulo si es un pedido de Takeout o Delivery
    mozo_id INT NOT NULL,
    cajero_id INT NULL,
    tipo_servicio VARCHAR(30), -- SALON, TAKEAWAY, DELIVERY
    estado_pedido VARCHAR(30) DEFAULT 'EN_COLA', -- EN_COLA, EN_PREPARACION, LISTO, ENTREGADO, PAGADO
    subtotal DECIMAL(10,2) NOT NULL,
    igv DECIMAL(10,2) NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    cliente_nombre VARCHAR(150),
    cliente_documento VARCHAR(20),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (mozo_id) REFERENCES usuarios(id),
    FOREIGN KEY (cajero_id) REFERENCES usuarios(id)
);

-- 6. Tabla Detalle: Detalle de Pedido (Detalle - Histórico con Precio Congelado)
CREATE TABLE IF NOT EXISTS pedido_detalles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL, -- Precio histórico congelado al momento de la venta
    subtotal DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(255), -- Ej: "3 Ají de Gallina - Menú 1, sin ají"
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
