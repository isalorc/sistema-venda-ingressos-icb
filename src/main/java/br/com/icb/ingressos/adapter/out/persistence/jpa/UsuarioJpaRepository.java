package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpa, Long> {

    Optional<UsuarioJpa> findByEmail(String email);
}
