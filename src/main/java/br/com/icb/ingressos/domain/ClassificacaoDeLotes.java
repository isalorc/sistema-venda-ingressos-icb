package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.icb.ingressos.domain.enums.StatusLote;
import br.com.icb.ingressos.domain.enums.StatusVendas;

/**
 * Resolve o "lote ativo" de um evento e o {@link StatusLote} de cada lote a
 * partir do estado atual (estoque + janela de vendas). Tudo é <strong>derivado
 * </strong> — nada é persistido. Regra completa em {@code docs/viradaDeLote.md}.
 *
 * <p>Lote ativo = o primeiro lote (por {@code id} crescente) que tem estoque e
 * está dentro da janela de vendas. Só ele é comprável.
 */
public final class ClassificacaoDeLotes {

    private final List<Lote> lotesOrdenados;
    private final Lote ativo;
    private final Map<Long, StatusLote> statusPorLote;
    private final LocalDateTime proximaAbertura;

    private ClassificacaoDeLotes(List<Lote> lotesOrdenados, Lote ativo,
                                 Map<Long, StatusLote> statusPorLote, LocalDateTime proximaAbertura) {
        this.lotesOrdenados = lotesOrdenados;
        this.ativo = ativo;
        this.statusPorLote = statusPorLote;
        this.proximaAbertura = proximaAbertura;
    }

    public static ClassificacaoDeLotes de(Collection<Lote> lotes, LocalDateTime agora) {
        Validacao.exigir(lotes, "lotes");
        Validacao.exigir(agora, "agora");

        var ordenados = lotes.stream()
                .sorted(Comparator.comparing(Lote::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        var ativo = ordenados.stream()
                .filter(lote -> lote.disponivelParaVenda(agora))
                .findFirst()
                .orElse(null);

        var status = new LinkedHashMap<Long, StatusLote>();
        LocalDateTime proximaAbertura = null;
        for (var lote : ordenados) {
            var classificacao = classificar(lote, ativo, agora);
            status.put(lote.getId(), classificacao);
            if (classificacao == StatusLote.AGENDADO
                    && (proximaAbertura == null || lote.getInicioVendas().isBefore(proximaAbertura))) {
                proximaAbertura = lote.getInicioVendas();
            }
        }
        return new ClassificacaoDeLotes(ordenados, ativo, status, proximaAbertura);
    }

    private static StatusLote classificar(Lote lote, Lote ativo, LocalDateTime agora) {
        if (!lote.vendasNaoEncerradas(agora)) {
            return StatusLote.ENCERRADO;
        }
        if (!lote.vendasIniciadas(agora)) {
            return StatusLote.AGENDADO;
        }
        if (lote.estaEsgotado()) {
            return StatusLote.ESGOTADO;
        }
        if (ativo != null && Objects.equals(ativo.getId(), lote.getId())) {
            return StatusLote.A_VENDA;
        }
        return StatusLote.NA_FILA;
    }

    /** O lote comprável agora, se houver. */
    public Optional<Lote> ativo() {
        return Optional.ofNullable(ativo);
    }

    /** {@code true} se {@code loteId} é o lote ativo do evento. */
    public boolean ativoEh(Long loteId) {
        return ativo != null && Objects.equals(ativo.getId(), loteId);
    }

    public StatusLote statusDe(Lote lote) {
        return statusPorLote.get(lote.getId());
    }

    public StatusLote statusDe(Long loteId) {
        return statusPorLote.get(loteId);
    }

    /** Lotes do evento em ordem de criação ({@code id} crescente). */
    public List<Lote> lotesOrdenados() {
        return lotesOrdenados;
    }

    /** Data em que o próximo lote agendado abre, se houver algum agendado. */
    public Optional<LocalDateTime> proximaAbertura() {
        return Optional.ofNullable(proximaAbertura);
    }

    /**
     * Lote que representa o evento agora: o ativo, ou — se nenhum está ativo — o
     * próximo agendado a abrir. Vazio só quando as vendas estão encerradas.
     */
    public Optional<Lote> ativoOuProximo() {
        if (ativo != null) {
            return Optional.of(ativo);
        }
        return lotesOrdenados.stream()
                .filter(lote -> statusPorLote.get(lote.getId()) == StatusLote.AGENDADO)
                .min(Comparator.comparing(Lote::getInicioVendas));
    }

    public StatusVendas statusVendas() {
        if (ativo != null) {
            return StatusVendas.A_VENDA;
        }
        return statusPorLote.containsValue(StatusLote.AGENDADO)
                ? StatusVendas.AGENDADA
                : StatusVendas.ENCERRADA;
    }
}
