package com.biblioteca.libros.repository;

import com.biblioteca.libros.domain.Libro;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByIsbn(String isbn);
}
