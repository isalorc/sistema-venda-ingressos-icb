package br.com.icb.ingressos.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Base dos adapters de persistência em memória do MVP (Épico 5).
 *
 * <p>Guarda as entidades em um {@link ConcurrentHashMap} e simula a geração de
 * chave primária com um contador. Ao entrar o PostgreSQL (Épico 9) estes adapters
 * são trocados por implementações Spring Data JPA sem tocar nos casos de uso.
 *
 * <p>As entidades de domínio têm {@code id} imutável: ao persistir uma entidade
 * nova (id nulo), a subclasse devolve, via {@link #comId(Object, long)}, uma cópia
 * reconstituída já com o id atribuído — é essa instância que o chamador deve usar.
 *
 * @param <T> tipo da entidade de domínio
 */
abstract class RepositorioEmMemoria<T> {

    protected final ConcurrentMap<Long, T> dados = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    /** Id da entidade, ou {@code null} se ainda não foi persistida. */
    protected abstract Long id(T entidade);

    /** Reconstrói a entidade com o id informado (usado só na primeira persistência). */
    protected abstract T comId(T entidade, long id);

    protected T persistir(T entidade) {
        var id = id(entidade);
        if (id != null) {
            dados.put(id, entidade);
            return entidade;
        }
        var novoId = sequencia.incrementAndGet();
        var persistida = comId(entidade, novoId);
        dados.put(novoId, persistida);
        return persistida;
    }

    protected List<T> persistirTodos(List<T> entidades) {
        return entidades.stream().map(this::persistir).toList();
    }

    protected Optional<T> porId(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(dados.get(id));
    }

    protected List<T> todos() {
        return List.copyOf(dados.values());
    }

    protected List<T> filtrar(Predicate<T> criterio) {
        return dados.values().stream().filter(criterio).toList();
    }

    protected Optional<T> primeiro(Predicate<T> criterio) {
        return dados.values().stream().filter(criterio).findFirst();
    }
}
