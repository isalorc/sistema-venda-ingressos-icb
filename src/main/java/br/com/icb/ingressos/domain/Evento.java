package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Evento {

    @EqualsAndHashCode.Include
    private final Long id;
    private final String nome;
    private final String descricao;
    /** Início do evento (histórico: chamava-se só "dataHora"). */
    private final LocalDateTime dataHora;
    /** Fim do evento. Nulo = evento pontual (usa {@link #dataHora}). */
    private final LocalDateTime dataFim;
    /** URL da imagem/banner do evento (o arquivo é hospedado fora). Nulo = sem imagem. */
    private final String imagemUrl;
    /** Momento do cancelamento. Nulo = evento ativo. */
    private LocalDateTime canceladoEm;

    private Evento(Long id, String nome, String descricao, LocalDateTime dataHora,
                   LocalDateTime dataFim, String imagemUrl, LocalDateTime canceladoEm) {
        this.id = id;
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
        this.dataHora = Validacao.exigir(dataHora, "dataHora");
        if (dataFim != null && dataFim.isBefore(dataHora)) {
            throw new IllegalArgumentException("dataFim não pode ser anterior a dataHora.");
        }
        this.dataFim = dataFim;
        this.imagemUrl = validarImagemUrl(imagemUrl);
        this.canceladoEm = canceladoEm;
    }

    public static Evento novo(String nome, String descricao, LocalDateTime dataHora) {
        return novo(nome, descricao, dataHora, null, null);
    }

    public static Evento novo(String nome, String descricao, LocalDateTime dataHora,
                              LocalDateTime dataFim, String imagemUrl) {
        return new Evento(null, nome, descricao, dataHora, dataFim, imagemUrl, null);
    }

    public static Evento reconstituir(Long id, String nome, String descricao, LocalDateTime dataHora) {
        return reconstituir(id, nome, descricao, dataHora, null, null, null);
    }

    public static Evento reconstituir(Long id, String nome, String descricao, LocalDateTime dataHora,
                                      LocalDateTime dataFim, String imagemUrl, LocalDateTime canceladoEm) {
        return new Evento(Validacao.exigir(id, "id"), nome, descricao, dataHora, dataFim, imagemUrl, canceladoEm);
    }

    /** Instante em que o evento termina, para efeito de "já ocorreu": o fim, ou o início se não há fim. */
    public LocalDateTime termino() {
        return dataFim != null ? dataFim : dataHora;
    }

    public boolean jaOcorreu(LocalDateTime referencia) {
        return termino().isBefore(Validacao.exigir(referencia, "referencia"));
    }

    public boolean cancelado() {
        return canceladoEm != null;
    }

    /** Cancela o evento. Idempotente: cancelar de novo não muda o momento registrado. */
    public void cancelar(LocalDateTime referencia) {
        if (canceladoEm == null) {
            this.canceladoEm = Validacao.exigir(referencia, "referencia");
        }
    }

    /**
     * Devolve uma nova instância com os dados editados, preservando id e o estado
     * de cancelamento. Recusa editar evento cancelado.
     */
    public Evento editado(String nome, String descricao, LocalDateTime dataHora, LocalDateTime dataFim,
                          String imagemUrl) {
        if (cancelado()) {
            throw new TransicaoInvalidaException("Não é possível editar um evento cancelado.");
        }
        return new Evento(id, nome, descricao, dataHora, dataFim, imagemUrl, canceladoEm);
    }

    private static String validarImagemUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        var limpo = url.strip();
        if (!limpo.startsWith("http://") && !limpo.startsWith("https://")) {
            throw new IllegalArgumentException("imagemUrl deve começar com http:// ou https://.");
        }
        return limpo;
    }
}
