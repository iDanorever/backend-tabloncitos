package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // Listar pedidos por estado (útil para la cola de cocina KDS y caja)
    List<Pedido> findByEstadoPedido(String estadoPedido);

    List<Pedido> findByEstadoPedidoInOrderByFechaCreacionAsc(List<String> estados);
}