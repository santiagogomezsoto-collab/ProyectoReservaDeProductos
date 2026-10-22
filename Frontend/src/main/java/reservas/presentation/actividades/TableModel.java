package reservas.presentation.actividades;

import reservas.logic.Reserva;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TableModel extends AbstractTableModel {
    private List<LocalTime> horas;
    private List<LocalDate> dias;
    private List<Reserva> reservas;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");

    public TableModel(List<LocalTime> horas, List<LocalDate> dias, List<Reserva> reservas) {
        this.horas = horas;
        this.dias = dias;
        this.reservas = reservas;
    }

    @Override
    public int getRowCount() {
        return horas.size();
    }

    @Override
    public int getColumnCount() {
        return dias.size() + 1;
    }

    @Override
    public String getColumnName(int col) {
        if (col == 0) return "Hora";
        return dias.get(col - 1).format(FMT);
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return horas.get(row).toString();
        }
        LocalDate dia = dias.get(col - 1);
        LocalTime hora = horas.get(row);

        return reservas.stream()
                .filter(r -> r.getFecha().equals(dia))
                .filter(r -> !hora.isBefore(r.getHoraInicio()) && hora.isBefore(r.getHoraFin()))
                .map(r -> r.getActividad() + " (" + r.getFuncionario().getNombre() + ")")
                .collect(Collectors.joining(" | "));
    }
}
