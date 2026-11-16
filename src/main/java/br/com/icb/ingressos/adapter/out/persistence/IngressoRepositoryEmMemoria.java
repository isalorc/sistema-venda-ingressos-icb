package br.com.icb.ingressos.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class IngressoRepositoryEmMemoria extends RepositorioEmMemoria<Ingresso> implements IngressoRepositoryPort {

    @Override
    public Ingresso salvar(Ingresso ingresso) {
        return persistir(ingresso);
    }

    @Override
    public List<Ingresso> salvarTodos(List<Ingresso> ingressos) {
        return persistirTodos(ingressos);
    }

    @Override
    public Optional<Ingresso> buscarPorId(Long id) {
        return porId(id);
    }

    @Override
    public Optional<Ingresso> buscarPrimeiroDisponivelDoLote(Long loteId) {
        return primeiro(ingresso -> ingresso.getLoteId().equals(loteId)
                && ingresso.getStatus() == StatusIngresso.DISPONIVEL);
    }

    @Override
    public List<Ingresso> listarPorPedido(Long pedidoId) {
        return filtrar(ingresso -> pedidoId != null && pedidoId.equals(ingresso.getPedidoId()));
    }

    @Override
    public List<Ingresso> listarVendidosDoEvento(Long eventoId) {
        return filtrar(ingresso -> ingresso.getEventoId().equals(eventoId)
                && ingresso.getStatus() == StatusIngresso.VENDIDO);
    }

    @Override
    public long contarPorEventoNosStatus(Long eventoId, Collection<StatusIngresso> status) {
        return contar(ingresso -> ingresso.getEventoId().equals(eventoId)
                && status.contains(ingresso.getStatus()));
    }

    @Override
    public long contarPorLoteNosStatus(Long loteId, Collection<StatusIngresso> status) {
        return contar(ingresso -> ingresso.getLoteId().equals(loteId)
                && status.contains(ingresso.getStatus()));
    }

    @Override
    public void excluirPorLote(Long loteId) {
        removerSe(ingresso -> ingresso.getLoteId().equals(loteId));
    }

    @Override
    protected Long id(Ingresso ingresso) {
        return ingresso.getId();
    }

    @Override
    protected Ingresso comId(Ingresso ingresso, long id) {
        return Ingresso.reconstituir(id, ingresso.getEventoId(), ingresso.getLoteId(),
                ingresso.getPedidoId(), ingresso.getCodigoQr(), ingresso.getStatus());
    }
}
