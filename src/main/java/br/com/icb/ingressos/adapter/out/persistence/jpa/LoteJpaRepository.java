package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface LoteJpaRepository extends JpaRepository<LoteJpa, Long> {

    List<LoteJpa> findByEventoId(Long eventoId);

    /**
     * Carrega o lote com bloqueio pessimista de escrita (RNF-07):
     * {@code SELECT ... FOR UPDATE}. Duas compras concorrentes do mesmo lote
     * serializam aqui — a segunda espera a transação da primeira terminar e só
     * então lê a {@code quantidade_disponivel} já atualizada. Exige transação
     * ativa (garantida pelo {@code @Transactional} do caso de uso).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteJpa l where l.id = :id")
    Optional<LoteJpa> buscarComBloqueio(@Param("id") Long id);
}
