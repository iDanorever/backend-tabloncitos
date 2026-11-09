package com.tabloncitos.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductoImagenStorageTest {

    @TempDir
    Path directorio;

    @Test
    void guardaLaImagenComoJpgEnLaCarpetaDeProductos() throws Exception {
        ProductoImagenStorage storage = new ProductoImagenStorage(directorio.toString());
        byte[] imagen = { 1, 2, 3 };

        storage.guardar(42L, imagen);

        assertArrayEquals(imagen, Files.readAllBytes(directorio.resolve("42.jpg")));
        try (Stream<Path> archivos = Files.list(directorio)) {
            assertEquals(1, archivos.count());
        }
    }

    @Test
    void reemplazaLaImagenExistenteDelProducto() throws Exception {
        ProductoImagenStorage storage = new ProductoImagenStorage(directorio.toString());
        storage.guardar(42L, new byte[] { 1 });

        storage.guardar(42L, new byte[] { 2, 3 });

        assertArrayEquals(new byte[] { 2, 3 }, Files.readAllBytes(directorio.resolve("42.jpg")));
        try (Stream<Path> archivos = Files.list(directorio)) {
            assertEquals(1, archivos.count());
        }
    }
}
