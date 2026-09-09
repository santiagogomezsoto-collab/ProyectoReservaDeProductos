package reservas.presentation.reservas;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Funcionario;
import reservas.logic.Reserva;
import reservas.logic.Service;
import reservas.presentation.Sesion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
        model.setCategorias(Service.instance().findAllCategorias());
        model.setList(Service.instance().findReservas((Funcionario) Sesion.getUsuario()));
    }

    public void reservar(String actividad, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                         List<CategoriaRecurso> categorias) throws Exception {
        Service.instance().crearReserva((Funcionario) Sesion.getUsuario(), actividad, fecha, horaInicio, horaFin, categorias);
        model.setCurrent(new NuevaReservaData());
        model.setList(Service.instance().findReservas((Funcionario) Sesion.getUsuario()));
    }

    public void cancelar(Reserva e) throws Exception {
        Service.instance().cancelarReserva(e);
        model.setList(Service.instance().findReservas((Funcionario) Sesion.getUsuario()));
    }

    public void limpiar() {
        model.setCurrent(new NuevaReservaData());
    }

    public void extraerConIA(String frase) throws Exception {
        NuevaReservaData datos = reservas.ia.IAExtractorService.instance().extraer(frase, model.getCategorias());
        model.setCurrent(datos);
    }

    public void print() throws Exception {
        String dest = "reservas.pdf";
        com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(dest);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        document.add(new com.itextpdf.layout.element.Paragraph("Mis Reservas").setFont(font).setBold().setFontSize(18)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(6);
        table.setWidth(document.getPdfDocument().getDefaultPageSize().getWidth() - 40);
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Id").setFont(font).setBold()));
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Actividad").setFont(font).setBold()));
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Fecha").setFont(font).setBold()));
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Horario").setFont(font).setBold()));
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Recursos").setFont(font).setBold()));
        table.addCell(getCell(new com.itextpdf.layout.element.Paragraph("Estado").setFont(font).setBold()));

        for (Reserva r : model.getList()) {
            String recursos = r.getRecursos().stream().map(reservas.logic.Recurso::getId).collect(Collectors.joining(", "));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getId()).setFont(font)));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getActividad()).setFont(font)));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getFecha().toString()).setFont(font)));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getHoraInicio() + " - " + r.getHoraFin()).setFont(font)));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(recursos).setFont(font)));
            table.addCell(getCell(new com.itextpdf.layout.element.Paragraph(r.getEstado()).setFont(font)));
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