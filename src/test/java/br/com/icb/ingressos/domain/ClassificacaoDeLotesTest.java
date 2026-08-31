package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.enums.StatusLote;
import br.com.icb.ingressos.domain.enums.StatusVendas;

/**
 * Virada de lote (docs/viradaDeLote.md): o lote ativo é derivado do estoque e da
 * janela de vendas, sem estado guardado.
 */
class ClassificacaoDeLotesTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final BigDecimal PRECO = new BigDecimal("50.00");

    private static Lote lote(long id, int disponivel) {
        return Lote.reconstituir(id, 1L, "Lote " + id, PRECO, 100, disponivel);
    }

    private static Lote lote(long id, int disponivel, LocalDateTime inicio, LocalDateTime fim) {
        return Lote.reconstituir(id, 1L, "Lote " + id, PRECO, 100, disponivel, inicio, fim);
    }

    @Test
    void oPrimeiroLoteComEstoqueEDentroDaJanelaEhOAtivo() {
        var c = ClassificacaoDeLotes.de(List.of(lote(10, 40), lote(11, 40)), AGORA);

        assertThat(c.ativoEh(10L)).isTrue();
        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.A_VENDA);
        assertThat(c.statusDe(11L)).isEqualTo(StatusLote.NA_FILA);
        assertThat(c.statusVendas()).isEqualTo(StatusVendas.A_VENDA);
    }

    @Test
    void loteEsgotadoCedeAVezAoProximo() {
        var c = ClassificacaoDeLotes.de(List.of(lote(10, 0), lote(11, 30)), AGORA);

        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.ESGOTADO);
        assertThat(c.ativoEh(11L)).isTrue();
        assertThat(c.statusDe(11L)).isEqualTo(StatusLote.A_VENDA);
    }

    @Test
    void ordemEhPorIdCrescenteIndependenteDaOrdemDeEntrada() {
        var c = ClassificacaoDeLotes.de(List.of(lote(11, 30), lote(10, 30)), AGORA);

        assertThat(c.ativoEh(10L)).isTrue();
        assertThat(c.lotesOrdenados()).extracting(Lote::getId).containsExactly(10L, 11L);
    }

    @Test
    void loteAgendadoNaoVendeEDefineAStatusVendasComoAgendada() {
        var abre = AGORA.plusDays(10);
        var c = ClassificacaoDeLotes.de(List.of(lote(10, 100, abre, null)), AGORA);

        assertThat(c.ativo()).isEmpty();
        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.AGENDADO);
        assertThat(c.statusVendas()).isEqualTo(StatusVendas.AGENDADA);
        assertThat(c.proximaAbertura()).contains(abre);
        assertThat(c.ativoOuProximo()).map(Lote::getId).contains(10L);
    }

    @Test
    void proximaAberturaEhAMenorEntreOsAgendados() {
        var c = ClassificacaoDeLotes.de(List.of(
                lote(10, 100, AGORA.plusDays(30), null),
                lote(11, 100, AGORA.plusDays(10), null)), AGORA);

        assertThat(c.proximaAbertura()).contains(AGORA.plusDays(10));
    }

    @Test
    void loteEncerradoPorDataCedeAVezMesmoComEstoque() {
        var c = ClassificacaoDeLotes.de(List.of(
                lote(10, 50, null, AGORA.minusDays(1)),
                lote(11, 50)), AGORA);

        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.ENCERRADO);
        assertThat(c.ativoEh(11L)).isTrue();
    }

    @Test
    void encerradoTemPrecedenciaSobreEsgotado() {
        // sem estoque E fim de vendas no passado -> ENCERRADO vence
        var c = ClassificacaoDeLotes.de(List.of(lote(10, 0, null, AGORA.minusDays(1))), AGORA);

        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.ENCERRADO);
    }

    @Test
    void agendadoTemPrecedenciaSobreEsgotado() {
        // sem estoque mas ainda não abriu -> AGENDADO (não ESGOTADO)
        var c = ClassificacaoDeLotes.de(List.of(lote(10, 0, AGORA.plusDays(5), null)), AGORA);

        assertThat(c.statusDe(10L)).isEqualTo(StatusLote.AGENDADO);
    }

    @Test
    void tudoEncerradoOuEsgotadoSemNadaAAbrirEhVendasEncerradas() {
        var c = ClassificacaoDeLotes.de(List.of(
                lote(10, 0),
                lote(11, 50, null, AGORA.minusDays(1))), AGORA);

        assertThat(c.statusVendas()).isEqualTo(StatusVendas.ENCERRADA);
        assertThat(c.ativoOuProximo()).isEmpty();
    }

    @Test
    void semLotesEhVendasEncerradas() {
        var c = ClassificacaoDeLotes.de(List.of(), AGORA);

        assertThat(c.statusVendas()).isEqualTo(StatusVendas.ENCERRADA);
        assertThat(c.ativo()).isEmpty();
    }

    @Test
    void loteSemDatasEntreLotesComDatasEntraNaFilaSoPelaOrdem() {
        var c = ClassificacaoDeLotes.de(List.of(
                lote(10, 50, null, AGORA.minusDays(1)),   // encerrado
                lote(11, 50),                              // sem datas -> assume
                lote(12, 50, AGORA.plusDays(5), null)),    // agendado
                AGORA);

        assertThat(c.ativoEh(11L)).isTrue();
        assertThat(c.statusDe(12L)).isEqualTo(StatusLote.AGENDADO);
    }
}
