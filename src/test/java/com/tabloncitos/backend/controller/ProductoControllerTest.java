package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.ProductoCrearRequest;
import com.tabloncitos.backend.model.Categoria;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.repository.CategoriaRepository;
import com.tabloncitos.backend.repository.ProductoRepository;
import com.tabloncitos.backend.service.ProductoImagenStorage;
import com.tabloncitos.backend.service.PromocionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ProductoControllerTest {

    private ProductoRepository productoRepository;
    private CategoriaRepository categoriaRepository;
    private PromocionService promocionService;
    private ProductoImagenStorage productoImagenStorage;
    private ProductoController controller;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        categoriaRepository = mock(CategoriaRepository.class);
        promocionService = mock(PromocionService.class);
        productoImagenStorage = mock(ProductoImagenStorage.class);
        controller = new ProductoController(
                productoRepository, categoriaRepository, promocionService, productoImagenStorage);
    }

    @Test
    void creaProductoActivoConCategoriaYEstacion() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Brasas & Pollos");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(21L);
            return producto;
        });

        ResponseEntity<?> response = controller.crearProducto(
                new ProductoCrearRequest("  Pollo nuevo  ", new BigDecimal("45.50"), 1L, "Horno/Brasas"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Producto producto = (Producto) response.getBody();
        assertEquals("Pollo nuevo", producto.getNombre());
        assertEquals(new BigDecimal("45.50"), producto.getPrecio());
        assertEquals(categoria, producto.getCategoria());
        assertEquals("Horno/Brasas", producto.getEstacionKds());
        assertTrue(producto.getActivo());
    }

    @Test
    void rechazaPrecioInvalidoSinGuardarProducto() {
        ResponseEntity<?> response = controller.crearProducto(
                new ProductoCrearRequest("Plato", new BigDecimal("-1"), 1L, "Barra"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(categoriaRepository, productoRepository);
    }

    @Test
    void rechazaCategoriaInexistenteSinGuardarProducto() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.crearProducto(
                new ProductoCrearRequest("Plato", new BigDecimal("12.00"), 99L, "Barra"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void listarProductosIncluyeSoloActivosPorDefectoYSincronizaPromociones() {
        Producto activo = new Producto();
        activo.setActivo(true);
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(activo));

        assertEquals(List.of(activo), controller.listarProductos(false));
        verify(promocionService).sincronizarPrecios();
        verify(productoRepository).findByActivoTrue();
        verify(productoRepository, never()).findAll();
    }

    @Test
    void puedeListarProductosPausadosParaAdministracion() {
        Producto activo = new Producto();
        activo.setActivo(true);
        Producto pausado = new Producto();
        pausado.setActivo(false);
        when(productoRepository.findAll()).thenReturn(List.of(activo, pausado));

        assertEquals(List.of(activo, pausado), controller.listarProductos(true));
        verify(productoRepository).findAll();
        verify(productoRepository, never()).findByActivoTrue();
    }

    @Test
    void actualizaLaDisponibilidadDelProducto() {
        Producto producto = new Producto();
        producto.setId(21L);
        producto.setActivo(true);
        when(productoRepository.findById(21L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        ResponseEntity<?> response = controller.actualizarDisponibilidad(21L, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(producto.getActivo());
        verify(productoRepository).save(producto);
    }

    @Test
    void guardaYEntregaLaImagenDelProducto() throws IOException {
        Producto producto = new Producto();
        producto.setId(21L);
        when(productoRepository.findById(21L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);
        ByteArrayOutputStream imagen = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "jpg", imagen);
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getSize()).thenReturn((long) imagen.size());
        when(archivo.getContentType()).thenReturn(MediaType.IMAGE_JPEG_VALUE);
        when(archivo.getInputStream()).thenReturn(new ByteArrayInputStream(imagen.toByteArray()));

        ResponseEntity<?> guardado = controller.guardarImagen(21L, archivo);

        assertEquals(HttpStatus.OK, guardado.getStatusCode());
        assertTrue(producto.isTieneImagen());
        verify(productoImagenStorage).guardar(eq(21L), any(byte[].class));
        verify(productoRepository).save(producto);
    }

    @Test
    void rechazaImagenesQueNoSonJpgOPng() {
        MultipartFile archivo = mock(MultipartFile.class);
        when(productoRepository.findById(21L)).thenReturn(Optional.of(new Producto()));
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getContentType()).thenReturn("text/plain");

        ResponseEntity<?> response = controller.guardarImagen(21L, archivo);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(productoRepository, never()).save(any(Producto.class));
    }

}
