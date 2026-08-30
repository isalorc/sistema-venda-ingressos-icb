package br.com.icb.ingressos.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventoJpaRepository extends JpaRepository<EventoJpa, Long> {
}
