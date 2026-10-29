package com.banknode.atm.repository;

import com.banknode.atm.model.Cajero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad Cajero.
 */
@Repository
public interface CajeroRepository extends JpaRepository<Cajero, Long> {
}
