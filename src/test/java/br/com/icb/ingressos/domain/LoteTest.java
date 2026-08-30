package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

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
