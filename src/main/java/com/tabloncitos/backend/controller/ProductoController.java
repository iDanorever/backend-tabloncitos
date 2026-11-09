package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.ProductoCrearRequest;
import com.tabloncitos.backend.model.Categoria;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.repository.CategoriaRepository;
import com.tabloncitos.backend.repository.ProductoRepository;
import com.tabloncitos.backend.service.ProductoImagenStorage;
import com.tabloncitos.backend.service.PromocionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductoController.class);
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PromocionService promocionService;
    private final ProductoImagenStorage productoImagenStorage;

    public ProductoController(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            PromocionService promocionService,
            ProductoImagenStorage productoImagenStorage) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.promocionService = promocionService;
        this.productoImagenStorage = productoImagenStorage;
    }

    // GET: Listar todos los productos o menús activos
    @GetMapping
    public List<Producto> listarProductos(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        promocionService.sincronizarPrecios();
        return incluirInactivos ? productoRepository.findAll() : productoRepository.findByActivoTrue();
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody(required = false) ProductoCrearRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Ingrese los datos del plato"));
        }
        if (request.nombre() == null || request.nombre().isBlank() || request.nombre().trim().length() > 150) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "El nombre del plato es obligatorio y no debe superar 150 caracteres"));
        }
        if (request.precio() == null
                || request.precio().compareTo(BigDecimal.ZERO) <= 0
                || request.precio().compareTo(new BigDecimal("99999999.99")) > 0
                || request.precio().stripTrailingZeros().scale() > 2) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Ingrese un precio válido con hasta dos decimales"));
        }
        if (request.categoriaId() == null || request.categoriaId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Seleccione una categoría válida"));
        }
        if (request.estacionKds() != null && request.estacionKds().trim().length() > 50) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La estación no debe superar 50 caracteres"));
        }

        Optional<Categoria> categoria = categoriaRepository.findById(request.categoriaId());
        if (categoria.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La categoría seleccionada no existe"));
        }

        Producto producto = new Producto();
        producto.setNombre(request.nombre().trim());
        producto.setPrecio(request.precio());
        producto.setCategoria(categoria.get());
        producto.setEstacionKds(
                request.estacionKds() == null || request.estacionKds().isBlank()
                        ? null
                        : request.estacionKds().trim());
        producto.setActivo(true);

        return ResponseEntity.status(HttpStatus.CREATED).body(productoRepository.save(producto));
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> guardarImagen(@PathVariable Long id, @RequestParam("imagen") MultipartFile archivo) {
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró el plato seleccionado"));
        }
        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Selecciona una imagen"));
        }
        if (archivo.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La imagen no debe superar 5 MB"));
        }
        if (!MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(archivo.getContentType())
                && !MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(archivo.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Selecciona una imagen JPG o PNG"));
        }

        try {
            byte[] imagen = normalizarImagen(archivo);
            productoImagenStorage.guardar(id, imagen);
            producto.get().setTieneImagen(true);
            productoRepository.save(producto.get());
            return ResponseEntity.ok(Map.of("mensaje", "Imagen guardada"));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        } catch (IOException error) {
            LOGGER.error("No se pudo guardar la imagen del producto {}", id, error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No se pudo guardar la imagen. Inténtalo nuevamente."));
        }
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> actualizarDisponibilidad(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró el plato seleccionado"));
        }

        producto.get().setActivo(activo);
        return ResponseEntity.ok(productoRepository.save(producto.get()));
    }

    private byte[] normalizarImagen(MultipartFile archivo) throws IOException {
        BufferedImage original;
        try (ImageInputStream entrada = ImageIO.createImageInputStream(archivo.getInputStream())) {
            if (entrada == null) {
                throw new IllegalArgumentException("El archivo seleccionado no es una imagen válida");
            }
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new IllegalArgumentException("El archivo seleccionado no es una imagen JPG o PNG válida");
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || (long) ancho * alto > 20_000_000L) {
                    throw new IllegalArgumentException("La imagen debe tener como máximo 20 megapíxeles");
                }
                original = lector.read(0);
            } finally {
                lector.dispose();
            }
        }
        if (original == null) {
            throw new IllegalArgumentException("El archivo seleccionado no es una imagen válida");
        }

        double escala = Math.min(1d, Math.min(1200d / original.getWidth(), 900d / original.getHeight()));
        int ancho = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(original.getHeight() * escala));
        BufferedImage optimizada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = optimizada.createGraphics();
        try {
            grafico.setColor(new Color(24, 23, 22));
            grafico.fillRect(0, 0, ancho, alto);
            grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            grafico.drawImage(original, 0, 0, ancho, alto, null);
        } finally {
            grafico.dispose();
        }

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            if (!ImageIO.write(optimizada, "jpg", salida)) {
                throw new IOException("No hay un codificador JPEG disponible");
            }
            return salida.toByteArray();
        }
    }

    // GET: Buscar un producto por su ID
    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    // DELETE: Borrado Lógico (Soft-Delete)
    @DeleteMapping("/{id}")
    public Producto desactivarProducto(@PathVariable Long id) {
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto != null) {
            producto.setActivo(false);
            return productoRepository.save(producto);
        }
        return null;
    }
}