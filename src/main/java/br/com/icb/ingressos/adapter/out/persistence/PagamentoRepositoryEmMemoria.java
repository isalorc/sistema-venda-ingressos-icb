package br.com.icb.ingressos.adapter.out.persistence;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class PagamentoRepositoryEmMemoria extends RepositorioEmMemoria<Pagamento> implements PagamentoRepositoryPort {

    @Override
    public Pagamento salvar(Pagamento pagamento) {
        return persistir(pagamento);
    }

    @Override
    public Optional<Pagamento> buscarPorId(Long id) {
        return porId(id);
    }

    @Override
    public Optional<Pagamento> buscarPorPedido(Long pedidoId) {
        return primeiro(pagamento -> pagamento.getPedidoId().equals(pedidoId));
    }

    @Override
    public Optional<Pagamento> buscarPorReferenciaGateway(String referenciaGateway) {
        return referenciaGateway == null
                ? Optional.empty()
                : primeiro(pagamento -> referenciaGateway.equals(pagamento.getReferenciaGateway()));
    }

    @Override
    protected Long id(Pagamento pagamento) {
        return pagamento.getId();
    }

    @Override
    protected Pagamento comId(Pagamento pagamento, long id) {
        return Pagamento.reconstituir(id, pagamento.getPedidoId(), pagamento.getDataPagamento(),
                pagamento.getValor(), pagamento.getMetodo(), pagamento.getStatus(),
                pagamento.getReferenciaGateway());
    }
}
