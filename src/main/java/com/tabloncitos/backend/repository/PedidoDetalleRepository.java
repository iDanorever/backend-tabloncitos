package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {
    // Listar los detalles asociados a un ID de pedido específico
    List<PedidoDetalle> findByPedidoId(Long pedidoId);
}