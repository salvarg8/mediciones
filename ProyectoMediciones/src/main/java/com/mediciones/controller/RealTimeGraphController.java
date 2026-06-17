package com.mediciones.controller;

import com.fazecast.jSerialComm.SerialPort;
import com.mediciones.dao.RealTimeGraphDAO;
import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.model.Operador;
import com.mediciones.model.SensorCalibracion;
import com.mediciones.model.Ubicacion;
import com.mediciones.model.Valvula;
import com.mediciones.reportes.ExcelGenerator;
import com.mediciones.repository.ArchivoNoEncontradoException;
import com.mediciones.view.RealTimeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RealTimeGraphController {
    private final RealTimeGraph view;
    private final RealTimeGraphDAO dao;
    private final UbicacionController ubicacionController;

    private SerialPort comPort;
    private volatile boolean running = false;
    private Thread dataThread;

    private final List<String> csvDataPoints = Collections.synchronizedList(new ArrayList<>());
    private double maxValue = 0;
    private double recValue = Double.MAX_VALUE;
    private boolean maxReached = false;
    private String nombreUltimoArchivoCSV;

    private double factorA = 1.0;
    private double constanteC = 0.0;
    private double factorATemp = 1.0;
    private double constanteCTemp = 0.0;
    private String selectedSensorType = "Motorola";
    private double pressureRequested = 0.0;

    private static final Logger logger = LoggerFactory.getLogger(RealTimeGraphController.class);

    public RealTimeGraphController(RealTimeGraph view) {
        this.view = view;
        this.dao = new RealTimeGraphDAO();
        this.ubicacionController = new UbicacionController();
    }

    public void init() {
        loadCalibrationValues();
    }

    public void autoSelectAndOpenPort(JComboBox<String> portCombo) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length > 0) {
            for (int i = 0; i < ports.length; i++) {
                String name = ports[i].getDescriptivePortName().toLowerCase();
                if (name.contains("arduino") || name.contains("usb") || name.contains("ch340")) {
                    portCombo.setSelectedIndex(i);
                    openSelectedPort(ports[i]);
                    return;
                }
            }
            openSelectedPort(ports[ports.length - 1]);
        }
    }

    private void openSelectedPort(SerialPort port) {
        try {
            comPort = port;
            comPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
            if (!comPort.openPort()) throw new IOException("No se pudo abrir el puerto.");
            view.setLedColor(Color.GREEN);
        } catch (Exception ex) {
            logger.error("error al abrir el puerto. openSelectedPort() ", ex);
        }
    }

    public void closePort() {
        if (dataThread != null && dataThread.isAlive()) {
            dataThread.interrupt();
        }

        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            System.out.println("Puerto serial cerrado correctamente.");
        }
    }

    public void loadCalibrationValues() {
        try {
            SensorCalibracion ucPresion = new SensorCalibracionController().obtenerUltimaCalibracionPorSensor(selectedSensorType);
            if (ucPresion != null) {
                factorA = (ucPresion.getA1() != null && selectedSensorType.equals("Motorola")) ? ucPresion.getA1() :
                        (ucPresion.getA2() != null && selectedSensorType.equals("Endress-Hauser")) ? ucPresion.getA2() : 1.0;
                constanteC = (ucPresion.getC1() != null && selectedSensorType.equals("Motorola")) ? ucPresion.getC1() :
                        (ucPresion.getC2() != null && selectedSensorType.equals("Endress-Hauser")) ? ucPresion.getC2() : 0.0;
            } else {
                factorA = 1.0;
                constanteC = 0.0;
            }

            SensorCalibracion ucTemp = new SensorCalibracionController().obtenerUltimaCalibracionPorSensor("LM35");
            if (ucTemp != null) {
                factorATemp = ucTemp.getA3() != null ? ucTemp.getA3() : 1.0;
                constanteCTemp = ucTemp.getC3() != null ? ucTemp.getC3() : 0.0;
            } else {
                factorATemp = 1.0;
                constanteCTemp = 0.0;
                System.err.println("⚠️ Advertencia: No se encontró calibración para LM35. Usando a=1.0, c=0.0");
            }

            if (factorA == 0) factorA = 1.0;
            if (factorATemp == 0) factorATemp = 1.0;

        } catch (Exception e) {
            factorA = 1.0;
            constanteC = 0.0;
            factorATemp = 1.0;
            constanteCTemp = 0.0;
            logger.error("error al cargar valores de calibración. loadCalibrationValues()", e);
        }
    }

    public void updateSensorType(String sensorType) {
        this.selectedSensorType = sensorType;
        resetValues();
        loadCalibrationValues();
    }

    public void resetValues() {
        maxValue = 0;
        recValue = Double.MAX_VALUE;
        maxReached = false;
        csvDataPoints.clear();
        running = false;
        view.resetIndicators();
    }

    public void updatePressureRequestedValue(double pressure) {
        this.pressureRequested = pressure;
    }

    public void startDataCapture(JComboBox<String> portCombo, JComboBox<Integer> baudCombo,
                                 Cliente cliente, Valvula valvula, double currentPressureRequested) {

        if (dataThread != null && dataThread.isAlive()) {
            return;
        }

        if (currentPressureRequested <= 0) {
            view.showErrorMessage("Ingrese un valor válido para la Presión Solicitada antes de iniciar la captura.");
            return;
        }

        this.pressureRequested = currentPressureRequested;

        try {
            if (comPort == null || !comPort.isOpen()) {
                comPort = SerialPort.getCommPorts()[portCombo.getSelectedIndex()];
                comPort.setComPortParameters((Integer) baudCombo.getSelectedItem(), 8, 1, SerialPort.NO_PARITY);
                if (!comPort.openPort()) throw new IOException("No se pudo abrir el puerto.");
                view.setLedColor(Color.GREEN);
            }

            if (comPort.bytesAvailable() > 0) {
                comPort.readBytes(new byte[comPort.bytesAvailable()], comPort.bytesAvailable());
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String fileName = cliente.getNombre().replace(" ", "-") + "-"
                    + valvula.getTag().replace(" ", "-") + "-" + timestamp + ".csv";
            Ubicacion u = ubicacionController.obtenerUbicacion();
            nombreUltimoArchivoCSV = (u != null && u.getUbicacion() != null)
                    ? new File(new File(u.getUbicacion()), fileName).getAbsolutePath()
                    : fileName;

            resetValues();
            view.clearChart();

            running = false;
            view.setStartButtonText("Detener");
            view.setInfoFieldsEnabled(false);

            dataThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(comPort.getInputStream()))) {
                    while (comPort.isOpen() && !Thread.currentThread().isInterrupted()) {
                        if (comPort.bytesAvailable() > 0) {
                            String d = reader.readLine();
                            if (d != null) {
                                processNewData(d);
                            }
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    logger.error("Hilo de captura interrumpido. startDatacapture()", e);
                } catch (Exception ex) {
                    logger.error("Error en la lectura del puerto. startDatacapture()", ex);
                } finally {
                    stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
                }
            });
            dataThread.start();
        } catch (Exception ex) {
            logger.error("error al iniciar la captura. startDataCapture()", ex);
            view.showErrorMessage("Error " + ex.getMessage());
            stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
        }
    }

    private void processNewData(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length >= 3) {
                double tiempo = Double.parseDouble(parts[0].trim());
                double vMotorola = Double.parseDouble(parts[1].trim());
                double vEndress = Double.parseDouble(parts[2].trim());
                double tempRaw = (parts.length > 3) ? Double.parseDouble(parts[3].trim()) : 0.0;

                double currentV = selectedSensorType.equals("Motorola") ? vMotorola : vEndress;
                double finalP = Math.max(0, (currentV - constanteC) * factorA);
                double finalT = factorATemp * (tempRaw - constanteCTemp);

                SwingUtilities.invokeLater(() -> {
                    view.updateCurrentValue(finalP);

                    if (pressureRequested > 0 && !running && finalP >= 0.76 * pressureRequested) {
                        running = true;
                        view.setStartButtonText("Detener");

                        double lowerBound = 0.76 * pressureRequested;
                        double upperBound = Math.max(lowerBound + 1, maxValue + 1);
                        view.setChartBounds(lowerBound, upperBound);
                    }

                    if (running) {
                        if (finalP > maxValue) {
                            maxValue = finalP;
                            view.updateMaxValue(maxValue);
                            maxReached = true;
                            recValue = Double.MAX_VALUE;
                            view.updateRecValue("-");

                            double lowerBound = 0.76 * pressureRequested;
                            double upperBound = maxValue + (maxValue * 0.1);
                            view.setChartBounds(lowerBound, upperBound);
                        } else if (maxReached && finalP < recValue) {
                            recValue = finalP;
                            view.updateRecValue(String.format("%.2f", recValue));
                        }

                        view.updateTempValue(finalT);

                        view.addChartPoint(tiempo, finalP);
                        csvDataPoints.add(String.format("%.1f;%.2f;%.2f", tiempo, finalP, finalT).replace(",", "."));
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    public void stopDataCapture(Valvula valvula, Operador operador, Fluido fluido) {
        running = false;

        if (dataThread != null && dataThread.isAlive()) {
            dataThread.interrupt();
        }

        if (csvDataPoints.isEmpty()) {
            view.resetCaptureUI();
            return;
        }

        double minPressure = 0.76 * pressureRequested;
        int startIndex = -1;
        double startTime = 0;
        for (int i = 0; i < csvDataPoints.size(); i++) {
            try {
                String[] parts = csvDataPoints.get(i).split(";");
                if (parts.length >= 2) {
                    double pressure = Double.parseDouble(parts[1]);
                    if (pressure >= minPressure) {
                        startIndex = i;
                        startTime = Double.parseDouble(parts[0]);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (startIndex == -1) {
            startIndex = 0;
            try {
                String[] parts = csvDataPoints.get(0).split(";");
                if (parts.length >= 1) {
                    startTime = Double.parseDouble(parts[0]);
                }
            } catch (Exception ignored) {
            }
        }

        List<String> filteredDataPoints = new ArrayList<>();
        for (int i = startIndex; i < csvDataPoints.size(); i++) {
            try {
                String[] parts = csvDataPoints.get(i).split(";");
                if (parts.length >= 3) {
                    double time = Double.parseDouble(parts[0]);
                    double pressure = Double.parseDouble(parts[1]);
                    double temperature = Double.parseDouble(parts[2]);
                    double adjustedTime = time - startTime;
                    filteredDataPoints.add(String.format("%.1f;%.2f;%.2f", adjustedTime, pressure, temperature).replace(",", "."));
                }
            } catch (Exception ignored) {
            }
        }

        if (valvula != null && operador != null && fluido != null) {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(Paths.get(nombreUltimoArchivoCSV)), StandardCharsets.UTF_8)) {
                writer.write("\uFEFF");
                writer.write("Valvula ID;" + valvula.getId() + "\n");
                writer.write("Valvula TAG;" + valvula.getTag() + "\n");
                writer.write("Operador;" + operador.getNombre() + "\n");
                writer.write("Fluido;" + fluido.getNombre() + "\n");
                writer.write("Presión;" + pressureRequested + "\n");
                writer.write("Maximo;" + String.format("%.2f", maxValue).replace(",", ".") + "\n");
                writer.write("Recuperacion;" + ((recValue == Double.MAX_VALUE || !maxReached) ? "0.00" : String.format("%.2f", recValue).replace(",", ".")) + "\n");
                writer.write("\ntiempo_s;Presion (" + view.getSelectedPressureUnit() + ");temperatura\n");
                synchronized (filteredDataPoints) {
                    for (String p : filteredDataPoints) writer.write(p + "\n");
                }
            } catch (IOException ignored) {
            }
        }

        view.resetCaptureUI();
    }

    public void guardarExcel(Cliente cliente, Valvula valvula, Operador operador, Fluido fluido) {
        if (valvula == null || operador == null || fluido == null) {
            view.showErrorMessage("Debe seleccionar Válvula, Operador y Fluido.");
            return;
        }

        if (maxValue == 0) {
            view.showErrorMessage("Realice una medición primero.");
            return;
        }

        if (isRunning()) {
            stopDataCapture(valvula, operador, fluido);
        }

        try {
            Ubicacion ubicacion = ubicacionController.obtenerUbicacion();
            String direccion;
            if (ubicacion != null && ubicacion.getUbicacion() != null) {
                direccion = ubicacion.getUbicacion();
            } else {
                direccion = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
            }

            String fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            String clienteLimpio = cliente.getNombre().replaceAll("[\\\\/:*?\"<>|]", "-");
            String tagLimpio = valvula.getTag().replaceAll("[\\\\/:*?\"<>|]", "-");

            String nombreArchivo = clienteLimpio + "-" + tagLimpio + "-" + fecha + ".xlsx";

            File archivoDestino = new File(direccion, nombreArchivo);

            new ExcelGenerator().generarExcel(nombreUltimoArchivoCSV, archivoDestino.getAbsolutePath());

            view.showMessage("Reporte generado exitosamente en:\n" + archivoDestino.getAbsolutePath());

        } catch (IOException ex) {
            view.showErrorMessage("Error al generar el reporte Excel: " + ex.getMessage());
            logger.error("Error al generar el reporte Excel: " + ex.getMessage(), ex);
        }
    }

    public void recargarPortal() {
        try {
            dao.recargarPortal();
            view.cargarClientes(dao.obtenerTodosClientes());
            view.showMessage("Portal recargado correctamente.");
        } catch (Exception ex) {
            view.showErrorMessage("Error al recargar el Portal:\n" + ex.getMessage());
            logger.error("Error al recargar el portal: " + ex.getMessage(), ex);
        }
    }

    public void loadComboBoxData(JComboBox<Cliente> cmbCliente, JComboBox<Operador> cmbOperador, JComboBox<Fluido> cmbFluido) {
        try {
            dao.cargarComboBoxClientes(cmbCliente);
        } catch (Exception ex) {
            view.showErrorMessage("Error al cargar clientes: " + ex.getMessage());
        }
        cmbOperador.setModel(new DefaultComboBoxModel<>(dao.obtenerTodosOperadores().toArray(new Operador[0])));
        cmbFluido.setModel(new DefaultComboBoxModel<>(dao.obtenerTodosFluidos().toArray(new Fluido[0])));
    }

    public void updateValvulas(JComboBox<Valvula> cmbValvula, Cliente cliente) {
        try {
            dao.cargarComboBoxValvulas(cmbValvula, cliente != null ? cliente.getId() : null);
        } catch (Exception ex) {
            view.showErrorMessage("Error al cargar válvulas: " + ex.getMessage());
        }
    }

    public boolean isRunning() {
        return dataThread != null && dataThread.isAlive();
    }

    public void discardData() {
        if (isRunning()) {
            stopDataCapture(null, null, null); // Stop but don't save
        }
        resetValues();
        view.clearChart();
        view.showMessage("Datos descartados y gráfico limpiado.");
    }

    public void startSimulatedDataCapture(Cliente cliente, Valvula valvula, double currentPressureRequested) {
         logger.info("inicio simulacion");
        if (dataThread != null && dataThread.isAlive()) {
            return;
        }

        if (currentPressureRequested <= 0) {
            view.showErrorMessage("Ingrese un valor válido para la Presión Solicitada antes de iniciar la simulación.");
            return;
        }

        this.pressureRequested = currentPressureRequested;

        try {
            // Indicador visual para saber que estás en simulación
            view.setLedColor(Color.ORANGE);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            // Le agrego "-SIMULADO-" al nombre del archivo para no confundirlo con mediciones reales
            String fileName = cliente.getNombre().replace(" ", "-") + "-"
                    + valvula.getTag().replace(" ", "-") + "-SIMULADO-" + timestamp + ".csv";

            Ubicacion u = ubicacionController.obtenerUbicacion();
            nombreUltimoArchivoCSV = (u != null && u.getUbicacion() != null)
                    ? new File(new File(u.getUbicacion()), fileName).getAbsolutePath()
                    : fileName;

            resetValues();
            view.clearChart();

            running = false;
            view.setStartButtonText("Detener (Simulando)");
            view.setInfoFieldsEnabled(false);

            dataThread = new Thread(() -> {
                try {
                    double simulatedTime = 0.0;
                    double simulatedVolt = constanteC; // Empieza en voltaje base para que Presión sea 0

                    // Calculamos el voltaje objetivo para superar el 100% de la presión solicitada
                    double targetVolt = (pressureRequested / factorA) + constanteC + 0.2;

                    while (!Thread.currentThread().isInterrupted()) {
                        // Curva de subida de presión
                        if (simulatedVolt < targetVolt) {
                            simulatedVolt += 0.02 + (Math.random() * 0.01);
                        } else {
                            // Fluctuación leve una vez que alcanza el máximo
                            simulatedVolt += (Math.random() * 0.02) - 0.01;
                        }

                        // Temperatura simulada oscilando entre 24 y 26 grados
                        double tempRaw = (25.0 / factorATemp) + constanteCTemp + (Math.random() * 2.0 - 1.0);

                        // String idéntico al que enviaría el microcontrolador
                        String simulatedData = String.format(java.util.Locale.US, "%.1f,%.4f,%.4f,%.2f",
                                simulatedTime, simulatedVolt, simulatedVolt, tempRaw);

                        processNewData(simulatedData);

                        simulatedTime += 0.1; // Avanzamos el tiempo simulado
                        Thread.sleep(100);    // Pausa real del hilo (100ms)
                    }
                } catch (InterruptedException e) {
                    logger.info("Hilo de simulación detenido.");
                } catch (Exception ex) {
                    logger.error("Error durante la simulación.", ex);
                } finally {
                    stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
                }
            });
            dataThread.start();

        } catch (Exception ex) {
            logger.error("Error al iniciar la simulación", ex);
            view.showErrorMessage("Error al iniciar simulación: " + ex.getMessage());
            stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
        }
    }
}