package com.mediciones.gestor;

import com.fazecast.jSerialComm.SerialPort;
import com.mediciones.dao.ConfiguracionDAO;
import com.mediciones.dao.RealTimeGraphDAO;
import com.mediciones.model.*;
import com.mediciones.reportes.ExcelGenerator;
import com.mediciones.view.RealTimeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RealTimeGraphGestor {
    private final RealTimeGraph view;
    private final RealTimeGraphDAO dao;
    private final UbicacionGestor ubicacionGestor;
    private final ValvulaGestor valvulaGestor;
    private final TipoValvulaGestor tipoValvulaGestor;
    private ConfiguracionDAO configDAO;

    private SerialPort comPort;
    private volatile boolean running = false;
    private Thread dataThread;

    private double maxValue = 0;
    private double recValue = Double.MAX_VALUE;
    private boolean maxReached = false;

    private double factorA = 1.0;
    private double constanteC = 0.0;
    private double factorATemp = 1.0;
    private double constanteCTemp = 0.0;
    private String selectedSensorType = "Motorola";
    private double pressureRequested = 0.0;

    private Medicion medicionActual;

    private static final Logger logger = LoggerFactory.getLogger(RealTimeGraphGestor.class);

    public RealTimeGraphGestor(RealTimeGraph view) {
        this.view = view;
        this.dao = new RealTimeGraphDAO();
        this.ubicacionGestor = new UbicacionGestor();
        this.valvulaGestor = new ValvulaGestor();
        this.tipoValvulaGestor = new TipoValvulaGestor();
        this.configDAO = new ConfiguracionDAO();
    }

    public void init() {
        loadCalibrationValues();
    }

    public void autoSelectAndOpenPort(JComboBox<String> portCombo) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) return;

        Configuracion config = configDAO.obtenerConfiguracion();
        String puertoFavorito = (config != null) ? config.getPuertoComDefault() : null;
        int selectedIndex = -1;

        if (puertoFavorito != null && !puertoFavorito.trim().isEmpty()) {
            for (int i = 0; i < ports.length; i++) {
                if (ports[i].getSystemPortName().equalsIgnoreCase(puertoFavorito)) {
                    selectedIndex = i;
                    logger.info("Puerto COM persistente encontrado: " + puertoFavorito);
                    break;
                }
            }
        }

        if (selectedIndex == -1) {
            for (int i = 0; i < ports.length; i++) {
                String name = ports[i].getDescriptivePortName().toLowerCase();
                if (name.contains("arduino") || name.contains("usb") || name.contains("ch340")) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex == -1) {
            selectedIndex = ports.length - 1;
        }

        portCombo.setSelectedIndex(selectedIndex);
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
            SensorCalibracion ucPresion = new SensorCalibracionGestor().obtenerUltimaCalibracionPorSensor(selectedSensorType);
            if (ucPresion != null) {
                factorA = (ucPresion.getA1() != null && selectedSensorType.equals("Motorola")) ? ucPresion.getA1() :
                        (ucPresion.getA2() != null && selectedSensorType.equals("Endress-Hauser")) ? ucPresion.getA2() : 1.0;
                constanteC = (ucPresion.getC1() != null && selectedSensorType.equals("Motorola")) ? ucPresion.getC1() :
                        (ucPresion.getC2() != null && selectedSensorType.equals("Endress-Hauser")) ? ucPresion.getC2() : 0.0;
            } else {
                factorA = 1.0;
                constanteC = 0.0;
            }

            SensorCalibracion ucTemp = new SensorCalibracionGestor().obtenerUltimaCalibracionPorSensor("LM35");
            if (ucTemp != null) {
                factorATemp = ucTemp.getA3() != null ? ucTemp.getA3() : 1.0;
                constanteCTemp = ucTemp.getC3() != null ? ucTemp.getC3() : 0.0;
            } else {
                factorATemp = 1.0;
                constanteCTemp = 0.0;
                System.err.println("Advertencia: No se encontró calibración para LM35. Usando a=1.0, c=0.0");
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
        running = false;
        view.resetIndicators();
    }

    public void updatePressureRequestedValue(double pressure) {
        this.pressureRequested = pressure;
    }

    public void startDataCapture(JComboBox<String> portCombo, JComboBox<Integer> baudCombo,
                                 Cliente cliente, Valvula valvula, double currentPressureRequested) {

        medicionActual = new Medicion(
                valvula,
                cliente,
                view.getSelectedOperador(),
                view.getSelectedFluido(),
                currentPressureRequested,
                view.getUnidadSeleccionada()
        );

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

            resetValues();
            view.clearChart();

            running = false;
            view.setStartButtonText("Detener");
            view.setInfoFieldsEnabled(false);

            dataThread = new Thread(() -> {
                boolean porDesconexion = false;
                String mensajeDesconexion = "Error desconocido en los sensores.";

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(comPort.getInputStream(), StandardCharsets.UTF_8))) {
                    long ultimoDatoTime = System.currentTimeMillis();

                    while (comPort.isOpen() && !Thread.currentThread().isInterrupted()) {
                        int bytesDisponibles = comPort.bytesAvailable();

                        if (bytesDisponibles < 0) {
                            throw new IOException("Se desconectó físicamente el cable USB del banco de pruebas.");
                        }

                        if (bytesDisponibles > 0) {
                            String d = reader.readLine();
                            if (d != null) {
                                processNewData(d);
                                ultimoDatoTime = System.currentTimeMillis();
                            }
                        } else {
                            if (System.currentTimeMillis() - ultimoDatoTime > 20000) {
                                throw new IOException("Se perdió la señal del sensor. El dispositivo no responde (Timeout de 20s).");
                            }
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    logger.info("Hilo de captura interrumpido normalmente al presionar Detener.");
                } catch (Exception ex) {
                    logger.error("Error crítico detectado en la lectura del puerto serial", ex);
                    porDesconexion = true;
                    mensajeDesconexion = "Error crítico detectado en la lectura del puerto serial";
                } finally {
                    if (porDesconexion) {
                        final String msgPopUp = mensajeDesconexion;
                        SwingUtilities.invokeLater(() -> {
                            view.setLedColor(Color.RED);
                            view.showErrorMessage("Alerta de Hardware:\n" + msgPopUp);
                        });
                    }
                    stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
                }
            });
            dataThread.start();

            Configuracion config = configDAO.obtenerConfiguracion();
            if (config == null) config = new Configuracion();
            config.setPuertoComDefault(comPort.getSystemPortName());
            configDAO.guardarConfiguracion(config);

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

                        // Llenado EXCLUSIVO a través de la entidad Medicion
                        if (medicionActual != null) {
                            medicionActual.agregarPunto(tiempo, finalP);
                        }
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
        view.resetCaptureUI();
    }

    public void guardarExcel(Cliente cliente, Valvula valvula, Operador operador, Fluido fluido) {
        if (valvula == null || operador == null || fluido == null) {
            view.showErrorMessage("Debe seleccionar Válvula, Operador y Fluido.");
            return;
        }
        if (this.medicionActual == null || this.medicionActual.getValoresMedicion().isEmpty()) {
            view.showErrorMessage("No hay ninguna medición activa o finalizada con datos para exportar.");
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
            Ubicacion ubicacion = ubicacionGestor.obtenerUbicacion();
            String direccion;
            if (ubicacion != null && ubicacion.getUbicacion() != null) {
                direccion = ubicacion.getUbicacion();
            } else {
                direccion = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
            }

            String fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            String clienteLimpio = cliente.getNombre().replaceAll("[\\\\/:*?\"<>|]", "-");
            String tagLimpio = valvula.getTag().replaceAll("[\\\\/:*?\"<>|]", "-");

            String nombreArchivo = clienteLimpio + "-" + tagLimpio + "-" + fecha + ".xlsx";

            File archivoDestinoBase = new File(direccion, nombreArchivo);
            String rutaUnica = obtenerRutaUnica(archivoDestinoBase.getAbsolutePath());
            File archivoDestinoFinal = new File(rutaUnica);

            new ExcelGenerator().generarExcel(this.medicionActual, archivoDestinoFinal.getAbsolutePath());

            view.showMessage("Reporte generado exitosamente en:\n" + archivoDestinoFinal.getAbsolutePath());

            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(archivoDestinoFinal);
                }
            } catch (Exception e) {
                logger.error("No se pudo abrir el archivo automáticamente: " + e.getMessage());
            }

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

    private String obtenerRutaUnica(String rutaCompleta) {
        java.io.File archivo = new java.io.File(rutaCompleta);

        if (!archivo.exists()) {
            return rutaCompleta;
        }

        String carpeta = archivo.getParent();
        String nombreOriginal = archivo.getName();
        String nombreSinExtension = nombreOriginal;
        String extension = "";

        int dotIndex = nombreOriginal.lastIndexOf('.');
        if (dotIndex > 0) {
            nombreSinExtension = nombreOriginal.substring(0, dotIndex);
            extension = nombreOriginal.substring(dotIndex);
        }

        int contador = 1;
        java.io.File nuevoArchivo;

        do {
            String nuevoNombre = nombreSinExtension + " (" + contador + ")" + extension;
            nuevoArchivo = new java.io.File(carpeta, nuevoNombre);
            contador++;
        } while (nuevoArchivo.exists());

        return nuevoArchivo.getAbsolutePath();
    }

    public void loadComboBoxData(JComboBox<Cliente> cmbCliente, JComboBox<Operador> cmbOperador, JComboBox<Fluido> cmbFluido, JComboBox<TipoValvula> cmbTipoValvula) {
        try {
            dao.cargarComboBoxClientes(cmbCliente);
        } catch (Exception ex) {
            logger.error("Error al cargar el ComboBox de Clientes:\n" + ex.getMessage(), ex);
        }
        cmbOperador.setModel(new DefaultComboBoxModel<>(dao.obtenerTodosOperadores().toArray(new Operador[0])));
        cmbFluido.setModel(new DefaultComboBoxModel<>(dao.obtenerTodosFluidos().toArray(new Fluido[0])));
        if (cmbTipoValvula != null) {
            cmbTipoValvula.removeAllItems();
        }
    }

    public void updateTiposValvulaPorPlanta(JComboBox<TipoValvula> cmbTipo, Planta planta) {
        if (cmbTipo == null) return;
        cmbTipo.removeAllItems();

        if (planta != null && planta.getValvulas() != null) {
            cmbTipo.addItem(new TipoValvula(0, "Todos los tipos"));

            java.util.Map<Integer, TipoValvula> tiposUnicos = new java.util.HashMap<>();

            for (Valvula v : planta.getValvulas()) {
                if (v.getTipoValvula() != null && v.getTipoValvula().getId() != null) {
                    tiposUnicos.put(v.getTipoValvula().getId(), v.getTipoValvula());
                }
            }

            java.util.List<TipoValvula> listaTipos = new java.util.ArrayList<>(tiposUnicos.values());
            listaTipos.sort((t1, t2) -> {
                String n1 = t1.getNombre() == null ? "" : t1.getNombre();
                String n2 = t2.getNombre() == null ? "" : t2.getNombre();
                return n1.compareToIgnoreCase(n2);
            });

            for (TipoValvula tv : listaTipos) {
                cmbTipo.addItem(tv);
            }
        }
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
            stopDataCapture(null, null, null);
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

        medicionActual = new Medicion(
                valvula,
                cliente,
                view.getSelectedOperador(),
                view.getSelectedFluido(),
                currentPressureRequested,
                view.getSelectedPressureUnit()
        );

        if (currentPressureRequested <= 0) {
            view.showErrorMessage("Ingrese un valor válido para la Presión Solicitada antes de iniciar la simulación.");
            return;
        }

        this.pressureRequested = currentPressureRequested;

        try {
            view.setLedColor(Color.ORANGE);

            resetValues();
            view.clearChart();

            running = false;
            view.setStartButtonText("Detener (Simulando)");
            view.setInfoFieldsEnabled(false);

            dataThread = new Thread(() -> {
                try {
                    double simulatedTime = 0.0;
                    double simulatedVolt = constanteC;

                    double targetVolt = (pressureRequested / factorA) + constanteC + 0.2;

                    while (!Thread.currentThread().isInterrupted()) {
                        if (simulatedVolt < targetVolt) {
                            simulatedVolt += 0.02 + (Math.random() * 0.01);
                        } else {
                            simulatedVolt += (Math.random() * 0.02) - 0.01;
                        }

                        double tempRaw = (25.0 / factorATemp) + constanteCTemp + (Math.random() * 2.0 - 1.0);

                        String simulatedData = String.format(java.util.Locale.US, "%.1f,%.4f,%.4f,%.2f",
                                simulatedTime, simulatedVolt, simulatedVolt, tempRaw);

                        processNewData(simulatedData);

                        simulatedTime += 0.1;
                        Thread.sleep(100);
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

    public void updateValvulasFiltradas(JComboBox<Valvula> cmbValvula, Planta planta, TipoValvula tipoFiltro) {
        if (planta == null || cmbValvula == null) return;

        List<Valvula> valvulas = planta.getValvulas();
        cmbValvula.removeAllItems();

        if (valvulas != null) {
            List<Valvula> valvulasOrdenadas = new ArrayList<>(valvulas);
            valvulasOrdenadas.sort((v1, v2) -> {
                String tag1 = v1.getTag() == null ? "" : v1.getTag();
                String tag2 = v2.getTag() == null ? "" : v2.getTag();
                return tag1.compareToIgnoreCase(tag2);
            });

            for (Valvula v : valvulasOrdenadas) {
                if (tipoFiltro == null || tipoFiltro.getId() == 0 ||
                        (v.getTipoValvula() != null && v.getTipoValvula().getId().equals(tipoFiltro.getId()))) {
                    cmbValvula.addItem(v);
                }
            }
        }
    }

    public void updateValvulasPorPlanta(JComboBox<Valvula> cmbValvula, Planta selected) {
        if (cmbValvula == null) {
            return;
        }

        cmbValvula.removeAllItems();

        if (selected != null && selected.getValvulas() != null) {
            List<Valvula> valvulasDePlanta = new ArrayList<>(selected.getValvulas());

            valvulasDePlanta.sort((v1, v2) -> {
                String tag1 = v1.getTag() == null ? "" : v1.getTag();
                String tag2 = v2.getTag() == null ? "" : v2.getTag();
                return tag1.compareToIgnoreCase(tag2);
            });

            for (Valvula valvula : valvulasDePlanta) {
                cmbValvula.addItem(valvula);
            }
        }
    }

    public void updatePlantas(JComboBox<Planta> cmbPlanta, Cliente selected) {
        if (cmbPlanta == null) {
            return;
        }
        cmbPlanta.removeAllItems();

        List<Planta> plantasDeCliente = new ArrayList<>(selected.getPlantas());

        plantasDeCliente.sort((p1, p2) -> {
            String nombre1 = p1.getNombre() == null ? "" : p1.getNombre();
            String nombre2 = p2.getNombre() == null ? "" : p2.getNombre();
            return nombre1.compareToIgnoreCase(nombre2);
        });

        for (Planta planta : plantasDeCliente) {
            cmbPlanta.addItem(planta);
        }
    }

    public void loadRadioButtonsData(JRadioButton rbtnBarg, JRadioButton rbtnKgCm2, JRadioButton rbtnPSIG, ButtonGroup grupoUnidades) {
        rbtnPSIG = new javax.swing.JRadioButton("PSIG");
        rbtnKgCm2 = new javax.swing.JRadioButton("Kg/cm²");
        rbtnBarg = new javax.swing.JRadioButton("Barg");

        grupoUnidades = new javax.swing.ButtonGroup();
        grupoUnidades.add(rbtnPSIG);
        grupoUnidades.add(rbtnKgCm2);
        grupoUnidades.add(rbtnBarg);
    }
}