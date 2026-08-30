package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface PagamentoJpaRepository extends JpaRepository<PagamentoJpa, Long> {

    Optional<PagamentoJpa> findByPedidoId(Long pedidoId);

    Optional<PagamentoJpa> findByReferenciaGateway(String referenciaGateway);
}
