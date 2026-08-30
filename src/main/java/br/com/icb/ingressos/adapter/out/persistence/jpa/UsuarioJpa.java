package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Espelho de persistência de {@link Usuario}. Vive na borda (adapter) e conhece
 * o domínio; o domínio não conhece a JPA (regra de dependência — RNF-02).
 */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UsuarioJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    private UsuarioJpa(Long id, String nome, String email, String telefone, LocalDateTime dataCriacao) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.dataCriacao = dataCriacao;
    }

    static UsuarioJpa de(Usuario usuario) {
        return new UsuarioJpa(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getTelefone(), usuario.getDataCriacao());
    }

    Usuario paraDominio() {
        return Usuario.reconstituir(id, nome, email, telefone, dataCriacao);
    }
}
