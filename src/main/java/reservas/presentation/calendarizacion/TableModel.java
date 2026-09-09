package reservas.presentation.calendarizacion;

import reservas.logic.Recurso;
import reservas.logic.Reserva;

import javax.swing.table.AbstractTableModel;
import java.time.LocalTime;
import java.util.List;

public class TableModel extends AbstractTableModel {
    private List<LocalTime> horas;
    private List<Recurso> recursos;
    private List<Reserva> reservas;

    public TableModel(List<LocalTime> horas, List<Recurso> recursos, List<Reserva> reservas) {
        this.horas = horas;
        this.recursos = recursos;
        this.reservas = reservas;
    }

    @Override
    public int getRowCount() {
        return horas.size();
    }

    @Override
    public int getColumnCount() {
        return recursos.size() + 1;
    }

    @Override
    public String getColumnName(int col) {
        if (col == 0) return "Hora";
        return recursos.get(col - 1).getDescripcion();
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return horas.get(row).toString();
        }
        Recurso recurso = recursos.get(col - 1);
        LocalTime hora = horas.get(row);

        Reserva reserva = reservas.stream()
                .filter(r -> r.getRecursos().stream().anyMatch(rec -> rec.getId().equals(recurso.getId())))
                .filter(r -> !hora.isBefore(r.getHoraInicio()) && hora.isBefore(r.getHoraFin()))
                .findFirst()
                .orElse(null);

        if (reserva == null) return "";
        return reserva.getActividad() + " - " + reserva.getFuncionario().getNombre();
    }
}