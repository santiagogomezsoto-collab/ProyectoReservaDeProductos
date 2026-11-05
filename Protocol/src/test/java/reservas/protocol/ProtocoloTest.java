package reservas.protocol;

import org.junit.jupiter.api.Test;
import reservas.logic.*;
import reservas.ia.ReservaExtraccion;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocoloTest {
    private static final CategoriaDto CAT = new CategoriaDto("C1", "Equipo");
    private static final FuncionarioDto FUN = new FuncionarioDto("F1", "FUNCIONARIO", "Ana", "123");
    private static final RecursoDto REC = new RecursoDto("R1", CAT, "Proyector");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 5);
    private static final ReservaDto RES = new ReservaDto("V1", FUN, "Clase", FECHA,
            LocalTime.of(8, 0), LocalTime.of(9, 30), new RecursoDto[]{REC}, "ACTIVA");
    private static final ReservaExtraccionDto EXT = new ReservaExtraccionDto(
            "Clase", "2026-11-05", "08:00", "09:30", new String[]{"Equipo"});

    static byte[] bytes(Object value) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) { out.writeObject(value); }
        return buffer.toByteArray();
    }

    static Object leer(byte[] bytes, ObjectInputFilter filtro) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(filtro);
            return in.readObject();
        }
    }

    static Object vuelta(Object value, ObjectInputFilter filtro) throws Exception {
        return leer(bytes(value), filtro);
    }

    @Test void todosLosDtoViajanConFiltro() throws Exception {
        UsuarioDto usuario = new UsuarioDto(FUN.id(), FUN.rol(), FUN);
        assertEquals(usuario, vuelta(usuario, FiltrosProtocolo.respuestas()));
        assertEquals(FUN, vuelta(FUN, FiltrosProtocolo.respuestas()));
        assertEquals(CAT, vuelta(CAT, FiltrosProtocolo.respuestas()));
        assertEquals(REC, vuelta(REC, FiltrosProtocolo.respuestas()));
        ReservaDto reserva = (ReservaDto) vuelta(RES, FiltrosProtocolo.respuestas());
        assertEquals(RES.id(), reserva.id());
        assertEquals(FUN, reserva.funcionario());
        assertEquals(FECHA, reserva.fecha());
        assertEquals(RES.horaInicio(), reserva.horaInicio());
        assertEquals(RES.horaFin(), reserva.horaFin());
        assertArrayEquals(RES.recursos(), reserva.recursos());
        ReservaExtraccionDto ext = (ReservaExtraccionDto) vuelta(EXT, FiltrosProtocolo.respuestas());
        assertEquals(EXT.actividad(), ext.actividad());
        assertEquals(EXT.fecha(), ext.fecha());
        assertEquals(EXT.horaInicio(), ext.horaInicio());
        assertEquals(EXT.horaFinal(), ext.horaFinal());
        assertArrayEquals(EXT.categoriasRecurso(), ext.categoriasRecurso());
        CantidadCategoria categoria = new CantidadCategoria("Equipo", 3);
        CantidadSemana semana = new CantidadSemana(FECHA, 4);
        assertEquals(categoria, vuelta(categoria, FiltrosProtocolo.respuestas()));
        assertEquals(semana, vuelta(semana, FiltrosProtocolo.respuestas()));
    }

    @Test void veintitresOperacionesYTipos() throws Exception {
        Set<Integer> codigos = new HashSet<>();
        for (var campo : Operaciones.class.getFields()) {
            assertTrue(codigos.add(campo.getInt(null)), "Codigo repetido");
        }
        assertEquals(23, codigos.size());
        assertEquals(23, IService.class.getDeclaredMethods().length);
        assertEquals(1, Operaciones.FIND_ALL_CATEGORIAS);
        Argumentos[] args = {null, new Argumentos.Login("F1", "secreto"),
                new Argumentos.CambioClave("F1", "anterior", "nueva"),
                new Argumentos.Funcionario(FUN), new Argumentos.Funcionario(FUN),
                new Argumentos.Funcionario(FUN), new Argumentos.Funcionario(FUN),
                new Argumentos.Categoria(CAT), new Argumentos.Categoria(CAT),
                new Argumentos.Categoria(CAT), new Argumentos.Categoria(CAT),
                new Argumentos.Recurso(REC), new Argumentos.Recurso(REC), new Argumentos.Recurso(REC),
                new Argumentos.BuscarRecursos(CAT, "Proyector"), new Argumentos.Funcionario(FUN),
                new Argumentos.CrearReserva(FUN, "Clase", FECHA, LocalTime.of(8, 0),
                        LocalTime.of(9, 30), new CategoriaDto[]{CAT}),
                new Argumentos.Reserva(RES), new Argumentos.Fecha(FECHA),
                new Argumentos.Periodo(FECHA, FECHA), new Argumentos.Periodo(FECHA, FECHA),
                new Argumentos.Periodo(FECHA, FECHA), new Argumentos.ExtraerReserva("reservar")};
        Resultado ok = new Resultado.Confirmacion();
        Resultado cats = new Resultado.Categorias(new CategoriaDto[]{CAT});
        Resultado reservas = new Resultado.Reservas(new ReservaDto[]{RES});
        Resultado[] resultados = {cats, new Resultado.Usuario(new UsuarioDto(FUN.id(), FUN.rol(), FUN)),
                ok, ok, ok, ok, new Resultado.Funcionarios(new FuncionarioDto[]{FUN}),
                ok, ok, ok, cats, ok, ok, ok, new Resultado.Recursos(new RecursoDto[]{REC}),
                reservas, new Resultado.Reserva(RES), ok, reservas, reservas,
                ConvertidorDto.categorias(Map.of("Equipo", 2L)),
                ConvertidorDto.semanas(Map.of(FECHA, 5L)), new Resultado.Extraccion(EXT)};
        for (int i = 0; i < args.length; i++) {
            Solicitud solicitud = new Solicitud(i + 1, args[i]);
            Solicitud recibida = (Solicitud) vuelta(solicitud, FiltrosProtocolo.solicitudes());
            assertEquals(i + 1, recibida.operacion());
            assertArrayEquals(bytes(solicitud), bytes(recibida));
            Respuesta respuesta = Respuesta.exito(i + 1, resultados[i]);
            Respuesta recibidaRespuesta = (Respuesta) vuelta(respuesta, FiltrosProtocolo.respuestas());
            Operaciones.validarResultado(i + 1, recibidaRespuesta.resultadoTipado());
            assertArrayEquals(bytes(respuesta), bytes(recibidaRespuesta));
        }
        assertThrows(IllegalArgumentException.class, () -> new Solicitud(999));
        assertThrows(IllegalArgumentException.class, () -> new Solicitud(Operaciones.LOGIN));
        assertThrows(IllegalArgumentException.class, () -> new Solicitud(Operaciones.LOGIN, args[2]));
        assertThrows(IllegalArgumentException.class, () -> Respuesta.exito(Operaciones.LOGIN, ok));
    }

    @Test void conversionesConservanRelacionesYSubtipoSinClaves() throws Exception {
        Funcionario f = new Funcionario("F1", "CLAVE_NO_TRANSMITIR_987", "Ana", "123");
        UsuarioDto u = ConvertidorDto.usuario(f);
        Usuario reconstruido = ConvertidorDto.usuario((UsuarioDto) vuelta(u, FiltrosProtocolo.respuestas()));
        assertInstanceOf(Funcionario.class, reconstruido);
        assertEquals("", reconstruido.getClave());
        assertEquals("Ana", ((Funcionario) reconstruido).getNombre());
        assertEquals(Usuario.ADMIN, ConvertidorDto.usuario(ConvertidorDto.usuario(
                new Usuario("A1", "CLAVE_NO_TRANSMITIR_987", Usuario.ADMIN))).getRol());
        Reserva e = ConvertidorDto.reserva(RES);
        e.setFuncionario(f);
        ReservaDto dto = ConvertidorDto.reserva(e);
        Reserva copia = ConvertidorDto.reserva((ReservaDto) vuelta(dto, FiltrosProtocolo.respuestas()));
        assertEquals("F1", copia.getFuncionario().getId());
        assertEquals("", copia.getFuncionario().getClave());
        assertEquals("C1", copia.getRecursos().get(0).getCategoria().getId());
        assertEquals("Proyector", copia.getRecursos().get(0).getDescripcion());
        assertEquals(e.getEstado(), copia.getEstado());
        assertEquals(e.getFecha(), copia.getFecha());
        for (Resultado resultado : List.of(new Resultado.Usuario(u),
                new Resultado.Funcionarios(new FuncionarioDto[]{ConvertidorDto.funcionario(f)}),
                new Resultado.Reservas(new ReservaDto[]{dto}))) {
            byte[] serializado = bytes(new Respuesta(null, null, resultado, null));
            assertFalse(new String(serializado, StandardCharsets.ISO_8859_1).contains("CLAVE_NO_TRANSMITIR_987"));
        }
        for (Class<?> tipo : List.of(UsuarioDto.class, FuncionarioDto.class))
            assertTrue(Arrays.stream(tipo.getRecordComponents()).noneMatch(c -> c.getName().equals("clave")));
        assertFalse(new Argumentos.Login("F1", "CLAVE_NO_TRANSMITIR_987").toString().contains("CLAVE_NO"));
        ReservaExtraccion extraida = ConvertidorDto.extraccion(EXT);
        assertArrayEquals(EXT.categoriasRecurso(), ConvertidorDto.extraccion(extraida).categoriasRecurso());
        assertNull(ConvertidorDto.extraccion(new ReservaExtraccion()).categoriasRecurso());
        assertEquals(Map.of("Equipo", 2L), ConvertidorDto.categorias(ConvertidorDto.categorias(Map.of("Equipo", 2L))));
        assertEquals(Map.of(FECHA, 3L), ConvertidorDto.semanas(ConvertidorDto.semanas(Map.of(FECHA, 3L))));
    }

    @Test void filtrosRechazanObjetosYDireccionIncorrecta() throws Exception {
        assertThrows(InvalidClassException.class, () -> vuelta(new java.io.File("x"), FiltrosProtocolo.respuestas()));
        assertThrows(InvalidClassException.class, () -> vuelta(new Argumentos.Login("a", "b"), FiltrosProtocolo.respuestas()));
        assertThrows(InvalidClassException.class, () -> vuelta(new Resultado.Confirmacion(), FiltrosProtocolo.solicitudes()));
        assertThrows(InvalidClassException.class, () -> vuelta(new CategoriaDto[10001], FiltrosProtocolo.respuestas()));
    }

    @Test void compatibilidadFiltrosOriginalesYErrores() throws Exception {
        ObjectInputFilter servidor = ObjectInputFilter.Config.createFilter(
                "maxdepth=5;maxrefs=20;maxbytes=4096;reservas.protocol.Solicitud;!*");
        assertEquals(new Solicitud(1), vuelta(new Solicitud(1), servidor));
        ObjectInputFilter cliente = ObjectInputFilter.Config.createFilter(
                "maxdepth=5;maxrefs=50000;maxarray=10000;maxbytes=1048576;"
                + "reservas.protocol.Respuesta;reservas.protocol.CategoriaDto;!*");
        Respuesta original = new Respuesta(new CategoriaDto[]{CAT}, null);
        Respuesta recibida = (Respuesta) vuelta(original, cliente);
        assertArrayEquals(original.categorias(), recibida.categorias());
        Respuesta errorViejo = (Respuesta) vuelta(new Respuesta(null, "Error controlado"),
                ObjectInputFilter.Config.createFilter("reservas.protocol.Respuesta;!*"));
        assertEquals("Error controlado", errorViejo.error());
        Respuesta error = (Respuesta) vuelta(Respuesta.fallo(CodigoError.NEGOCIO, "No disponible"),
                FiltrosProtocolo.respuestas());
        assertEquals(CodigoError.NEGOCIO, error.codigoErrorControlado());
        assertNull(error.resultadoTipado());
        assertThrows(IllegalArgumentException.class, () -> new Respuesta(null, null));
        assertThrows(IllegalArgumentException.class, () -> new Respuesta(new CategoriaDto[]{CAT}, "error"));
    }

    @Test void arreglosSonCopiasDefensivas() {
        RecursoDto[] recursos = {REC};
        ReservaDto r = new ReservaDto("1", FUN, "Clase", FECHA, null, null, recursos, "ACTIVA");
        recursos[0] = null;
        r.recursos()[0] = null;
        assertEquals(REC, r.recursos()[0]);
        CategoriaDto[] categorias = {CAT};
        Resultado.Categorias resultado = new Resultado.Categorias(categorias);
        categorias[0] = null;
        resultado.categorias()[0] = null;
        assertEquals(CAT, resultado.categorias()[0]);
    }

    @Test void identificacionNoEstableceConexiones() throws Exception {
        for (Object mensaje : List.of(new Identificacion(TipoConexion.SYNC, null),
                new Identificacion(TipoConexion.ASYNC, "sesion-temporal"), new SesionCreada("sesion-temporal"))) {
            assertEquals(mensaje, vuelta(mensaje, FiltrosProtocolo.identificacion()));
        }
        assertThrows(IllegalArgumentException.class, () -> new Identificacion(TipoConexion.ASYNC, null));
        assertThrows(IllegalArgumentException.class, () -> new SesionCreada(" "));
    }
}
