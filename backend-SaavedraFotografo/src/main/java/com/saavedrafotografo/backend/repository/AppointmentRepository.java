package com.saavedrafotografo.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saavedrafotografo.backend.entity.Appointment;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findBySessionId(String sessionId);
    boolean existsBySessionId(String sessionId);
    List<Appointment> findAllByAppointmentDateBetween(LocalDateTime start, LocalDateTime end);
}
