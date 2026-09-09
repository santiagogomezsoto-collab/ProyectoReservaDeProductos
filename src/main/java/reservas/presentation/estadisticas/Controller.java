package reservas.presentation.estadisticas;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import reservas.logic.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
    }

    public void cargarRecursos(LocalDate desde, LocalDate hasta) {
        Map<String, Long> mapa = Service.instance().estadisticasRecursosPorCategoria(desde, hasta);
        List<CategoriaCantidad> lista = new ArrayList<>();
        for (Map.Entry<String, Long> e : mapa.entrySet()) {
            lista.add(new CategoriaCantidad(e.getKey(), e.getValue()));
        }
        model.setRecursos(lista);
    }

    public void cargarActividades(LocalDate desde, LocalDate hasta) {
        Map<LocalDate, Long> mapa = Service.instance().estadisticasActividadesPorSemana(desde, hasta);
        List<SemanaCantidad> lista = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> e : mapa.entrySet()) {
            lista.add(new SemanaCantidad(e.getKey(), e.getValue()));
        }
        model.setActividades(lista);
    }

    public ChartPanel construirGraficoRecursos() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (CategoriaCantidad c : model.getRecursos()) {
            dataset.addValue(c.getCantidad(), "Recurso", c.getCategoria());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Recursos Usados", "Categoria", "Cantidad",
                dataset, PlotOrientation.VERTICAL, true, true, false);
        return new ChartPanel(chart);
    }

    public ChartPanel construirGraficoActividades() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (SemanaCantidad s : model.getActividades()) {
            dataset.addValue(s.getCantidad(), "Semana", s.getSemanaInicio().toString());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Actividades Realizadas", "Semana", "Cantidad",
                dataset, PlotOrientation.VERTICAL, true, true, false);
        return new ChartPanel(chart);
    }

    public void print() throws Exception {
        String dest = "estadisticas.pdf";
        com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(dest);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        document.add(new com.itextpdf.layout.element.Paragraph("Estadisticas").setFont(font).setBold().setFontSize(18)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

        document.add(new com.itextpdf.layout.element.Paragraph("Recursos por categoria").setFont(font).setBold().setFontSize(14));
        com.itextpdf.layout.element.Table t1 = new com.itextpdf.layout.element.Table(2);
        t1.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Categoria").setFont(font).setBold()));
        t1.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Cantidad").setFont(font).setBold()));
        for (CategoriaCantidad c : model.getRecursos()) {
            t1.addCell(getCell(new com.itextpdf.layout.element.Paragraph(c.getCategoria()).setFont(font)));
            t1.addCell(getCell(new com.itextpdf.layout.element.Paragraph(String.valueOf(c.getCantidad())).setFont(font)));
        }
        document.add(t1);

        document.add(new com.itextpdf.layout.element.Paragraph("Actividades por semana").setFont(font).setBold().setFontSize(14));
        com.itextpdf.layout.element.Table t2 = new com.itextpdf.layout.element.Table(2);
        t2.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Semana").setFont(font).setBold()));
        t2.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Cantidad").setFont(font).setBold()));
        for (SemanaCantidad s : model.getActividades()) {
            t2.addCell(getCell(new com.itextpdf.layout.element.Paragraph(s.getSemanaInicio().toString()).setFont(font)));
            t2.addCell(getCell(new com.itextpdf.layout.element.Paragraph(String.valueOf(s.getCantidad())).setFont(font)));
        }
        document.add(t2);

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