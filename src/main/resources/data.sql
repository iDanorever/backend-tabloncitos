-- Insercion de Usuarios de Prueba (Roles)
INSERT IGNORE INTO usuarios (id, nombre, correo, password, rol) VALUES 
(1, 'Roberto Mendoza', 'cajero@tabloncitos.com', '123456', 'CAJERO'),
(2, 'Sara Jiménez', 'mesero@tabloncitos.com', '123456', 'MESERO'),
(3, 'Marco Huamán', 'cocina@tabloncitos.com', '123456', 'COCINERO'),
(4, 'Admin General', 'admin@tabloncitos.com', '123456', 'ADMIN');

-- Inserción de Mesas Iniciales
INSERT IGNORE INTO mesas (id, numero_mesa, zona, estado) VALUES 
(1, 1, 'Salón Principal', 'LIBRE'),
(2, 2, 'Salón Principal', 'OCUPADA'),
(3, 3, 'Terraza', 'LIBRE'),
(4, 4, 'Salón Principal', 'CUENTA_PEDIDA');

-- Inserción de Categorías
INSERT IGNORE INTO categorias (id, nombre) VALUES 
(1, 'Pollos a la Brasa'),
(2, 'Menús Ejecutivos'),
(3, 'Bebidas y Refrescos');

-- Inserción de Productos (Platos y Menus)
INSERT IGNORE INTO productos (id, categoria_id, nombre, precio, estacion_kds, activo) VALUES 
(1, 1, '1 Pollo Tradicional a la Brasa (Papas + Ensalada)', 78.00, 'Horno/Brasas', TRUE),
(2, 1, '1/2 Pollo a la Brasa', 44.50, 'Horno/Brasas', TRUE),
(3, 2, 'Menú 1: Ají de Gallina Tradicional', 18.00, 'Cocina Caliente', TRUE),
(4, 2, 'Menú 2: Seco de Res con Frijoles', 20.00, 'Cocina Caliente', TRUE),
(5, 3, 'Jarra Chicha Morada 1 Litro', 16.00, 'Barra', TRUE);