package reservas.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias (Surefire) de la eleccion del archivo de conexion.
 *
 * No abren ninguna conexion ni necesitan MySQL: solo comprueban de donde decide
 * Database leer su configuracion. Es una pieza pequeña pero delicada, porque es
 * la que permite que las pruebas de integracion trabajen contra una base
 * aparte en vez de borrar los datos de desarrollo.
 */
class DatabaseConfiguracionTest {

    @AfterEach
    void limpiarPropiedad() {
        System.clearProperty(Database.PROPERTIES_SYSTEM_PROPERTY);
    }

    @Test
    @DisplayName("Sin propiedad del sistema se usa el archivo de produccion")
    void porDefecto() {
        System.clearProperty(Database.PROPERTIES_SYSTEM_PROPERTY);
        assertEquals("/reservas.properties", Database.propertiesFileName());
    }

    @Test
    @DisplayName("La propiedad del sistema cambia el archivo de conexion")
    void conPropiedad() {
        System.setProperty(Database.PROPERTIES_SYSTEM_PROPERTY, "/reservas-test.properties");
        assertEquals("/reservas-test.properties", Database.propertiesFileName());
    }

    @Test
    @DisplayName("Un nombre sin barra inicial se normaliza anadiendosela")
    void sinBarraInicial() {
        System.setProperty(Database.PROPERTIES_SYSTEM_PROPERTY, "reservas-test.properties");
        assertEquals("/reservas-test.properties", Database.propertiesFileName());
    }

    @Test
    @DisplayName("Los espacios alrededor del valor se ignoran")
    void conEspacios() {
        System.setProperty(Database.PROPERTIES_SYSTEM_PROPERTY, "  /reservas-test.properties  ");
        assertEquals("/reservas-test.properties", Database.propertiesFileName());
    }

    @Test
    @DisplayName("El valor por defecto es exactamente el que documenta la constante")
    void constanteCoherente() {
        System.clearProperty(Database.PROPERTIES_SYSTEM_PROPERTY);
        assertEquals(Database.PROPERTIES_FILE_NAME, Database.propertiesFileName());
    }

    @Test
    @DisplayName("El nombre de la propiedad del sistema no cambia sin querer")
    void nombreDeLaPropiedad() {
        // El README y la configuracion de Maven usan este texto literal.
        // Si alguien lo renombra, las pruebas de integracion apuntarian en
        // silencio a la base real.
        assertEquals("reservas.db.properties", Database.PROPERTIES_SYSTEM_PROPERTY);
    }
}
