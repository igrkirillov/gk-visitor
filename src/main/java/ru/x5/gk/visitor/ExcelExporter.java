package ru.x5.gk.visitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelExporter {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    private final ResultData resultData;

    public ExcelExporter(ResultData resultData) {
        this.resultData = resultData;
        workbook = new XSSFWorkbook();
    }
 
 
    private void writeHeaderLine() {
        sheet = workbook.createSheet("Data");
         
        Row row = sheet.createRow(0);
         
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        int col = 0;
        for (String header : resultData.getHeaders()) {
            createCell(row, col, header, style);
            ++col;
        }
    }
     
    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }
     
    private void writeDataLines() {
        int rowCount = 1;
 
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);
                 
        for (Object[] rowData : resultData.getRows()) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;
            for (Object colValue : rowData) {
                createCell(row, columnCount++, colValue, style);
            }

        }
    }
     
    public void exportTo(File file) {
        resultData.flush();
        try {
            writeHeaderLine();
            writeDataLines();
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
                workbook.close();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}