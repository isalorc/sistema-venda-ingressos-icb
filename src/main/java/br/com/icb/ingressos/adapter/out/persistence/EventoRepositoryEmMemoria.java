package br.com.icb.ingressos.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class EventoRepositoryEmMemoria extends RepositorioEmMemoria<Evento> implements EventoRepositoryPort {

    @Override
    public Evento salvar(Evento evento) {
        return persistir(evento);
    }

    @Override
    public Optional<Evento> buscarPorId(Long id) {
        return porId(id);
    }

    @Override
    public List<Evento> listarTodos() {
        return todos();
    }

    @Override
    protected Long id(Evento evento) {
        return evento.getId();
    }

    @Override
    protected Evento comId(Evento evento, long id) {
        return Evento.reconstituir(id, evento.getNome(), evento.getDescricao(), evento.getDataHora());
    }
}
