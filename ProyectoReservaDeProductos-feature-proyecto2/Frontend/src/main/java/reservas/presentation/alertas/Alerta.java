package reservas.presentation.alertas;

import java.util.List;

public record Alerta(String id, String encargado, String actividad,
                     String fecha, String desde, String hasta, List<String> recursos) {}