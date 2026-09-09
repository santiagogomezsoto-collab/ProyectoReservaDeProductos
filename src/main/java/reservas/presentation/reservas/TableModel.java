package reservas.presentation.reservas;

import reservas.logic.Recurso;
import reservas.logic.Reserva;
import reservas.presentation.AbstractTableModel;

import java.util.List;
import java.util.stream.Collectors;

public class TableModel extends AbstractTableModel<Reserva> implements javax.swing.table.TableModel {
    public TableModel(int[] cols, List<Reserva> rows) {
        super(cols, rows);
    }

    public static final int ID = 0;
    public static final int ACTIVIDAD = 1;
    public static final int FECHA = 2;
    public static final int HORARIO = 3;
    public static final int RECURSOS = 4;
    public static final int ESTADO = 5;

    @Override
    protected void initColNames() {
        colNames = new String[6];
        colNames[ID] = "Id";
        colNames[ACTIVIDAD] = "Actividad";
        colNames[FECHA] = "Fecha";
        colNames[HORARIO] = "Horario";
        colNames[RECURSOS] = "Recursos";
        colNames[ESTADO] = "Estado";
    }

    @Override
    protected Object getPropetyAt(Reserva e, int col) {
        switch (cols[col]) {
            case ID:
                return e.getId();
            case ACTIVIDAD:
                return e.getActividad();
            case FECHA:
                return e.getFecha().toString();
            case HORARIO:
                return e.getHoraInicio() + " - " + e.getHoraFin();
            case RECURSOS:
                return e.getRecursos().stream().map(Recurso::getId).collect(Collectors.joining(", "));
            case ESTADO:
                return e.getEstado();
            default:
                return "";
        }
    }
}