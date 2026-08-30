package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase.CadastrarEventoCommand;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CadastrarEventoServiceTest {

    @Mock private EventoRepositoryPort eventoRepository;
    @InjectMocks private CadastrarEventoService service;

    @Test
    void persisteOEventoNovoERetornaOIdGerado() {
        var dataHora = LocalDateTime.of(2026, 12, 1, 20, 0);
        when(eventoRepository.salvar(any())).thenReturn(
                Evento.reconstituir(42L, "Congresso", "descrição", dataHora));

        var id = service.cadastrar(new CadastrarEventoCommand("Congresso", "descrição", dataHora));

        assertThat(id).isEqualTo(42L);

        var salvo = ArgumentCaptor.forClass(Evento.class);
        org.mockito.Mockito.verify(eventoRepository).salvar(salvo.capture());
        assertThat(salvo.getValue().getId()).isNull();
        assertThat(salvo.getValue().getNome()).isEqualTo("Congresso");
    }
}
