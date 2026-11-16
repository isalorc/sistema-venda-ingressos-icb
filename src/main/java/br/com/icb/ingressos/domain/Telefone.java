package br.com.icb.ingressos.domain;

import java.util.regex.Pattern;

/**
 * Telefone brasileiro: DDD (11 a 99, sem zero) + número. Celular tem 9 dígitos
 * começando por 9; fixo tem 8 dígitos começando de 2 a 8. Aceita máscara e o
 * prefixo +55 na entrada e guarda só os dígitos (ex.: {@code 13999998888}).
 */
public final class Telefone {

    static final String MENSAGEM = "Telefone inválido. Informe DDD + número, ex.: (13) 99999-8888.";

    private static final Pattern VALIDO = Pattern.compile("^[1-9]{2}(9\\d{8}|[2-8]\\d{7})$");

    private Telefone() {
    }

    /** Devolve só os dígitos (sem +55) ou lança {@link IllegalArgumentException}. */
    public static String normalizar(String telefone) {
        var digitos = Validacao.exigirTexto(telefone, "telefone").replaceAll("\\D", "");
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            digitos = digitos.substring(2);
        }
        if (!VALIDO.matcher(digitos).matches()) {
            throw new IllegalArgumentException(MENSAGEM);
        }
        return digitos;
    }
}
