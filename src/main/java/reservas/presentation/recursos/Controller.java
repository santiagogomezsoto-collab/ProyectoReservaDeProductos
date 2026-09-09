package reservas.presentation.recursos;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.borders.Border;
import reservas.logic.CategoriaRecurso;
import reservas.logic.Recurso;
import reservas.logic.Service;

import java.awt.*;
import java.io.File;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
        model.setCategorias(Service.instance().findAllCategorias());
        model.setList(Service.instance().search(null, ""));
    }

    public void create(Recurso e) throws Exception {
        Service.instance().create(e);
        model.setCurrent(new Recurso());
        model.setList(Service.instance().search(null, ""));
    }

    public void update(Recurso e) throws Exception {
        Service.instance().update(e);
        model.setCurrent(new Recurso());
        model.setList(Service.instance().search(null, ""));
    }

    public void delete(Recurso e) throws Exception {
        Service.instance().delete(e);
        model.setCurrent(new Recurso());
        model.setList(Service.instance().search(null, ""));
    }

    public void search(CategoriaRecurso categoria, String descripcion) {
        model.setList(Service.instance().search(categoria, descripcion));
    }

    public void clear() {
        model.setCurrent(new Recurso());
    }

    public void edit(int row) {
        Recurso e = model.getList().get(row);
        model.setCurrent(e);
    }

    public void print() throws Exception {
        String dest = "recursos.pdf";
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdf = new PdfDocument(writer);

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        Table header = new Table(1);
        header.setWidth(400);
        header.setHorizontalAlignment(HorizontalAlignment.CENTER);
        header.addCell(getCell(new Paragraph("Listado de Recursos").setFont(font).setBold().setFontSize(18), TextAlignment.CENTER, false));
        document.add(header);

        Table table = new Table(3);
        table.setWidth(document.getPdfDocument().getDefaultPageSize().getWidth() - 40);
        table.addCell(getCell(new Paragraph("Id").setFont(font).setBold(), TextAlignment.CENTER, true));
        table.addCell(getCell(new Paragraph("Categoria").setFont(font).setBold(), TextAlignment.CENTER, true));
        table.addCell(getCell(new Paragraph("Descripcion").setFont(font).setBold(), TextAlignment.CENTER, true));

        for (Recurso r : model.getList()) {
            table.addCell(getCell(new Paragraph(r.getId()).setFont(font), TextAlignment.LEFT, true));
            table.addCell(getCell(new Paragraph(r.getCategoria().getDescripcion()).setFont(font), TextAlignment.LEFT, true));
            table.addCell(getCell(new Paragraph(r.getDescripcion()).setFont(font), TextAlignment.LEFT, true));
        }
        document.add(table);

        document.close();
        openPdf(dest);
    }

    private Cell getCell(Paragraph paragraph, TextAlignment alignment, boolean hasBorder) {
        Cell cell = new Cell().add(paragraph);
        cell.setPadding(5);
        cell.setTextAlignment(alignment);
        if (!hasBorder) cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    private void openPdf(String path) {
        try {
            File pdfFile = new File(path);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}