package com.mediciones.reportes;

import com.mediciones.dao.ConfiguracionDAO;
import com.mediciones.repository.PortalRepository;
import com.mediciones.view.FrmOperadorCRUD;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import com.mediciones.dao.ValvulaDAO;
import com.mediciones.model.Valvula;
import com.mediciones.dao.ClienteDAO;
import com.mediciones.model.Cliente;
import java.util.Date;
import com.mediciones.controller.UbicacionController;
import com.mediciones.model.Ubicacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelGenerator {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    private Double primeraTemperatura = null;
    private String valorMaximo = "0.00";
    private String valorRecuperacion = "0.00";
    private String nombreOperadorCSV = "";
    private String nombreFluidoCSV = "";
    private Double presionAperturaSolicitada;

    private ConfiguracionDAO configuracionDAO;
    private ValvulaDAO valvulaDAO;
    private PortalRepository portalRepository;

    private static final Logger logger = LoggerFactory.getLogger(ExcelGenerator.class);


    public ExcelGenerator() {
        configuracionDAO = new ConfiguracionDAO();
        valvulaDAO = new ValvulaDAO();
        portalRepository = PortalRepository.getInstancia();
    }

    public void generarExcel(String archivoCSV, String archivoExcel) throws IOException {
        // 1. Usamos directamente la ruta absoluta que nos envió RealTimeGraph
        File archivoExcelFile = new File(archivoExcel);
        String rutaCarpeta = archivoExcelFile.getParent();

        String nombreArchivoCSV = new File(archivoCSV).getName();
        String rutaCompletaCSV = new File(rutaCarpeta, nombreArchivoCSV).getAbsolutePath();

        File csvFile = new File(rutaCompletaCSV);
        if (!csvFile.exists()) {
            throw new FileNotFoundException("No existe el CSV: " + rutaCompletaCSV);
        }

        File plantilla = new File("plantilla.xlsx");
        if (!plantilla.exists()) {
            System.out.println("ADVERTENCIA: No se encontró 'plantilla.xlsx'. Creando nuevo workbook.");
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Reporte");
        } else {
            try (FileInputStream fis = new FileInputStream(plantilla)) {
                workbook = new XSSFWorkbook(fis);
            }
            sheet = workbook.getSheetAt(0);
        }

        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        int valvulaId = leerIdValvulaDeCSV(rutaCompletaCSV);
        leerDatosCSV(rutaCompletaCSV, xValues, yValues);

        String unidadPresion = obtenerUnidadPresionDesdeCSV(rutaCompletaCSV);

        agregarDatosCSV(xValues, yValues);
        crearGrafico(xValues, yValues, unidadPresion);

        if (valvulaId != -1) {
            escribirDatosValvula(valvulaId);
        }

        // Escribir datos capturados del CSV
        escribirDatosCSVHeader();
        escribirFechaActual();
        escribirPrimeraTemperatura();
        escribirPresionAperturaSolicitada();

        // 2. GUARDAMOS DIRECTAMENTE EN EL ARCHIVO SOLICITADO
        // Ya no calculamos ningún nombre nuevo, simplemente usamos "archivoExcel"
        guardarArchivoExcel(archivoExcel);

        // 3. ELIMINAR EL CSV TEMPORAL
        // Una vez que el Excel se guardó correctamente, procedemos a borrar el CSV
        if (csvFile.exists()) {
            boolean eliminado = csvFile.delete();
            if (!eliminado) {
                System.err.println("Advertencia: No se pudo eliminar el archivo CSV temporal: " + csvFile.getAbsolutePath());
            }
        }
    }

    private void escribirPresionAperturaSolicitada() {
        escribirCelda(11, 6, presionAperturaSolicitada.toString()); // F12 - Valor máximo de presión

    }

    private String construirRutaArchivo(String nombreArchivo) {
        UbicacionController ubicacionController = new UbicacionController();
        Ubicacion ubicacion = ubicacionController.obtenerUbicacion();
        if (ubicacion == null || ubicacion.getUbicacion() == null || ubicacion.getUbicacion().isEmpty()) {
            return nombreArchivo;
        }
        File carpeta = new File(ubicacion.getUbicacion());
        return new File(carpeta, nombreArchivo).getAbsolutePath();
    }

    private int leerIdValvulaDeCSV(String archivoCSV) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivoCSV), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.contains("Valvula ID;")) {
                    String[] columnas = linea.split(";");
                    if (columnas.length >= 2) {
                        return Integer.parseInt(columnas[1].replaceAll("[^0-9]", "").trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error leyendo ID de válvula desde CSV: " + e);
        }
        return -1;
    }

    private String[] obtenerInfoParaNombreArchivo(int valvulaId) {
        ValvulaDAO valvulaDAO = new ValvulaDAO();
        Valvula valvula = valvulaDAO.obtenerPorId(valvulaId);
        if (valvula != null) {
            String nombreCliente = (valvula.getCliente() != null) ? valvula.getCliente().getNombre() : "Desconocido";
            return new String[]{nombreCliente, valvula.getTag()};
        }
        return null;
    }

    private void escribirDatosValvula(int valvulaId) {
        Valvula valvula;
        if (configuracionDAO.obtenerConfiguracion().getOrigenDatos().equals("TXT")) {
            valvula = portalRepository.getValvulaPorId(valvulaId);
        } else {
            valvula = valvulaDAO.obtenerPorId(valvulaId);
        }


        if (valvula != null) {
            String nombreCliente = (valvula.getCliente() != null) ? valvula.getCliente().getNombre() : "Desconocido";

            escribirCelda(4, 1, nombreCliente); // B5
            escribirCelda(7, 2, valvula.getMarca()); // C8
            escribirCelda(8, 2, valvula.getMaterialCuerpo()); // C9
            escribirCelda(9, 2, valvula.getLugarConexion()); // C10

            // ESCRIBIR MÁXIMO Y RECUPERACIÓN (C27 y C28)
            escribirCelda(26, 2, valorMaximo); // C27
            escribirCelda(27, 2, valorRecuperacion); // C28
            // El nombre del fluido se toma ahora preferentemente del CSV para ser fiel a la prueba,
            // pero si fallara el CSV, podrías usar: (valvula.getFluido() != null) ? valvula.getFluido().getNombre() : "N/A"
            // Por ahora, lo manejamos en escribirDatosCSVHeader().

            escribirCelda(11, 2, valvula.getNumeroSerie()); // C12
            escribirCelda(12, 2, valvula.getTag()); // C13

            String tipoConexion = "";
            if (valvula.getEntradaRoscaTipo() != null && !valvula.getEntradaRoscaTipo().isEmpty()) {
                tipoConexion = "Roscada";
            } else if (valvula.getEntradaBridaDiametro() != null && !valvula.getEntradaBridaDiametro().isEmpty()) {
                tipoConexion = "Bridada";
            }
            escribirCelda(7, 6, tipoConexion); // G8

            String conexionEntrada = "";
            if (tipoConexion.equals("Roscada")) {
                conexionEntrada = valvula.getEntradaRoscaTipo();
            } else if (tipoConexion.equals("Bridada")) {
                conexionEntrada = valvula.getEntradaBridaDiametro() + " " + (valvula.getEntradaBridaSerie() != null ? valvula.getEntradaBridaSerie() : "");
            }
            escribirCelda(8, 6, conexionEntrada); // G9

            String conexionSalida = "";
            if (valvula.getSalidaRoscaTipo() != null && !valvula.getSalidaRoscaTipo().isEmpty()) {
                conexionSalida = valvula.getSalidaRoscaTipo();
            } else if (valvula.getSalidaBridaDiametro() != null && !valvula.getSalidaBridaDiametro().isEmpty()) {
                conexionSalida = valvula.getSalidaBridaDiametro() + " " + (valvula.getSalidaBridaSerie() != null ? valvula.getSalidaBridaSerie() : "");
            }
            escribirCelda(9, 6, conexionSalida); // G10
        }
    }

    private void escribirDatosCSVHeader() {
        // Operador en fila 49, columna 1 (B es 1, pero usuario dice columna 1 -> A?)
        // En Excel, Columna 1 es A. Índice 0.
        // Fila 49 -> Índice 48.
        escribirCelda(48, 0, nombreOperadorCSV); // A50

        // Fluido en fila 26, columna 7 (G es 7).
        // Fila 26 -> Índice 25.
        // Columna 7 -> Índice 6.
        escribirCelda(25, 6, nombreFluidoCSV); // G26
    }

    private void escribirFechaActual() {
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        escribirCelda(48, 8, fechaActual); // I49
    }

    private void escribirPrimeraTemperatura() {
        if (primeraTemperatura != null) {
            escribirCelda(26, 6, String.format("%.2f", primeraTemperatura)); // F27
        }
    }

    private void escribirCelda(int rowIdx, int colIdx, String valor) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(valor != null ? valor : "");
    }

    private void leerDatosCSV(String archivoCSV, List<Double> x, List<Double> y) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivoCSV), StandardCharsets.UTF_8))) {
            String linea;
            boolean primeraTemperaturaLeida = false;

            // --- NUEVAS VARIABLES PARA EL FILTRADO ---
            boolean comenzarGrafico = false;
            double tiempoInicial = 0.0;
            // ----------------------------------------

            while ((linea = br.readLine()) != null) {
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);

                if (linea.contains("Operador;")) {
                    String[] p = linea.split(";");
                    if (p.length >= 2) nombreOperadorCSV = p[1].trim();
                    continue;
                }
                if (linea.contains("Fluido;")) {
                    String[] p = linea.split(";");
                    if (p.length >= 2) nombreFluidoCSV = p[1].trim();
                    continue;
                }
                if (linea.contains("Maximo;")) {
                    String[] p = linea.split(";");
                    if (p.length >= 2) valorMaximo = p[1].trim();
                    continue;
                }
                if (linea.contains("Recuperacion;")) {
                    String[] p = linea.split(";");
                    if (p.length >= 2) valorRecuperacion = p[1].trim();
                    continue;
                }
                if (linea.contains("Presión;")) {
                    String[] p = linea.split(";");
                    if (p.length >= 2) presionAperturaSolicitada = Double.parseDouble(p[1].trim());
                    continue;
                }

                if (!linea.contains(";")) continue;
                if (linea.contains("Valvula") || linea.contains("Operador") ||
                        linea.contains("Fluido") || linea.contains("Fecha") ||
                        linea.trim().isEmpty() || linea.contains("tiempo_s")) {
                    continue;
                }

                String[] p = linea.split(";");
                try {
                    double px = Double.parseDouble(p[0]);
                    double py = Double.parseDouble(p[1]);

                    // --- LÓGICA DE FILTRADO ---
                    if (!comenzarGrafico) {
                        // Si la presión es mayor o igual al 75% de la solicitada, comenzamos
                        if (presionAperturaSolicitada != null && py >= (0.75 * presionAperturaSolicitada)) {
                            comenzarGrafico = true;
                            tiempoInicial = px; // Guardamos el tiempo exacto en el que se cruzó el umbral
                        } else if (presionAperturaSolicitada == null) {
                            // Respaldo por si el CSV no trae el dato de presión solicitada
                            comenzarGrafico = true;
                            tiempoInicial = px;
                        }
                    }

                    // Solo agregamos los datos al gráfico si ya se superó el 75%
                    if (comenzarGrafico) {
                        x.add(px - tiempoInicial); // Restamos el tiempo inicial para que el gráfico empiece en 0 segundos
                        y.add(py);

                        if (!primeraTemperaturaLeida && p.length >= 3) {
                            primeraTemperatura = Double.parseDouble(p[2]);
                            primeraTemperaturaLeida = true;
                        }
                    }
                    // --------------------------

                } catch (Exception ignored) {}
            }
        }
    }

    private void agregarDatosCSV(List<Double> x, List<Double> y) {
        for (int i = 0; i < x.size(); i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) row = sheet.createRow(i + 1);
            Cell cx = row.createCell(10); cx.setCellValue(x.get(i)); // Columna K
            Cell cy = row.createCell(11); cy.setCellValue(y.get(i)); // Columna L

            if (presionAperturaSolicitada != null) {
                Cell cRef = row.createCell(12);
                cRef.setCellValue(presionAperturaSolicitada);
            }
        }

        sheet.setColumnHidden(10, true);
        sheet.setColumnHidden(11, true);
        sheet.setColumnHidden(12, true);
        sheet.setColumnHidden(13, true);

    }

    private String obtenerUnidadPresionDesdeCSV(String archivoCSV) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivoCSV), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.contains("tiempo_s;")) {
                    int startIndex = linea.indexOf('(');
                    int endIndex = linea.indexOf(')', startIndex);
                    if (startIndex != -1 && endIndex != -1) return linea.substring(startIndex + 1, endIndex);
                }
            }
        }
        return "Presion";
    }

    private void crearGrafico(List<Double> xValues, List<Double> yValues, String unidadPresion) {
        if (xValues.isEmpty()) return;

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 29, 10, 44);
        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText("Gráfico de Medición");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        xAxis.setTitle("Tiempo (s)");

        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setTitle(unidadPresion);

        // Calcular máximo y mínimo del eje Y
        double maxY = 0;
        for (Double y : yValues) {
            if (y > maxY) {
                maxY = y;
            }
        }

        if (maxY > 0) {
            double minY = 0.76 * maxY;

            if (presionAperturaSolicitada != null) {
                minY = Math.min(minY, presionAperturaSolicitada * 0.98);
            }

            yAxis.setMinimum(minY);
        }

        // Serie principal (Presión)
        XDDFDataSource<Double> xs =
                XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet,
                        new CellRangeAddress(1, xValues.size(), 10, 10)); // Columna K

        XDDFNumericalDataSource<Double> ys =
                XDDFDataSourcesFactory.fromNumericCellRange(
                        sheet,
                        new CellRangeAddress(1, xValues.size(), 11, 11)); // Columna L

        XDDFLineChartData data =
                (XDDFLineChartData) chart.createData(
                        ChartTypes.LINE,
                        xAxis,
                        yAxis);

        XDDFLineChartData.Series seriePresion =
                (XDDFLineChartData.Series) data.addSeries(xs, ys);

        seriePresion.setTitle("Presión", null);
        seriePresion.setSmooth(false);
        seriePresion.setMarkerStyle(MarkerStyle.NONE);

        XDDFLineProperties lineaRoja = new XDDFLineProperties();
        lineaRoja.setFillProperties(
                new XDDFSolidFillProperties(
                        XDDFColor.from(PresetColor.RED)));

        seriePresion.setLineProperties(lineaRoja);

        // CORRECCIÓN: Línea horizontal de presión solicitada sin duplicar filas
        if (presionAperturaSolicitada != null) {

            // Usamos el MISMO rango de Tiempo (Columna 10 / K)
            XDDFDataSource<Double> xsReferencia =
                    XDDFDataSourcesFactory.fromNumericCellRange(
                            sheet,
                            new CellRangeAddress(1, xValues.size(), 10, 10));

            // Usamos la nueva columna de Referencia (Columna 12 / M)
            XDDFNumericalDataSource<Double> ysReferencia =
                    XDDFDataSourcesFactory.fromNumericCellRange(
                            sheet,
                            new CellRangeAddress(1, xValues.size(), 12, 12));

            XDDFLineChartData.Series serieReferencia =
                    (XDDFLineChartData.Series) data.addSeries(
                            xsReferencia,
                            ysReferencia);

            serieReferencia.setTitle("Presión Apertura Solicitada", null);
            serieReferencia.setSmooth(false);
            serieReferencia.setMarkerStyle(MarkerStyle.NONE);

            XDDFLineProperties lineaAzul = new XDDFLineProperties();
            lineaAzul.setFillProperties(
                    new XDDFSolidFillProperties(
                            XDDFColor.from(PresetColor.BLUE)));

            serieReferencia.setLineProperties(lineaAzul);
        }

        chart.plot(data);
    }

    private void guardarArchivoExcel(String archivoExcel) throws IOException {
        try (FileOutputStream out = new FileOutputStream(archivoExcel)) { workbook.write(out); }
        workbook.close();
    }
}