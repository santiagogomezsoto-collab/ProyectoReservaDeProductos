package reservas.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceRecursosTest {

    @BeforeEach
    void setUp() {
        Service.resetForTesting();
    }

    @Test
    void crearRecursoLoAgregaCorrectamente() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);

        Recurso r = new Recurso("R-1", cat, "Sala Principal");
        Service.instance().create(r);

        Recurso leido = Service.instance().read(r);
        assertEquals("Sala Principal", leido.getDescripcion());
        assertEquals(cat.getId(), leido.getCategoria().getId());
    }

    @Test
    void crearRecursoConIdDuplicadoLanzaExcepcion() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().create(new Recurso("R-1", cat, "Sala B")));
        assertEquals("Recurso ya existe", ex.getMessage());
    }

    @Test
    void actualizarRecursoCambiaDescripcionYCategoria() throws Exception {
        CategoriaRecurso cat1 = new CategoriaRecurso("", "Sala");
        CategoriaRecurso cat2 = new CategoriaRecurso("", "Laptop");
        Service.instance().create(cat1);
        Service.instance().create(cat2);

        Recurso r = new Recurso("R-1", cat1, "Descripcion original");
        Service.instance().create(r);

        Recurso actualizado = new Recurso("R-1", cat2, "Descripcion nueva");
        Service.instance().update(actualizado);

        Recurso leido = Service.instance().read(r);
        assertEquals("Descripcion nueva", leido.getDescripcion());
        assertEquals(cat2.getId(), leido.getCategoria().getId());
    }

    @Test
    void borrarRecursoLoEliminaDeLaLista() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Recurso r = new Recurso("R-1", cat, "Sala A");
        Service.instance().create(r);

        Service.instance().delete(r);

        Exception ex = assertThrows(Exception.class, () -> Service.instance().read(r));
        assertEquals("Recurso no existe", ex.getMessage());
    }

    @Test
    void buscarRecursosFiltraPorCategoria() throws Exception {
        CategoriaRecurso catSala = new CategoriaRecurso("", "Sala");
        CategoriaRecurso catLaptop = new CategoriaRecurso("", "Laptop");
        Service.instance().create(catSala);
        Service.instance().create(catLaptop);

        Service.instance().create(new Recurso("R-1", catSala, "Sala A"));
        Service.instance().create(new Recurso("R-2", catLaptop, "Laptop A"));

        List<Recurso> resultado = Service.instance().search(catSala, "");
        assertEquals(1, resultado.size());
        assertEquals("R-1", resultado.get(0).getId());
    }

    @Test
    void buscarRecursosSinCategoriaDevuelveTodos() throws Exception {
        CategoriaRecurso catSala = new CategoriaRecurso("", "Sala");
        CategoriaRecurso catLaptop = new CategoriaRecurso("", "Laptop");
        Service.instance().create(catSala);
        Service.instance().create(catLaptop);

        Service.instance().create(new Recurso("R-1", catSala, "Sala A"));
        Service.instance().create(new Recurso("R-2", catLaptop, "Laptop A"));

        List<Recurso> resultado = Service.instance().search(null, "");
        assertEquals(2, resultado.size());
    }
}