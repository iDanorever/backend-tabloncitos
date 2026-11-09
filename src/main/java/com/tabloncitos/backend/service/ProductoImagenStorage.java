package com.tabloncitos.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class ProductoImagenStorage {

    private final Path directorio;

    public ProductoImagenStorage(
            @Value("${app.product-images-dir:src/main/resources/static/images/productos}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public Path getDirectorio() {
        return directorio;
    }

    public void guardar(Long productoId, byte[] imagen) throws IOException {
        Files.createDirectories(directorio);
        Path archivoTemporal = Files.createTempFile(directorio, "producto-" + productoId + "-", ".tmp");
        try {
            Files.write(archivoTemporal, imagen);
            Path destino = directorio.resolve(productoId + ".jpg").normalize();
            if (!destino.getParent().equals(directorio)) {
                throw new IllegalArgumentException("El identificador del plato no es válido");
            }
            try {
                Files.move(archivoTemporal, destino, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(archivoTemporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(archivoTemporal);
        }
    }
}
