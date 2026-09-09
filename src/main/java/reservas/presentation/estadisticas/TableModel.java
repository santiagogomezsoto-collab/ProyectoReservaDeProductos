package reservas.presentation.estadisticas;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class TableModel {

    public static class Recursos extends AbstractTableModel {
        private List<CategoriaCantidad> datos;

        public Recursos(List<CategoriaCantidad> datos) {
            this.datos = datos;
        }

        @Override
        public int getRowCount() {
            return datos.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? "Categoria" : "Cantidad";
        }

        @Override
        public Object getValueAt(int row, int col) {
            CategoriaCantidad c = datos.get(row);
            return col == 0 ? c.getCategoria() : c.getCantidad();
        }
    }

    public static class Actividades extends AbstractTableModel {
        private List<SemanaCantidad> datos;

        public Actividades(List<SemanaCantidad> datos) {
            this.datos = datos;
        }

        @Override
        public int getRowCount() {
            return datos.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? "Semana" : "Cantidad";
        }

        @Override
        public Object getValueAt(int row, int col) {
            SemanaCantidad s = datos.get(row);
            return col == 0 ? s.getSemanaInicio().toString() : s.getCantidad();
        }
    }
}