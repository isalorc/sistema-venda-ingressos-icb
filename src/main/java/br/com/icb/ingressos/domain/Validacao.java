package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Validacao {

    private Validacao() {
    }

    static <T> T exigir(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor;
    }

    static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.strip();
    }

    /** Valor monetário não nulo, não negativo e com no máximo 2 casas decimais (RNF-10). */
    static BigDecimal exigirDinheiro(BigDecimal valor, String campo) {
        exigir(valor, campo);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(campo + " deve ter no máximo 2 casas decimais.");
        }
    }

    static int exigirPositivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return valor;
    }
}
