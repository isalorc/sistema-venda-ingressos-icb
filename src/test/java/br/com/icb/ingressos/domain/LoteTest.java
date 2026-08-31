package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

class LoteTest {

    private static Lote loteCom(int total) {
        return Lote.novo(1L, "Inteira", new BigDecimal("50.00"), total);
    }

    @Nested
    class Construcao {

        @Test
        void loteNovoComecaComEstoqueCheio() {
            var lote = loteCom(100);

            assertThat(lote.getQuantidadeTotal()).isEqualTo(100);
            assertThat(lote.getQuantidadeDisponivel()).isEqualTo(100);
            assertThat(lote.estaEsgotado()).isFalse();
        }

        @Test
        void normalizaPrecoParaDuasCasas() {
            var lote = Lote.novo(1L, "Inteira", new BigDecimal("50.5"), 10);

            assertThat(lote.getPreco()).isEqualByComparingTo("50.50");
            assertThat(lote.getPreco().scale()).isEqualTo(2);
        }

        @Test
        void rejeitaPrecoComMaisDeDuasCasas() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Lote.novo(1L, "Inteira", new BigDecimal("50.555"), 10));
        }

        @Test
        void rejeitaPrecoNegativo() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Lote.novo(1L, "Inteira", new BigDecimal("-1.00"), 10));
        }

        @Test
        void rejeitaQuantidadeTotalNaoPositiva() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Lote.novo(1L, "Inteira", new BigDecimal("50.00"), 0));
        }

        @Test
        void rejeitaDisponivelMaiorQueTotalNaReconstituicao() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Lote.reconstituir(1L, 1L, "Inteira", new BigDecimal("50.00"), 10, 11));
        }

        @Test
        void rejeitaFimDeVendasAntesDoInicio() {
            var inicio = LocalDateTime.of(2026, 10, 10, 0, 0);
            assertThatIllegalArgumentException().isThrownBy(() -> Lote.novo(
                    1L, "Inteira", new BigDecimal("50.00"), 10, inicio, inicio.minusDays(1)));
        }
    }

    @Nested
    class JanelaDeVendas {

        private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);

        @Test
        void loteSemJanelaEstaSempreDentroDaJanela() {
            var lote = loteCom(10);

            assertThat(lote.dentroDaJanelaDeVendas(AGORA)).isTrue();
            assertThat(lote.disponivelParaVenda(AGORA)).isTrue();
        }

        @Test
        void loteComInicioNoFuturoAindaNaoIniciou() {
            var lote = Lote.reconstituir(1L, 1L, "Inteira", new BigDecimal("50.00"), 10, 10,
                    AGORA.plusDays(5), null);

            assertThat(lote.vendasIniciadas(AGORA)).isFalse();
            assertThat(lote.disponivelParaVenda(AGORA)).isFalse();
        }

        @Test
        void loteComFimNoPassadoJaEncerrou() {
            var lote = Lote.reconstituir(1L, 1L, "Inteira", new BigDecimal("50.00"), 10, 10,
                    null, AGORA.minusDays(1));

            assertThat(lote.vendasNaoEncerradas(AGORA)).isFalse();
            assertThat(lote.disponivelParaVenda(AGORA)).isFalse();
        }

        @Test
        void loteEsgotadoNaoEstaDisponivelMesmoDentroDaJanela() {
            var lote = Lote.reconstituir(1L, 1L, "Inteira", new BigDecimal("50.00"), 10, 0);

            assertThat(lote.dentroDaJanelaDeVendas(AGORA)).isTrue();
            assertThat(lote.disponivelParaVenda(AGORA)).isFalse();
        }
    }

    @Nested
    class Edicao {

        @Test
        void editadoAumentandoCapacidadeSomaAoEstoqueDisponivel() {
            var lote = Lote.reconstituir(5L, 1L, "Inteira", new BigDecimal("50.00"), 100, 40);

            var novo = lote.editado("Inteira", new BigDecimal("60.00"), 150, null, null);

            assertThat(novo.getId()).isEqualTo(5L);
            assertThat(novo.getQuantidadeTotal()).isEqualTo(150);
            assertThat(novo.getQuantidadeDisponivel()).isEqualTo(90); // 40 + 50
            assertThat(novo.getPreco()).isEqualByComparingTo("60.00");
        }

        @Test
        void editadoRecusaReduzirACapacidade() {
            var lote = Lote.reconstituir(5L, 1L, "Inteira", new BigDecimal("50.00"), 100, 40);

            assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(
                    () -> lote.editado("Inteira", new BigDecimal("50.00"), 80, null, null));
        }
    }

    @Nested
    class Estoque {

        @Test
        void reservarUnidadeDecrementaDisponivel() {
            var lote = loteCom(2);

            lote.reservarUnidade();

            assertThat(lote.getQuantidadeDisponivel()).isEqualTo(1);
            assertThat(lote.quantidadeReservada()).isEqualTo(1);
        }

        @Test
        void reservarAlemDoEstoqueLancaIngressoEsgotado() {
            var lote = loteCom(1);
            lote.reservarUnidade();

            assertThatExceptionOfType(IngressoEsgotadoException.class)
                    .isThrownBy(lote::reservarUnidade);
            assertThat(lote.estaEsgotado()).isTrue();
        }

        @Test
        void liberarUnidadeDevolveAoEstoque() {
            var lote = loteCom(1);
            lote.reservarUnidade();

            lote.liberarUnidade();

            assertThat(lote.getQuantidadeDisponivel()).isEqualTo(1);
        }

        @Test
        void liberarComEstoqueCheioLancaTransicaoInvalida() {
            var lote = loteCom(1);

            assertThatExceptionOfType(TransicaoInvalidaException.class)
                    .isThrownBy(lote::liberarUnidade);
        }

        @Test
        void reservarTodoOEstoqueEDepoisLiberarTudoRestauraOInvariante() {
            var lote = loteCom(5);

            for (int i = 0; i < 5; i++) {
                lote.reservarUnidade();
            }
            assertThat(lote.estaEsgotado()).isTrue();

            for (int i = 0; i < 5; i++) {
                lote.liberarUnidade();
            }
            assertThat(lote.getQuantidadeDisponivel()).isEqualTo(5);
        }
    }
}
