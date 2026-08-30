package br.com.icb.ingressos.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class LoteRepositoryEmMemoria extends RepositorioEmMemoria<Lote> implements LoteRepositoryPort {

    @Override
    public Lote salvar(Lote lote) {
        return persistir(lote);
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return porId(id);
    }

    /**
     * No MVP em memória não há bloqueio pessimista real (RNF-07): a serialização
     * de verdade (<em>SELECT ... FOR UPDATE</em>) vem com o JPA/PostgreSQL no
     * Épico 9. Aqui a chamada apenas delega para {@link #buscarPorId}.
     */
    @Override
    public Optional<Lote> buscarPorIdComBloqueio(Long id) {
        return porId(id);
    }

    @Override
    public List<Lote> listarPorEvento(Long eventoId) {
        return filtrar(lote -> lote.getEventoId().equals(eventoId));
    }

    @Override
    protected Long id(Lote lote) {
        return lote.getId();
    }

    @Override
    protected Lote comId(Lote lote, long id) {
        return Lote.reconstituir(id, lote.getEventoId(), lote.getNome(), lote.getPreco(),
                lote.getQuantidadeTotal(), lote.getQuantidadeDisponivel());
    }
}
