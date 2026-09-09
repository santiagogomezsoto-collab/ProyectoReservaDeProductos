package reservas.presentation.categorias;

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
        model.setList(Service.instance().findAllCategorias());
    }

    public void create(CategoriaRecurso e) throws Exception {
        Service.instance().create(e);
        model.setCurrent(new CategoriaRecurso());
        model.setList(Service.instance().findAllCategorias());
    }

    public void update(CategoriaRecurso e) throws Exception {
        Service.instance().update(e);
        model.setCurrent(new CategoriaRecurso());
        model.setList(Service.instance().findAllCategorias());
    }

    public void delete(CategoriaRecurso e) throws Exception {
        Service.instance().delete(e);
        model.setCurrent(new CategoriaRecurso());
        model.setList(Service.instance().findAllCategorias());
    }

    public void search(String descripcion) {
        CategoriaRecurso e = new CategoriaRecurso();
        e.setDescripcion(descripcion);
        model.setList(Service.instance().search(e));
    }

    public void clear() {
        model.setCurrent(new CategoriaRecurso());
    }

    public void edit(int row) {
        CategoriaRecurso e = model.getList().get(row);
        model.setCurrent(e);
    }

    public void print() throws Exception {
        String dest = "categorias.pdf";
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdf = new PdfDocument(writer);

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        Table header = new Table(1);
        header.setWidth(400);
        header.setHorizontalAlignment(HorizontalAlignment.CENTER);
        header.addCell(getCell(new Paragraph("Listado de Categorias").setFont(font).setBold().setFontSize(18), TextAlignment.CENTER, false));
        document.add(header);

        Table table = new Table(2);
        table.setWidth(document.getPdfDocument().getDefaultPageSize().getWidth() - 40);
        table.addCell(getCell(new Paragraph("Id").setFont(font).setBold(), TextAlignment.CENTER, true));
        table.addCell(getCell(new Paragraph("Descripcion").setFont(font).setBold(), TextAlignment.CENTER, true));

        for (CategoriaRecurso c : model.getList()) {
            table.addCell(getCell(new Paragraph(c.getId()).setFont(font), TextAlignment.LEFT, true));
            table.addCell(getCell(new Paragraph(c.getDescripcion()).setFont(font), TextAlignment.LEFT, true));
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
                } else {
                    System.out.println("AWT Desktop is not supported on this platform.");
                }
            } else {
                System.out.println("The target PDF file does not exist.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}