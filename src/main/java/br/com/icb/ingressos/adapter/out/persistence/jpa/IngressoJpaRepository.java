package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.enums.StatusIngresso;

interface IngressoJpaRepository extends JpaRepository<IngressoJpa, Long> {

    Optional<IngressoJpa> findFirstByLoteIdAndStatus(Long loteId, StatusIngresso status);

    List<IngressoJpa> findByPedidoId(Long pedidoId);

    List<IngressoJpa> findByEventoIdAndStatus(Long eventoId, StatusIngresso status);

    long countByEventoIdAndStatusIn(Long eventoId, Collection<StatusIngresso> status);

    long countByLoteIdAndStatusIn(Long loteId, Collection<StatusIngresso> status);

    @Modifying
    @Transactional
    void deleteByLoteId(Long loteId);
}
