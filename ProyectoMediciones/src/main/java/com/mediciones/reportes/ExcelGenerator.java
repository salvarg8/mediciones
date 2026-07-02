package com.mediciones.reportes;

import com.mediciones.model.Medicion;
import com.mediciones.model.Valvula;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExcelGenerator {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private Medicion medicion;

    private static final Logger logger = LoggerFactory.getLogger(ExcelGenerator.class);

    public ExcelGenerator() {
    }

    /**
     * Genera el reporte Excel utilizando el snapshot de medición y objetos en memoria.
     */
    public void generarExcel(Medicion medicion, String archivoExcel) throws IOException {
        if (medicion == null || archivoExcel == null) {
            throw new IllegalArgumentException("La medición y la ruta de destino no pueden ser nulas");
        }
        this.medicion = medicion; // Guardamos el snapshot en la instancia

        // 1. Carga segura de la plantilla Excel desde los recursos del proyecto
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Plantillas/plantilla.xlsx")) {
            if (is == null) {
                logger.warn("ADVERTENCIA: No se encontró 'Plantillas/plantilla.xlsx' en los recursos. Creando nuevo workbook vacío.");
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Reporte");
            } else {
                workbook = new XSSFWorkbook(is);
                sheet = workbook.getSheetAt(0);
            }
        } catch (IOException e) {
            logger.error("Error al cargar la plantilla Excel desde los recursos.", e);
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Reporte");
        }

        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        List<Double> tiemposRaw = medicion.getTiemposMedicion();
        List<Double> valoresRaw = medicion.getValoresMedicion();

        boolean comenzarGrafico = false;
        double tiempoInicial = 0.0;

        Double presionSolicitada = medicion.getPresionSolicitada();
        double umbralDisparo = (presionSolicitada != null) ? (0.75 * presionSolicitada) : 0.0;

        for (int i = 0; i < tiemposRaw.size(); i++) {
            double px = tiemposRaw.get(i);
            double py = valoresRaw.get(i);

            if (!comenzarGrafico) {
                if (py >= umbralDisparo) {
                    comenzarGrafico = true;
                    tiempoInicial = px;
                }
            }

            if (comenzarGrafico) {
                xValues.add(px - tiempoInicial);
                yValues.add(py);
            }
        }

        if (xValues.isEmpty()) {
            for (int i = 0; i < tiemposRaw.size(); i++) {
                xValues.add(tiemposRaw.get(i));
                yValues.add(valoresRaw.get(i));
            }
        }

        agregarDatosHojaSecundaria(xValues, yValues);

        crearGrafico(xValues, yValues);

        if (medicion.getValvula() != null) {
            escribirDatosValvula();
        }

        //escribirDatosCSVHeader();
        escribirFechaActual();
        escribirPrimeraTemperatura();
        escribirPresionAperturaSolicitada();

        guardarArchivoExcel(archivoExcel);

        /*File archivoExcelFile = new File(archivoExcel);
        try {
            if (archivoExcelFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivoExcelFile);
            } else {
                logger.warn("El entorno actual no soporta la apertura automática de archivos.");
            }
        } catch (Exception ex) {
            logger.error("No se pudo ejecutar la apertura automática del archivo reporte: " + archivoExcel, ex);
        } */
    }

    private void escribirPresionAperturaSolicitada() {
        if (medicion.getPresionSolicitada() != null) {
            escribirCelda(11, 6, medicion.getPresionSolicitada() + " " + medicion.getUnidadPresion());
        } else {
            escribirCelda(11, 6, "N/A");
        }
    }

    private void escribirDatosValvula() {
        Valvula valvula = medicion.getValvula();
        if (valvula != null) {
            // Formateo seguro para máximo y recuperación directos del modelo
            String nombreCliente = (valvula.getCliente() != null) ? valvula.getCliente().getNombre() : "Desconocido";
            String valorRecuperacion = (medicion.getRecuperacion() == Double.MAX_VALUE || medicion.getRecuperacion() == 0.0)
                    ? "0.00"
                    : String.format(java.util.Locale.US, "%.2f", medicion.getRecuperacion());
            String conexionEntrada = "Ø " + valvula.getEntradaBridaDiametro() + " " + (valvula.getEntradaBridaSerie() != null ? valvula.getEntradaBridaSerie() : "");
            String conexionSalida = "Ø " + valvula.getSalidaBridaDiametro() + " " + (valvula.getSalidaBridaSerie() != null ? valvula.getSalidaBridaSerie() : "");

            escribirCelda(4, 1, nombreCliente);       // B5
            escribirCelda(7, 2, valvula.getMarca());     // C8
            escribirCelda(8, 2, valvula.getMaterialCuerpo()); // C9
            escribirCelda(26, 2, String.format(java.util.Locale.US, "%.2f", medicion.getMaximo()));
            escribirCelda(27, 2, valorRecuperacion);
            escribirCelda(11, 2, valvula.getTag()); // C13
            escribirCelda(12, 2, valvula.getNumeroSerie()); // C12
            escribirCelda(8, 6, conexionEntrada); // G9
            escribirCelda(9, 6, conexionSalida); // G10
        }
    }

    private void escribirDatosCSVHeader() {
        String operador = medicion.getOperador() != null ? medicion.getOperador().getNombre() : "N/A";
        String fluido = medicion.getFluido() != null ? medicion.getFluido().getNombre() : "N/A";

        escribirCelda(48, 0, operador); // A50
        escribirCelda(25, 6, fluido);   // G26
    }

    private void escribirFechaActual() {
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        escribirCelda(48, 8, fechaActual); // I49
    }

    private void escribirPrimeraTemperatura() {
        if (medicion.getTemperaturaInicial() != null) {
            escribirCelda(26, 6, String.format(java.util.Locale.US, "%.2f", medicion.getTemperaturaInicial())); // F27
        } else {
            escribirCelda(26, 6, "0.00");
        }
    }

    private void escribirCelda(int rowIdx, int colIdx, String valor) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(valor != null ? valor : "");
    }

    private void agregarDatosHojaSecundaria(List<Double> xValues, List<Double> yValues) {
        XSSFSheet sheetDatos = workbook.getSheet("Datos_Grafico");
        if (sheetDatos == null) {
            sheetDatos = workbook.createSheet("Datos_Grafico");
        } else {
            int lastRow = sheetDatos.getLastRowNum();
            for (int i = 0; i <= lastRow; i++) {
                Row r = sheetDatos.getRow(i);
                if (r != null) sheetDatos.removeRow(r);
            }
        }

        Double presionObj = medicion.getPresionSolicitada() != null ? medicion.getPresionSolicitada() : 0.0;

        for (int i = 0; i < xValues.size(); i++) {
            XSSFRow row = sheetDatos.createRow(i);
            row.createCell(0).setCellValue(xValues.get(i));
            row.createCell(1).setCellValue(yValues.get(i));
            row.createCell(2).setCellValue(presionObj);
        }
    }

    private void crearGrafico(List<Double> xValues, List<Double> yValues) {

        if (xValues.isEmpty()) return;

        XSSFSheet sheetDatos = workbook.getSheet("Datos_Grafico");
        if (sheetDatos == null) {
            sheetDatos = sheet;
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 29, 10, 44);
        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText("Gráfico de Medición");
        chart.setTitleOverlay(false);

        // CONFIGURACIÓN DE EJES (Ambos numéricos)
        XDDFValueAxis xAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        xAxis.setTitle("Tiempo (s)");
        xAxis.setVisible(true);
        double maxX = xValues.get(xValues.size() - 1);
        xAxis.setMinimum(0.0);
        xAxis.setMaximum(maxX);
        double majorUnit = maxX / 10.0;
        if (majorUnit < 0.1) majorUnit = 0.1;
        xAxis.setMajorUnit(majorUnit);

        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        String unidad = medicion.getUnidadPresion() != null ? medicion.getUnidadPresion() : "Presión";
        yAxis.setTitle(unidad);
        yAxis.setVisible(true);

        if (!yValues.isEmpty()) {
            double minY = yValues.get(0);
            double maxY = yValues.get(0);

            for (Double y : yValues) {
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }

            Double presionObj = medicion.getPresionSolicitada();
            if (presionObj != null) {
                if (presionObj < minY) minY = presionObj;
                if (presionObj > maxY) maxY = presionObj;
            }

            yAxis.setMinimum(Math.max(0.0, minY * 0.95));
            yAxis.setMaximum(maxY * 1.05);
        } else {
            yAxis.setMinimum(0.0);
            yAxis.setMaximum(10.0);
        }

        int numDatos = xValues.size();

        XDDFDataSource<Double> xs = XDDFDataSourcesFactory.fromNumericCellRange(
                sheetDatos, new CellRangeAddress(0, numDatos - 1, 0, 0));

        XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(
                sheetDatos, new CellRangeAddress(0, numDatos - 1, 1, 1));

        // ---- EL CAMBIO CRUCIAL ESTÁ AQUÍ ----
        // Usamos ChartTypes.SCATTER en lugar de LINE para que soporte Eje X numérico
        XDDFScatterChartData data = (XDDFScatterChartData) chart.createData(ChartTypes.SCATTER, xAxis, yAxis);

        XDDFScatterChartData.Series seriePresion = (XDDFScatterChartData.Series) data.addSeries(xs, ys);
        seriePresion.setTitle("Presión", null);
        seriePresion.setSmooth(false);
        // Ocultamos los puntos para que parezca un gráfico de líneas continuo
        seriePresion.setMarkerStyle(MarkerStyle.NONE);

        XDDFLineProperties lineaRoja = new XDDFLineProperties();
        lineaRoja.setWidth(1.0);
        lineaRoja.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.RED)));
        seriePresion.setLineProperties(lineaRoja);

        if (medicion.getPresionSolicitada() != null) {
            XDDFDataSource<Double> xsReferencia = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheetDatos, new CellRangeAddress(0, numDatos - 1, 0, 0));

            XDDFNumericalDataSource<Double> ysReferencia = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheetDatos, new CellRangeAddress(0, numDatos - 1, 2, 2));

            XDDFScatterChartData.Series serieReferencia = (XDDFScatterChartData.Series) data.addSeries(xsReferencia, ysReferencia);
            serieReferencia.setTitle("Presión Apertura Solicitada", null);
            serieReferencia.setSmooth(false);
            serieReferencia.setMarkerStyle(MarkerStyle.NONE);

            XDDFLineProperties lineaAzul = new XDDFLineProperties();
            lineaAzul.setWidth(1.0);
            lineaAzul.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLUE)));
            serieReferencia.setLineProperties(lineaAzul);

            // --- INICIO CÓDIGO NUEVO PARA LA ETIQUETA ---
            // 1. Obtenemos el objeto base del gráfico de dispersión
            CTScatterSer ctScatterSer = serieReferencia.getCTScatterSer();

            // 2. Creamos el contenedor de etiquetas para la serie
            CTDLbls dLbls = ctScatterSer.addNewDLbls();

            // Apagamos las etiquetas generales para que no se imprima en todos los puntos
            dLbls.addNewShowVal().setVal(false);
            dLbls.addNewShowLegendKey().setVal(false);
            dLbls.addNewShowCatName().setVal(false);
            dLbls.addNewShowSerName().setVal(false);

            // 3. Agregamos la etiqueta específicamente al primer punto (índice 0)
            CTDLbl dLbl = dLbls.addNewDLbl();
            dLbl.addNewIdx().setVal(1);
            dLbl.addNewShowVal().setVal(true);       // Encendemos SOLO el valor Y (ej. 1)
            dLbl.addNewShowCatName().setVal(false);  // Apagamos el valor X / Categoría (el 0)
            dLbl.addNewShowSerName().setVal(false);  // Apagamos el nombre de la serie
            dLbl.addNewShowLegendKey().setVal(false);
            // 4. Posicionamos el texto
            dLbl.addNewDLblPos().setVal(STDLblPos.L);
        }

        formatearTituloXML(chart.getCTChart().getTitle());

        for (CTValAx valAx : chart.getCTChart().getPlotArea().getValAxArray()) {
            if (valAx.isSetTitle()) {
                formatearTituloXML(valAx.getTitle());
            }
        }

        chart.plot(data);
    }

    private void guardarArchivoExcel(String archivoExcel) throws IOException {
        try (FileOutputStream out = new FileOutputStream(archivoExcel)) {
            workbook.write(out);
        }
        workbook.close();
    }

    private void formatearTituloXML(CTTitle ctTitle) {
        if (ctTitle != null && ctTitle.isSetTx() && ctTitle.getTx().isSetRich()) {
            CTTextParagraph p = ctTitle.getTx().getRich().getPArray(0);
            CTTextCharacterProperties rPr;

            if (p.sizeOfRArray() > 0) {
                rPr = p.getRArray(0).isSetRPr() ? p.getRArray(0).getRPr() : p.getRArray(0).addNewRPr();
            } else {
                rPr = p.isSetPPr() && p.getPPr().isSetDefRPr() ? p.getPPr().getDefRPr() : p.addNewPPr().addNewDefRPr();
            }
            rPr.setSz(1300); // 1300 = Tamaño 13
            rPr.setB(false); // Quitar negrita
        }
    }
}