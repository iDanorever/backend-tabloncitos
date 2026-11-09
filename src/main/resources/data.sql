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
(3, 3, 'Salón Principal', 'LIBRE'),
(4, 4, 'Salón Principal', 'CUENTA_PEDIDA'),
(5, 5, 'Salón Principal', 'LIBRE'),
(6, 6, 'Terraza', 'LIBRE'),
(7, 7, 'Terraza', 'RESERVADA'),
(8, 8, 'Terraza', 'LIBRE'),
(9, 9, 'Terraza', 'OCUPADA'),
(10, 10, 'Terraza', 'LIBRE');

-- Mantiene numeracion global M-01 a M-10 en bases que ya tenian las mesas cargadas.
UPDATE mesas SET numero_mesa = id WHERE id BETWEEN 1 AND 10;

-- Inserción de Categorías
-- Actualiza los nombres de las categorías de prueba existentes sin sobrescribir cambios posteriores.
UPDATE categorias SET nombre = 'Brasas & Pollos' WHERE id = 1 AND nombre = 'Pollos a la Brasa';
UPDATE categorias SET nombre = 'Parrillas & Cortes' WHERE id = 2 AND nombre = 'Menús Ejecutivos';
UPDATE categorias SET nombre = 'Bebidas' WHERE id = 3 AND nombre = 'Bebidas y Refrescos';

INSERT IGNORE INTO categorias (id, nombre) VALUES 
(1, 'Brasas & Pollos'),
(2, 'Parrillas & Cortes'),
(3, 'Bebidas'),
(4, 'Guarniciones');

-- Migra los platos de ejemplo que antes estaban en Menús Ejecutivos.
UPDATE productos
SET categoria_id = 2, nombre = 'Anticuchos 3 Palos', precio = 32.00, estacion_kds = 'Horno/Brasas'
WHERE id = 3 AND nombre = 'Menú 1: Ají de Gallina Tradicional';
UPDATE productos
SET categoria_id = 2, nombre = 'Parrilla Mixta Personal', precio = 49.50, estacion_kds = 'Horno/Brasas'
WHERE id = 4 AND nombre = 'Menú 2: Seco de Res con Frijoles';

-- Inserción idempotente de productos iniciales: cinco por categoría.
INSERT IGNORE INTO productos (id, categoria_id, nombre, precio, estacion_kds, activo) VALUES 
(1, 1, '1 Pollo Tradicional a la Brasa (Papas + Ensalada)', 78.00, 'Horno/Brasas', TRUE),
(2, 1, '1/2 Pollo a la Brasa (Papas + Ensalada)', 44.50, 'Horno/Brasas', TRUE),
(3, 2, 'Anticuchos 3 Palos (Papas + Ensalada)', 32.00, 'Horno/Brasas', TRUE),
(4, 2, 'Parrilla Mixta Personal', 49.50, 'Horno/Brasas', TRUE),
(5, 3, 'Jarra Chicha Morada 1 Litro', 16.00, 'Barra', TRUE),
(6, 1, '1/4 Pollo a la Brasa (Papas + Ensalada)', 26.00, 'Horno/Brasas', TRUE),
(7, 1, 'Alitas a la Brasa BBQ', 29.00, 'Horno/Brasas', TRUE),
(8, 1, 'Pollo a la Brasa Familiar', 89.00, 'Horno/Brasas', TRUE),
(9, 2, 'Costillas BBQ a la Parrilla', 42.00, 'Horno/Brasas', TRUE),
(10, 2, 'Lomo Fino a la Parrilla', 45.00, 'Horno/Brasas', TRUE),
(11, 2, 'Churrasco a la Parrilla', 38.00, 'Horno/Brasas', TRUE),
(12, 3, 'Limonada Frozen', 12.00, 'Barra', TRUE),
(13, 3, 'Inca Kola Personal', 5.00, 'Barra', TRUE),
(14, 3, 'Coca-Cola 1 Litro', 9.00, 'Barra', TRUE),
(15, 3, 'Agua Mineral Sin Gas', 4.00, 'Barra', TRUE),
(16, 4, 'Papas Fritas', 10.00, 'Cocina Caliente', TRUE),
(17, 4, 'Ensalada Fresca', 8.00, 'Cocina Fría', TRUE),
(18, 4, 'Yuca Frita', 10.00, 'Cocina Caliente', TRUE),
(19, 4, 'Arroz Blanco', 5.00, 'Cocina Caliente', TRUE),
(20, 4, 'Choclo con Queso', 12.00, 'Cocina Caliente', TRUE);