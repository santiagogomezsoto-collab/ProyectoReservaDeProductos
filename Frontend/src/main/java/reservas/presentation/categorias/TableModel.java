package reservas.presentation.categorias;

import reservas.logic.CategoriaRecurso;
import reservas.presentation.AbstractTableModel;

import java.util.List;

public class TableModel extends AbstractTableModel<CategoriaRecurso> implements javax.swing.table.TableModel {
    public TableModel(int[] cols, List<CategoriaRecurso> rows) {
        super(cols, rows);
    }

    public static final int ID = 0;
    public static final int DESCRIPCION = 1;

    @Override
    protected void initColNames() {
        colNames = new String[2];
        colNames[ID] = "Id";
        colNames[DESCRIPCION] = "Descripcion";
    }

    @Override
    protected Object getPropetyAt(CategoriaRecurso e, int col) {
        switch (cols[col]) {
            case ID:
                return e.getId();
            case DESCRIPCION:
                return e.getDescripcion();
            default:
                return "";
        }
    }
}