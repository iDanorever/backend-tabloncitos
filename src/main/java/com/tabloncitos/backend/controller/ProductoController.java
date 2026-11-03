package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    // GET: Listar todos los productos o menús activos
    @GetMapping
    public List<Producto> listarProductos() {
        return productoRepository.findByActivoTrue();
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