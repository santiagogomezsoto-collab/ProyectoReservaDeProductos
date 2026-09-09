package reservas.presentation.recursos;

import reservas.logic.Recurso;
import reservas.presentation.AbstractTableModel;

import java.util.List;

public class TableModel extends AbstractTableModel<Recurso> implements javax.swing.table.TableModel {
    public TableModel(int[] cols, List<Recurso> rows) {
        super(cols, rows);
    }

    public static final int ID = 0;
    public static final int CATEGORIA = 1;
    public static final int DESCRIPCION = 2;

    @Override
    protected void initColNames() {
        colNames = new String[3];
        colNames[ID] = "Id";
        colNames[CATEGORIA] = "Categoria";
        colNames[DESCRIPCION] = "Descripcion";
    }

    @Override
    protected Object getPropetyAt(Recurso e, int col) {
        switch (cols[col]) {
            case ID:
                return e.getId();
            case CATEGORIA:
                return e.getCategoria().getDescripcion();
            case DESCRIPCION:
                return e.getDescripcion();
            default:
                return "";
        }
    }
}