package com.tabloncitos.backend.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AdminSchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        ensureColumn("usuarios", "codigo", "VARCHAR(20) NULL");
        ensureColumn("usuarios", "dni", "VARCHAR(20) NULL");
        ensureColumn("usuarios", "sede", "VARCHAR(100) NULL");
        ensureColumn("empleados", "dni", "VARCHAR(20) NULL");
        ensureColumn("usuarios", "activo", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("usuarios", "otp_generado", "VARCHAR(10) NULL");
        ensureColumn("usuarios", "estado_solicitud_otp", "VARCHAR(20) NULL");
        ensureColumn("mesas", "reserva_nombre", "VARCHAR(120) NULL");
        ensureColumn("mesas", "reserva_telefono", "VARCHAR(30) NULL");
        ensureColumn("mesas", "reserva_fecha_hora", "DATETIME NULL");
        ensureColumn("mesas", "reserva_personas", "INT NULL");
        ensureColumn("mesas", "ocupada_desde", "DATETIME NULL");
        ensureColumn("mesas", "mozo_responsable", "VARCHAR(100) NULL");
        ensureColumn("mesas", "ultima_comanda_enviada", "DATETIME NULL");
        ensureColumn("pedidos", "fecha_inicio_preparacion", "DATETIME NULL");
        ensureColumn("pedidos", "fecha_fin_preparacion", "DATETIME NULL");
        ensureColumn("productos", "tiene_imagen", "BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.update("""
                UPDATE mesas SET ocupada_desde = CURRENT_TIMESTAMP
                WHERE ocupada_desde IS NULL AND estado IN ('OCUPADA', 'SERVIDA', 'CUENTA_PEDIDA')
                """);
        jdbcTemplate.update(
                "UPDATE usuarios SET codigo = CONCAT('EMP-', LPAD(id, 4, '0')) WHERE codigo IS NULL OR codigo = ''");
        ensureUniqueIndex("usuarios", "uk_usuarios_codigo", "codigo");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS empleado_bajas (
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
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS promociones (
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
                    CONSTRAINT fk_promociones_producto
                        FOREIGN KEY (producto_id) REFERENCES productos(id)
                )
                """);
    }

    private void ensureColumn(String table, String column, String definition) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet columns = connection.getMetaData().getColumns(
                        connection.getCatalog(), null, table, column)) {
            if (!columns.next()) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    private void ensureUniqueIndex(String table, String indexName, String column) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet indexes = connection.getMetaData().getIndexInfo(
                        connection.getCatalog(), null, table, true, false)) {
            while (indexes.next()) {
                if (column.equalsIgnoreCase(indexes.getString("COLUMN_NAME"))) {
                    return;
                }
            }
        }
        jdbcTemplate.execute("CREATE UNIQUE INDEX " + indexName + " ON " + table + " (" + column + ")");
    }
}
