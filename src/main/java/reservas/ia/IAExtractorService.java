package reservas.ia;

import reservas.logic.CategoriaRecurso;
import reservas.presentation.reservas.NuevaReservaData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IAExtractorService {
    private static IAExtractorService theInstance;

    public static IAExtractorService instance() {
        if (theInstance == null) theInstance = new IAExtractorService();
        return theInstance;
    }

    private IAExtractorService() {
    }

    public NuevaReservaData extraer(String frase, List<CategoriaRecurso> categoriasDisponibles) throws Exception {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("No se configuro ANTHROPIC_API_KEY");
        }

        StringBuilder listaCategorias = new StringBuilder();
        for (CategoriaRecurso c : categoriasDisponibles) {
            listaCategorias.append("- ").append(c.getId()).append(": ").append(c.getDescripcion()).append("\n");
        }

        String prompt = "Extrae de la siguiente frase los datos de una reserva. " +
                "Responde SOLO con JSON, sin texto adicional, con este formato exacto: " +
                "{\"actividad\":\"...\",\"fecha\":\"YYYY-MM-DD\",\"horaInicio\":\"HH:mm\",\"horaFin\":\"HH:mm\",\"categoriaIds\":[\"...\"]}. " +
                "Las categorias disponibles son:\n" + listaCategorias +
                "Usa solo esos ids en categoriaIds, los que mejor calcen con lo pedido en la frase. " +
                "Fecha de referencia de hoy: " + LocalDate.now() + ". Frase: \"" + frase + "\"";

        String body = "{"
                + "\"model\":\"claude-sonnet-4-6\","
                + "\"max_tokens\":500,"
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}]"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String json = extraerCampoTexto(response.body());
        return parsear(json, categoriasDisponibles);
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String extraerCampoTexto(String respuestaCompleta) throws Exception {
        Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"\\s*}", Pattern.DOTALL).matcher(respuestaCompleta);
        if (!m.find()) {
            throw new Exception("No se pudo interpretar la respuesta de la IA");
        }
        return m.group(1).replace("\\\"", "\"").replace("\\n", "\n");
    }

    private NuevaReservaData parsear(String json, List<CategoriaRecurso> categoriasDisponibles) throws Exception {
        NuevaReservaData datos = new NuevaReservaData();

        datos.setActividad(extraerValor(json, "actividad"));

        String fechaStr = extraerValor(json, "fecha");
        if (fechaStr != null && !fechaStr.isEmpty()) {
            datos.setFecha(LocalDate.parse(fechaStr));
        }

        String horaInicioStr = extraerValor(json, "horaInicio");
        if (horaInicioStr != null && !horaInicioStr.isEmpty()) {
            datos.setHoraInicio(LocalTime.parse(horaInicioStr));
        }

        String horaFinStr = extraerValor(json, "horaFin");
        if (horaFinStr != null && !horaFinStr.isEmpty()) {
            datos.setHoraFin(LocalTime.parse(horaFinStr));
        }

        List<CategoriaRecurso> seleccionadas = new ArrayList<>();
        Matcher m = Pattern.compile("\"([A-Za-z0-9\\-]+)\"").matcher(extraerArreglo(json, "categoriaIds"));
        while (m.find()) {
            String id = m.group(1);
            for (CategoriaRecurso c : categoriasDisponibles) {
                if (c.getId().equals(id)) {
                    seleccionadas.add(c);
                }
            }
        }
        datos.setCategoriasSeleccionadas(seleccionadas);

        return datos;
    }

    private String extraerValor(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"(.*?)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private String extraerArreglo(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL).matcher(json);
        return m.find() ? m.group(1) : "";
    }
}