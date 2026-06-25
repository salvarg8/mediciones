package com.mediciones.reportes;

import com.mediciones.dao.ConfiguracionDAO;
import com.mediciones.model.Configuracion;
import com.mediciones.repository.PortalRepository;
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

import java.util.Date;
import com.mediciones.gestor.UbicacionGestor;
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
        if (archivoCSV == null || archivoExcel == null) {
            throw new IllegalArgumentException("Las rutas de los archivos no pueden ser nulas");
        }

        // 1. Usamos directamente la ruta absoluta que nos envió RealTimeGraph
        File archivoExcelFile = new File(archivoExcel);
        String rutaCarpeta = archivoExcelFile.getParent();

        String nombreArchivoCSV = new File(archivoCSV).getName();
        String rutaCompletaCSV = new File(rutaCarpeta, nombreArchivoCSV).getAbsolutePath();

        File csvFile = new File(rutaCompletaCSV);
        if (!csvFile.exists()) {
            throw new FileNotFoundException("No existe el CSV: " + rutaCompletaCSV);
        }

        // --- INICIO DEL CÓDIGO ACTUALIZADO: Carga segura de la plantilla desde Resources ---
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Plantillas/plantilla.xlsx")) {
            if (is == null) {
                logger.warn("ADVERTENCIA: No se encontró 'plantilla/plantilla.xlsx' en los recursos. Creando nuevo workbook.");
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Reporte");
            } else {
                workbook = new XSSFWorkbook(is);
                sheet = workbook.getSheetAt(0);
            }
        } catch (IOException e) {
            logger.error("Error al cargar la plantilla Excel desde los recursos.", e);
            // Fallback: si algo se rompe, creamos uno en blanco para que el programa no explote
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Reporte");
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
        guardarArchivoExcel(archivoExcel);

        // 3. ELIMINAR EL CSV TEMPORAL
        if (csvFile.exists()) {
            boolean eliminado = csvFile.delete();
            if (!eliminado) {
                System.err.println("Advertencia: No se pudo eliminar el archivo CSV temporal: " + csvFile.getAbsolutePath());
            }
        }

        // 4. ABRIR AUTOMÁTICAMENTE AL FINALIZAR
        try {
            if (archivoExcelFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(archivoExcelFile);
                logger.info("Reporte Excel abierto automáticamente.");
            } else {
                logger.warn("Apertura automática no soportada en este sistema.");
            }
        } catch (Exception ex) {
            logger.error("No se pudo abrir el Excel automáticamente", ex);
        }
    }
    private void escribirPresionAperturaSolicitada() {
        if (presionAperturaSolicitada != null) {
            escribirCelda(11, 6, presionAperturaSolicitada.toString());
        } else {
            escribirCelda(11, 6, "N/A"); // O déjalo en blanco
        }
    }

    private String construirRutaArchivo(String nombreArchivo) {
        UbicacionGestor ubicacionGestor = new UbicacionGestor();
        Ubicacion ubicacion = ubicacionGestor.obtenerUbicacion();
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
        Configuracion configuracion = configuracionDAO.obtenerConfiguracion();
        if (configuracion != null && "TXT".equals(configuracion.getOrigenDatos())) {
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

    private void agregarDatosCSV(List<Double> xValues, List<Double> yValues) {
        // Crea una nueva hoja para los datos
        XSSFSheet sheetDatos = workbook.getSheet("Datos_Grafico");
        if (sheetDatos == null) {
            sheetDatos = workbook.createSheet("Datos_Grafico");
        }

        for (int i = 0; i < xValues.size(); i++) {
            XSSFRow row = sheetDatos.createRow(i);
            row.createCell(0).setCellValue(xValues.get(i));
            row.createCell(1).setCellValue(yValues.get(i));
            row.createCell(2).setCellValue(presionAperturaSolicitada != null ? presionAperturaSolicitada : 0.0);
        }
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

        // 1. Buscamos la hoja secundaria donde están los datos limpios
        XSSFSheet sheetDatos = workbook.getSheet("Datos_Grafico");
        if (sheetDatos == null) {
            sheetDatos = sheet; // Fallback de seguridad en caso de que no se haya creado
        }

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 29, 10, 44);
        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText("Gráfico de Medición");
        chart.setTitleOverlay(false);

        // --- CONFIGURACIÓN DEL EJE X (TIEMPO) ---
        // IMPORTANTE: Cambiado a ValueAxis para poder aplicar Zoom numérico
        XDDFValueAxis xAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        xAxis.setTitle("Tiempo (s)");
        xAxis.setVisible(true); // Asegura que los números sean visibles
        xAxis.setMinimum(0.0);
        xAxis.setMaximum(xValues.get(xValues.size() - 1));

        // --- CONFIGURACIÓN DEL EJE Y (PRESIÓN) ---
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setTitle(unidadPresion);
        yAxis.setVisible(true); // Asegura que los números sean visibles

        // --- LÍMITES ESTRICTOS DEL EJE Y (PRESIÓN) ---
        if (!yValues.isEmpty()) {
            double minY = yValues.get(0);
            double maxY = yValues.get(0);

            // Buscamos el valor más alto y el más bajo de las mediciones reales
            for (Double y : yValues) {
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }

            // Si existe una presión solicitada, nos aseguramos de que no quede fuera del gráfico
            // (Opcional: puedes borrar este 'if' si no te importa que la línea azul no se vea
            // cuando la válvula falla por mucho y no alcanza la presión solicitada).
            if (presionAperturaSolicitada != null) {
                if (presionAperturaSolicitada < minY) minY = presionAperturaSolicitada;
                if (presionAperturaSolicitada > maxY) maxY = presionAperturaSolicitada;
            }

            // Asignamos los límites exactos al Eje Y
            yAxis.setMinimum(minY);
            yAxis.setMaximum(maxY);

        } else {
            // Fallback por si la lista viene vacía
            yAxis.setMinimum(0.0);
            yAxis.setMaximum(10.0);
        }

        // --- MAPEO DE DATOS A LA HOJA SECUNDARIA ---
        int numDatos = xValues.size();

        // Serie principal (Presión) apuntando a sheetDatos
        // Columna 0 = Tiempo, Columna 1 = Presión real
        XDDFDataSource<Double> xs =
                XDDFDataSourcesFactory.fromNumericCellRange(
                        sheetDatos,
                        new CellRangeAddress(0, numDatos - 1, 0, 0));

        XDDFNumericalDataSource<Double> ys =
                XDDFDataSourcesFactory.fromNumericCellRange(
                        sheetDatos,
                        new CellRangeAddress(0, numDatos - 1, 1, 1));

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, xAxis, yAxis);

        XDDFLineChartData.Series seriePresion = (XDDFLineChartData.Series) data.addSeries(xs, ys);
        seriePresion.setTitle("Presión", null);
        seriePresion.setSmooth(false);
        seriePresion.setMarkerStyle(MarkerStyle.NONE);

        XDDFLineProperties lineaRoja = new XDDFLineProperties();
        lineaRoja.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.RED)));
        seriePresion.setLineProperties(lineaRoja);

        // Línea horizontal de presión solicitada
        if (presionAperturaSolicitada != null) {

            // Usamos la misma columna de Tiempo
            XDDFDataSource<Double> xsReferencia =
                    XDDFDataSourcesFactory.fromNumericCellRange(
                            sheetDatos,
                            new CellRangeAddress(0, numDatos - 1, 0, 0));

            // Asumimos que la columna 2 (C) de sheetDatos tiene el valor constante guardado
            XDDFNumericalDataSource<Double> ysReferencia =
                    XDDFDataSourcesFactory.fromNumericCellRange(
                            sheetDatos,
                            new CellRangeAddress(0, numDatos - 1, 2, 2));

            XDDFLineChartData.Series serieReferencia = (XDDFLineChartData.Series) data.addSeries(xsReferencia, ysReferencia);

            serieReferencia.setTitle("Presión Apertura Solicitada", null);
            serieReferencia.setSmooth(false);
            serieReferencia.setMarkerStyle(MarkerStyle.NONE);

            XDDFLineProperties lineaAzul = new XDDFLineProperties();
            lineaAzul.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.BLUE)));
            serieReferencia.setLineProperties(lineaAzul);
        }

        chart.plot(data);
    }

    private void guardarArchivoExcel(String archivoExcel) throws IOException {
        try (FileOutputStream out = new FileOutputStream(archivoExcel)) { workbook.write(out); }
        workbook.close();
    }
}