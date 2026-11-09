package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.PromocionRequest;
import com.tabloncitos.backend.dto.PromocionResponse;
import com.tabloncitos.backend.service.PromocionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promociones")
@CrossOrigin(origins = "*")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    public List<PromocionResponse> listar() {
        return promocionService.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody(required = false) PromocionRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(promocionService.guardar(request, null));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody(required = false) PromocionRequest request) {
        try {
            return ResponseEntity.ok(promocionService.guardar(request, id));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        }
    }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<?> publicar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(promocionService.publicar(id));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        }
    }

    @PostMapping("/{id}/archivar")
    public ResponseEntity<?> archivar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(promocionService.archivar(id));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        }
    }
}
