package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.icb.ingressos.domain.enums.StatusPedido;

interface PedidoJpaRepository extends JpaRepository<PedidoJpa, Long> {

    List<PedidoJpa> findByStatusAndDataPedidoBefore(StatusPedido status, LocalDateTime limite);
}
