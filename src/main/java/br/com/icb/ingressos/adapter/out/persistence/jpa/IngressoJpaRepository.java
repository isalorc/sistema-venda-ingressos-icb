package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.icb.ingressos.domain.enums.StatusIngresso;

interface IngressoJpaRepository extends JpaRepository<IngressoJpa, Long> {

    Optional<IngressoJpa> findFirstByLoteIdAndStatus(Long loteId, StatusIngresso status);

    List<IngressoJpa> findByPedidoId(Long pedidoId);

    List<IngressoJpa> findByEventoIdAndStatus(Long eventoId, StatusIngresso status);
}
