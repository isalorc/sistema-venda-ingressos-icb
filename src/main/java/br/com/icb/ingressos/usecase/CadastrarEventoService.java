package br.com.icb.ingressos.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;

/**
 * Cadastra um evento (RF-20). A autenticação do administrador (RF-26) é
 * responsabilidade do adapter de entrada.
 */
@Service
public class CadastrarEventoService implements CadastrarEventoUseCase {

    private final EventoRepositoryPort eventoRepository;

    public CadastrarEventoService(EventoRepositoryPort eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Override
    @Transactional
    public Long cadastrar(CadastrarEventoCommand comando) {
        var evento = eventoRepository.salvar(Evento.novo(
                comando.nome(), comando.descricao(), comando.dataHora(),
                comando.dataFim(), comando.imagemUrl()));
        return evento.getId();
    }
}
