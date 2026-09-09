package reservas.presentation.calendarizacion;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Reserva;
import reservas.logic.Service;

import java.time.LocalDate;
import java.util.List;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
        model.setCategorias(Service.instance().findAllCategorias());
    }

    public void cargar(CategoriaRecurso categoria, LocalDate fecha) {
        List<Reserva> reservas = Service.instance().findReservasActivasPorFecha(fecha);
        model.setMatriz(Service.instance().search(categoria, ""), reservas);
    }

    public void print() throws Exception {
        String dest = "calendarizacion.pdf";
        com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(dest);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4.rotate());
        document.setMargins(20, 20, 20, 20);

        document.add(new com.itextpdf.layout.element.Paragraph("Calendarizacion de Recursos").setFont(font).setBold().setFontSize(18)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

        int numCols = model.getRecursos().size() + 1;
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(numCols);
        table.setWidth(document.getPdfDocument().getDefaultPageSize().getWidth() - 40);

        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Hora").setFont(font).setBold()));
        for (reservas.logic.Recurso r : model.getRecursos()) {
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getDescripcion()).setFont(font).setBold()));
        }

        TableModel tm = new TableModel(model.getHoras(), model.getRecursos(), model.getReservas());
        for (int row = 0; row < tm.getRowCount(); row++) {
            for (int col = 0; col < tm.getColumnCount(); col++) {
                table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(tm.getValueAt(row, col).toString()).setFont(font)));
            }
        }
        document.add(table);
        document.close();
        openPdf(dest);
    }

    private com.itextpdf.layout.element.Cell getCell(com.itextpdf.layout.element.Paragraph paragraph) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(paragraph);
        cell.setPadding(5);
        return cell;
    }

    private void openPdf(String path) {
        try {
            java.io.File pdfFile = new java.io.File(path);
            if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}