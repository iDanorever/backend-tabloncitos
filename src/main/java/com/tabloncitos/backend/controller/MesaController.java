package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Mesa;
import com.tabloncitos.backend.repository.MesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
@CrossOrigin(origins = "*")
public class MesaController {

    @Autowired
    private MesaRepository mesaRepository;

    // GET: Listar todas las mesas del restaurante
    @GetMapping
    public List<Mesa> listarMesas() {
        return mesaRepository.findAll();
    }

    // PUT: Actualizar el estado de la mesa (LIBRE, OCUPADA, CUENTA_PEDIDA, etc.)
    @PutMapping("/{id}/estado")
    public Mesa actualizarEstadoMesa(@PathVariable Long id, @RequestParam String nuevoEstado) {
        Mesa mesa = mesaRepository.findById(id).orElse(null);
        if (mesa != null) {
            mesa.setEstado(nuevoEstado);
            return mesaRepository.save(mesa);
        }
        return null;
    }
}