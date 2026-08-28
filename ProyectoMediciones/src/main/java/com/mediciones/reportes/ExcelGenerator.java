package com.mediciones.reportes;

import com.mediciones.gestor.ConfiguracionGestor;
import com.mediciones.model.Configuracion;
import com.mediciones.model.Medicion;
import com.mediciones.model.Valvula;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
        this.medicion = medicion;

        ConfiguracionGestor configGestor = new ConfiguracionGestor();
        Configuracion config = null;
        try {
            config = configGestor.obtenerConfiguracion();
        } catch (Exception e) {
            logger.error("Error al obtener la configuración de la base de datos.", e);
        }
        String rutaPlantilla = (config != null) ? config.getRutaPlantillaExcel() : null;
        InputStream is = null;
        try {
            if (rutaPlantilla != null && !rutaPlantilla.trim().isEmpty()) {
                File archivoExterno = new File(rutaPlantilla);
                if (archivoExterno.exists() && archivoExterno.isFile()) {
                    is = new FileInputStream(archivoExterno);
                } else {
                    logger.warn("La plantilla personalizada no existe. Se usará la interna. Ruta intentada: " + rutaPlantilla);
                }
            }
            if (is == null) {
                is = getClass().getClassLoader().getResourceAsStream("Plantillas/plantilla.xlsx");
                if (is != null) logger.info("Usando plantilla Excel interna por defecto.");
            }
            if (is == null) {
                logger.warn("ADVERTENCIA: No se encontró ninguna plantilla. Creando nuevo workbook vacío.");
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Reporte");
            } else {
                workbook = new XSSFWorkbook(is);
                sheet = workbook.getSheetAt(0);
            }
        } catch (IOException e) {
            logger.error("Error crítico al cargar la plantilla Excel.", e);
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Reporte");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
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
        escribirFechaActual();
        escribirPrimeraTemperatura();
        escribirPresionAperturaSolicitada();
        guardarArchivoExcel(archivoExcel);
    }

    private void escribirPresionAperturaSolicitada() {
        if (medicion.getPresionSolicitada() != null) {
            String presionFormateada = String.format(java.util.Locale.US, "%.2f", medicion.getPresionSolicitada());
            escribirCelda(11, 6, presionFormateada + " " + medicion.getUnidadPresion());
        } else {
            escribirCelda(11, 6, "N/A");
        }
    }

    private void escribirDatosValvula() {
        Valvula valvula = medicion.getValvula();
        if (valvula != null) {
            String nombreCliente = (valvula.getCliente() != null) ? valvula.getCliente().getNombre() : "Desconocido";
            String valorRecuperacion = (medicion.getRecuperacion() == Double.MAX_VALUE || medicion.getRecuperacion() == 0.0)
                    ? "0.00"
                    : String.format(java.util.Locale.US, "%.2f", medicion.getRecuperacion());
            String conexionEntrada = "Ø " + valvula.getEntradaBridaDiametro() + " " + (valvula.getEntradaBridaSerie() != null ? valvula.getEntradaBridaSerie() : "");
            String conexionSalida = "Ø " + valvula.getSalidaBridaDiametro() + " " + (valvula.getSalidaBridaSerie() != null ? valvula.getSalidaBridaSerie() : "");

            escribirCelda(4, 1, nombreCliente);
            escribirCelda(7, 2, valvula.getMarca());
            escribirCelda(8, 2, valvula.getMaterialCuerpo());
            escribirCelda(26, 2, String.format(java.util.Locale.US, "%.2f", medicion.getMaximo()) + " " + medicion.getUnidadPresion());
            escribirCelda(27, 2, valorRecuperacion + " " + medicion.getUnidadPresion());
            escribirCelda(11, 2, valvula.getTag());
            escribirCelda(12, 2, valvula.getNumeroSerie());
            escribirCelda(8, 6, conexionEntrada);
            escribirCelda(9, 6, conexionSalida);
            escribirCelda(48,0, medicion.getOperador().getNombre());
        }
    }

    private void escribirFechaActual() {
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        escribirCelda(48, 8, fechaActual); // I49
    }

    private void escribirPrimeraTemperatura() {
        if (medicion.getTemperaturaInicial() != null) {
            escribirCelda(26, 6, String.format(java.util.Locale.US, "%.0f", medicion.getTemperaturaInicial()) + "° C"); // F27
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
        XSSFSheet sheetDatos = workbook.getSheet("Datos_Grafico") != null ? workbook.getSheet("Datos_Grafico") : sheet;

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFChart chart = inicializarGraficoYAnclaje(drawing);

        XDDFValueAxis xAxis = configurarEjeX(chart, xValues);
        XDDFValueAxis yAxis = configurarEjeY(chart, yValues);

        XDDFScatterChartData data = (XDDFScatterChartData) chart.createData(ChartTypes.SCATTER, xAxis, yAxis);
        agregarSeriesDeDatos(data, sheetDatos, xValues.size());

        formatearTextosYCuadricula(chart);
        configurarColoresDeFondo(chart);
        maximizarAreaDeTrazado(chart);

        chart.plot(data);
    }

    private XSSFChart inicializarGraficoYAnclaje(XSSFDrawing drawing) {
        int marginPx = 3;
        int dx = Units.toEMU(marginPx);
        int dy = Units.toEMU(-marginPx);

        XSSFClientAnchor anchor = drawing.createAnchor(dx, dx, dy, dy, 0, 29, 10, 44);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Gráfico de Medición");
        chart.setTitleOverlay(false);
        return chart;
    }

    private XDDFValueAxis configurarEjeX(XSSFChart chart, List<Double> xValues) {
        XDDFValueAxis xAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        xAxis.setTitle("Tiempo (s)");
        xAxis.setVisible(true);
        xAxis.setNumberFormat("0.00");
        xAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.MIN);
        double maxX = xValues.get(xValues.size() - 1);
        xAxis.setMinimum(0.0);
        xAxis.setMaximum(maxX * 1.1);

        double majorUnit = Math.max(maxX / 10.0, 0.1);
        xAxis.setMajorUnit(majorUnit);
        return xAxis;
    }

    private XDDFValueAxis configurarEjeY(XSSFChart chart, List<Double> yValues) {
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        String unidad = medicion.getUnidadPresion() != null ? medicion.getUnidadPresion() : "Presión";
        yAxis.setTitle(unidad);
        yAxis.setVisible(true);
        yAxis.setNumberFormat("0.00");
        yAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.MIN);
        if (!yValues.isEmpty()) {
            double minY = yValues.stream().min(Double::compare).orElse(0.0);
            double maxY = yValues.stream().max(Double::compare).orElse(0.0);

            try {
                if (medicion.getPresionSolicitada() != null) {
                    double setPressure = medicion.getPresionSolicitada();
                    minY = Math.min(minY, setPressure);
                    maxY = Math.max(maxY, setPressure);
                }
            } catch(Exception ignored) {}

            yAxis.setMinimum(minY);
            yAxis.setMaximum(maxY * 1.15);
        }
        return yAxis;
    }

    private void agregarSeriesDeDatos(XDDFScatterChartData data, XSSFSheet sheetDatos, int numDatos) {
        XDDFDataSource<Double> xs = XDDFDataSourcesFactory.fromNumericCellRange(sheetDatos, new CellRangeAddress(0, numDatos - 1, 0, 0));
        XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(sheetDatos, new CellRangeAddress(0, numDatos - 1, 1, 1));

        XDDFScatterChartData.Series seriePresion = (XDDFScatterChartData.Series) data.addSeries(xs, ys);
        seriePresion.setTitle("Presión", null);
        seriePresion.setSmooth(false);
        seriePresion.setMarkerStyle(MarkerStyle.NONE);

        XDDFLineProperties lineaRoja = new XDDFLineProperties();
        lineaRoja.setWidth(1.0);
        lineaRoja.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.RED)));
        seriePresion.setLineProperties(lineaRoja);

        if (medicion.getPresionSolicitada() != null) {
            XDDFNumericalDataSource<Double> ysReferencia = XDDFDataSourcesFactory.fromNumericCellRange(sheetDatos, new CellRangeAddress(0, numDatos - 1, 2, 2));
            XDDFScatterChartData.Series serieReferencia = (XDDFScatterChartData.Series) data.addSeries(xs, ysReferencia);
            serieReferencia.setTitle("Presión Apertura Solicitada", null);
            serieReferencia.setSmooth(false);
            serieReferencia.setMarkerStyle(MarkerStyle.NONE);

            XDDFLineProperties lineaAzul = new XDDFLineProperties();
            lineaAzul.setWidth(1.0);
            lineaAzul.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLUE)));
            serieReferencia.setLineProperties(lineaAzul);

            inyectarEtiquetaXML(serieReferencia, numDatos);
        }
    }

    private void inyectarEtiquetaXML(XDDFScatterChartData.Series serieReferencia, int numDatos) {
        CTScatterSer ctScatterSer = serieReferencia.getCTScatterSer();
        CTDLbls dLbls = ctScatterSer.addNewDLbls();
        dLbls.addNewShowVal().setVal(false);
        dLbls.addNewShowLegendKey().setVal(false);
        dLbls.addNewShowCatName().setVal(false);
        dLbls.addNewShowSerName().setVal(false);

        CTDLbl dLbl = dLbls.addNewDLbl();
        dLbl.addNewIdx().setVal(numDatos-1);
        dLbl.addNewShowVal().setVal(false);
        dLbl.addNewShowCatName().setVal(false);
        dLbl.addNewShowSerName().setVal(false);
        dLbl.addNewShowLegendKey().setVal(false);
        dLbl.addNewDLblPos().setVal(STDLblPos.R);

        Double valorSet = medicion.getPresionSolicitada() != null ? medicion.getPresionSolicitada() : 0.0;
        String valorFormateado = String.format(java.util.Locale.US, "%.2f", valorSet).replace(".", ",");
        String unidadEtiqueta = medicion.getUnidadPresion() != null ? medicion.getUnidadPresion().trim() : "";

        CTTx tx = dLbl.addNewTx();
        CTTextBody rich = tx.addNewRich();
        rich.addNewBodyPr();
        rich.addNewLstStyle();
        CTTextParagraph p = rich.addNewP();
        CTRegularTextRun r = p.addNewR();
        r.setT(valorFormateado + " " + unidadEtiqueta);

        CTTextCharacterProperties rPr = r.addNewRPr();
        rPr.setB(true);
        CTSolidColorFillProperties fill = rPr.addNewSolidFill();
        fill.addNewSrgbClr().setVal(new byte[]{0, 0, 0});
    }

    private void formatearTextosYCuadricula(XSSFChart chart) {
        formatearTituloXML(chart.getCTChart().getTitle(), 1200);

        for (CTValAx valAx : chart.getCTChart().getPlotArea().getValAxArray()) {
            if (valAx.isSetTitle()) formatearTituloXML(valAx.getTitle(), 900);

            CTNumFmt numFmt = valAx.isSetNumFmt() ? valAx.getNumFmt() : valAx.addNewNumFmt();
            numFmt.setFormatCode("0.00");
            numFmt.setSourceLinked(false);

            org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody txPr = valAx.isSetTxPr() ? valAx.getTxPr() : valAx.addNewTxPr();
            if (txPr.getBodyPr() == null) txPr.addNewBodyPr();
            if (!txPr.isSetLstStyle()) txPr.addNewLstStyle();

            org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph p = txPr.sizeOfPArray() > 0 ? txPr.getPArray(0) : txPr.addNewP();
            org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties defRPr = (p.isSetPPr() && p.getPPr().isSetDefRPr())
                    ? p.getPPr().getDefRPr() : (p.isSetPPr() ? p.getPPr().addNewDefRPr() : p.addNewPPr().addNewDefRPr());

            defRPr.setSz(800);
            org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties textFill = defRPr.isSetSolidFill() ? defRPr.getSolidFill() : defRPr.addNewSolidFill();
            textFill.addNewSrgbClr().setVal(new byte[]{0, 0, 0});

            if (!valAx.isSetMajorGridlines()) {
                CTChartLines gridlines = valAx.addNewMajorGridlines();
                CTShapeProperties spPr = gridlines.addNewSpPr();
                CTLineProperties ln = spPr.addNewLn();
                ln.setW(9525);
                CTSolidColorFillProperties fill = ln.addNewSolidFill();
                fill.addNewSrgbClr().setVal(new byte[]{(byte) 211, (byte) 211, (byte) 211});
            }
        }
    }

    private void configurarColoresDeFondo(XSSFChart chart) {
        // Fondo degradado del gráfico principal
        org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties chartSpPr = chart.getCTChartSpace().isSetSpPr() ? chart.getCTChartSpace().getSpPr() : chart.getCTChartSpace().addNewSpPr();
        if (chartSpPr.isSetSolidFill()) chartSpPr.unsetSolidFill();

        org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties gradFill = chartSpPr.isSetGradFill() ? chartSpPr.getGradFill() : chartSpPr.addNewGradFill();
        org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStopList gsLst = gradFill.isSetGsLst() ? gradFill.getGsLst() : gradFill.addNewGsLst();
        if (gsLst.sizeOfGsArray() > 0) gsLst.setGsArray(new org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop[0]);

        org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop gs1 = gsLst.addNewGs();
        gs1.setPos(0);
        gs1.addNewSrgbClr().setVal(new byte[]{(byte) 141, (byte) 179, (byte) 226});

        org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop gs2 = gsLst.addNewGs();
        gs2.setPos(100000);
        gs2.addNewSrgbClr().setVal(new byte[]{(byte) 219, (byte) 229, (byte) 241});

        org.openxmlformats.schemas.drawingml.x2006.main.CTLinearShadeProperties lin = gradFill.isSetLin() ? gradFill.getLin() : gradFill.addNewLin();
        lin.setAng(5400000);

        // Fondo del área de trazado interna
        org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties plotSpPr = plotArea.isSetSpPr() ? plotArea.getSpPr() : plotArea.addNewSpPr();
        org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties plotFill = plotSpPr.isSetSolidFill() ? plotSpPr.getSolidFill() : plotSpPr.addNewSolidFill();

        org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor plotClr = plotFill.isSetSrgbClr() ? plotFill.getSrgbClr() : plotFill.addNewSrgbClr();
        plotClr.setVal(new byte[]{(byte) 219, (byte) 229, (byte) 241});
    }

    private void maximizarAreaDeTrazado(XSSFChart chart) {
        org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout ctLayout = plotArea.isSetLayout() ? plotArea.getLayout() : plotArea.addNewLayout();
        org.openxmlformats.schemas.drawingml.x2006.chart.CTManualLayout manualLayout = ctLayout.isSetManualLayout() ? ctLayout.getManualLayout() : ctLayout.addNewManualLayout();

        manualLayout.addNewXMode().setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STLayoutMode.EDGE);
        manualLayout.addNewYMode().setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STLayoutMode.EDGE);
        manualLayout.addNewWMode().setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STLayoutMode.EDGE);
        manualLayout.addNewHMode().setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STLayoutMode.EDGE);

        // Margen izquierdo para las etiquetas del eje Y
        manualLayout.addNewX().setVal(0.025);
        // Margen superior pequeño (título, si lo hay)
        manualLayout.addNewY().setVal(0.1);
        // Ancho: casi todo el espacio restante hacia la derecha
        manualLayout.addNewW().setVal(0.99);
        // Alto: deja espacio abajo para las etiquetas del eje X
        manualLayout.addNewH().setVal(0.96);
    }

    private void guardarArchivoExcel(String archivoExcel) throws IOException {
        try (FileOutputStream out = new FileOutputStream(archivoExcel)) {
            workbook.write(out);
        }
        workbook.close();
    }

    private void formatearTituloXML(CTTitle ctTitle, int tamanioCentecimas) {
        if (ctTitle != null && ctTitle.isSetTx() && ctTitle.getTx().isSetRich()) {
            CTTextParagraph p = ctTitle.getTx().getRich().getPArray(0);
            CTTextCharacterProperties rPr;

            if (p.sizeOfRArray() > 0) {
                rPr = p.getRArray(0).isSetRPr() ? p.getRArray(0).getRPr() : p.getRArray(0).addNewRPr();
            } else {
                rPr = p.isSetPPr() && p.getPPr().isSetDefRPr() ? p.getPPr().getDefRPr() : p.addNewPPr().addNewDefRPr();
            }
            rPr.setSz(tamanioCentecimas); // Aplicamos el tamaño recibido
            rPr.setB(true);               // Mantenemos la negrita

            // Forzar color negro (RGB: 0, 0, 0)
            CTSolidColorFillProperties fill = rPr.isSetSolidFill() ? rPr.getSolidFill() : rPr.addNewSolidFill();
            CTSRgbColor clr = fill.isSetSrgbClr() ? fill.getSrgbClr() : fill.addNewSrgbClr();
            clr.setVal(new byte[]{0, 0, 0});
        }
    }
}