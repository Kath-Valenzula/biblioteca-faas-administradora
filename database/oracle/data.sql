ALTER SESSION SET CONTAINER = FREEPDB1;

INSERT INTO BIBLIOTECA.USUARIOS (NOMBRE, CORREO, TELEFONO, ESTADO) VALUES ('Ana Torres', 'ana.torres@correo.com', '999111222', 'ACTIVO');
INSERT INTO BIBLIOTECA.USUARIOS (NOMBRE, CORREO, TELEFONO, ESTADO) VALUES ('Luis Paredes', 'luis.paredes@correo.com', '999333444', 'ACTIVO');

INSERT INTO BIBLIOTECA.LIBROS (TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION) VALUES ('Clean Code', 'Robert C. Martin', '9780132350884', 'DISPONIBLE', 'Libro de buenas practicas de desarrollo.');
INSERT INTO BIBLIOTECA.LIBROS (TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION) VALUES ('Spring in Action', 'Craig Walls', '9781617297571', 'DISPONIBLE', 'Guia practica de Spring Boot y ecosistema Spring.');
INSERT INTO BIBLIOTECA.LIBROS (TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION) VALUES ('Designing Data-Intensive Applications', 'Martin Kleppmann', '9781449373320', 'DISPONIBLE', 'Fundamentos de sistemas distribuidos y persistencia.');

COMMIT;
