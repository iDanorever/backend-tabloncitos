package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Categoria;
import com.tabloncitos.backend.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    // GET: Listar todas las categorías del menú
    @GetMapping
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }
}