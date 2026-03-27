package com.biblioteca.libros.service;

import com.biblioteca.libros.domain.EstadoLibro;
import com.biblioteca.libros.domain.Libro;
import com.biblioteca.libros.dto.ActualizarEstadoLibroRequest;
import com.biblioteca.libros.dto.CrearLibroRequest;
import com.biblioteca.libros.dto.LibroResponse;
import com.biblioteca.libros.exception.BusinessException;
import com.biblioteca.libros.exception.ResourceNotFoundException;
import com.biblioteca.libros.repository.LibroRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibroService {

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional(readOnly = true)
    public List<LibroResponse> listar() {
        return libroRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return toResponse(buscarEntidad(id));
    }

    @Transactional
    public LibroResponse crear(CrearLibroRequest request) {
        libroRepository.findByIsbn(request.isbn()).ifPresent(libro -> {
            throw new BusinessException("Ya existe un libro registrado con el ISBN indicado");
        });

        Libro libro = new Libro();
        libro.setTitulo(request.titulo());
        libro.setAutor(request.autor());
        libro.setIsbn(request.isbn());
        libro.setDescripcion(request.descripcion());
        libro.setEstado(EstadoLibro.DISPONIBLE);
        return toResponse(libroRepository.save(libro));
    }

    @Transactional
    public LibroResponse actualizarEstado(Long id, ActualizarEstadoLibroRequest request) {
        Libro libro = buscarEntidad(id);
        EstadoLibro nuevoEstado;
        try {
            nuevoEstado = EstadoLibro.valueOf(request.estado().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("El estado del libro no es valido");
        }
        libro.setEstado(nuevoEstado);
        return toResponse(libroRepository.save(libro));
    }

    @Transactional(readOnly = true)
    public boolean estaDisponible(Long id) {
        return buscarEntidad(id).getEstado() == EstadoLibro.DISPONIBLE;
    }

    @SuppressWarnings("null")
    private Libro buscarEntidad(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("No existe un libro con el id indicado");
        }
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un libro con el id indicado"));
    }

    private LibroResponse toResponse(Libro libro) {
        return new LibroResponse(
                libro.getId(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getIsbn(),
                libro.getEstado().name(),
                libro.getDescripcion(),
                libro.getEstado() == EstadoLibro.DISPONIBLE
        );
    }
}
