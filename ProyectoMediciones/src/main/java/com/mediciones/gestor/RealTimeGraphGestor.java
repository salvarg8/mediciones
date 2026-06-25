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

public class RealTimeGraphGestor {
    private final RealTimeGraph view;
    private final RealTimeGraphDAO dao;
    private final UbicacionGestor ubicacionGestor;
    private final ValvulaGestor valvulaGestor;
    private final TipoValvulaGestor tipoValvulaGestor; // NUEVO: Gestor de Tipos
    private ConfiguracionDAO configDAO;

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

    private static final Logger logger = LoggerFactory.getLogger(RealTimeGraphGestor.class);

    public RealTimeGraphGestor(RealTimeGraph view) {
        this.view = view;
        this.dao = new RealTimeGraphDAO();
        this.ubicacionGestor = new UbicacionGestor();
        this.valvulaGestor = new ValvulaGestor();
        this.tipoValvulaGestor = new TipoValvulaGestor(); // NUEVO: Inicialización
        this.configDAO = new ConfiguracionDAO();
    }

    public void init() {
        loadCalibrationValues();
    }

    public void autoSelectAndOpenPort(JComboBox<String> portCombo) {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) return;

        // 1. Obtenemos el puerto guardado en la base de datos
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

        // 3. Si no tenía puerto guardado (o el cable no está), usamos la lógica automática
        if (selectedIndex == -1) {
            for (int i = 0; i < ports.length; i++) {
                String name = ports[i].getDescriptivePortName().toLowerCase();
                if (name.contains("arduino") || name.contains("usb") || name.contains("ch340")) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        // 4. Si fallan ambas, seleccionamos el último de la lista como fallback
        if (selectedIndex == -1) {
            selectedIndex = ports.length - 1;
        }

        portCombo.setSelectedIndex(selectedIndex);
        // openSelectedPort(ports[selectedIndex]); // Descomenta esta línea si abrías el puerto automáticamente al iniciar
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
            Ubicacion u = ubicacionGestor.obtenerUbicacion();
            nombreUltimoArchivoCSV = (u != null && u.getUbicacion() != null)
                    ? new File(new File(u.getUbicacion()), fileName).getAbsolutePath()
                    : fileName;

            resetValues();
            view.clearChart();

            running = false;
            view.setStartButtonText("Detener");
            view.setInfoFieldsEnabled(false);

            dataThread = new Thread(() -> {
                boolean porDesconexion = false;
                String mensajeDesconexion = "Error desconocido en los sensores.";

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(comPort.getInputStream(), StandardCharsets.UTF_8))) {
                    // Guardamos el momento exacto en el que inicia la captura
                    long ultimoDatoTime = System.currentTimeMillis();

                    while (comPort.isOpen() && !Thread.currentThread().isInterrupted()) {
                        int bytesDisponibles = comPort.bytesAvailable();

                        // CASO 1: Desconexión física instantánea (jSerialComm retorna -1 si se desenchufa el USB)
                        if (bytesDisponibles < 0) {
                            throw new IOException("Se desconectó físicamente el cable USB del banco de pruebas.");
                        }

                        if (bytesDisponibles > 0) {
                            String d = reader.readLine();
                            if (d != null) {
                                processNewData(d);
                                ultimoDatoTime = System.currentTimeMillis(); // Reseteamos el temporizador si llegaron datos
                            }
                        } else {
                            // CASO 2: El cable sigue enchufado pero el microcontrolador dejó de transmitir datos (se colgó)
                            // Si pasan más de 2000 milisegundos (2 segundos) sin recibir nada, asumimos pérdida de señal
                            if (System.currentTimeMillis() - ultimoDatoTime > 2000) {
                                throw new IOException("Se perdió la señal del sensor. El dispositivo no responde (Timeout de 2s).");
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
                    // Si el hilo terminó debido a una desconexión abrupta/error, actualizamos la UI
                    if (porDesconexion) {
                        final String msgPopUp = mensajeDesconexion;
                        SwingUtilities.invokeLater(() -> {
                            view.setLedColor(Color.RED); // Cambiamos el indicador visual a ROJO
                            view.showErrorMessage("⚠️ Alerta de Hardware:\n" + msgPopUp); // Lanzamos el cartel de alerta
                        });
                    }

                    // Aseguramos que la interfaz vuelva a su estado original de todas formas
                    stopDataCapture(valvula, view.getSelectedOperador(), view.getSelectedFluido());
                }
            });
            dataThread.start();

            if (!comPort.openPort()) throw new IOException("No se pudo abrir el puerto.");
            view.setLedColor(Color.GREEN);

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

        // 1. SOLUCIÓN AL BUG: Tomar una copia segura y rápida de la lista.
        // Bloqueamos csvDataPoints solo un instante para copiarla sin que el hilo serial interfiera.
        List<String> copiaDatos;
        synchronized (csvDataPoints) {
            copiaDatos = new ArrayList<>(csvDataPoints);
        }

        // 2. A partir de aquí, usamos "copiaDatos" en lugar de "csvDataPoints"
        if (copiaDatos.isEmpty()) {
            view.resetCaptureUI();
            return;
        }

        double minPressure = 0.76 * pressureRequested;
        int startIndex = -1;
        double startTime = 0;

        // Iteramos sobre la copia de forma 100% segura
        for (int i = 0; i < copiaDatos.size(); i++) {
            try {
                String[] parts = copiaDatos.get(i).split(";");
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
                String[] parts = copiaDatos.get(0).split(";");
                if (parts.length >= 1) {
                    startTime = Double.parseDouble(parts[0]);
                }
            } catch (Exception ignored) {
            }
        }

        // Ya no necesitamos sincronizar esta lista porque es totalmente local
        List<String> filteredDataPoints = new ArrayList<>();
        for (int i = startIndex; i < copiaDatos.size(); i++) {
            try {
                String[] parts = copiaDatos.get(i).split(";");
                if (parts.length >= 3) {
                    double time = Double.parseDouble(parts[0]);
                    double pressure = Double.parseDouble(parts[1]);
                    double temperature = Double.parseDouble(parts[2]);
                    double adjustedTime = time - startTime;
                    filteredDataPoints.add(String.format(java.util.Locale.US, "%.1f;%.2f;%.2f", adjustedTime, pressure, temperature));
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
                writer.write("Maximo;" + String.format(java.util.Locale.US, "%.2f", maxValue) + "\n");
                writer.write("Recuperacion;" + ((recValue == Double.MAX_VALUE || !maxReached) ? "0.00" : String.format(java.util.Locale.US, "%.2f", recValue)) + "\n");
                writer.write("\ntiempo_s;Presion (" + view.getSelectedPressureUnit() + ");temperatura\n");

                // Borramos el "synchronized (filteredDataPoints)" que era inútil
                for (String p : filteredDataPoints) {
                    writer.write(p + "\n");
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
            Ubicacion ubicacion = ubicacionGestor.obtenerUbicacion();
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

            // Armamos la ruta base original
            File archivoDestinoBase = new File(direccion, nombreArchivo);

            // PASO CLAVE: Pasamos la ruta por el validador para que agregue (1), (2)... si ya existe
            String rutaUnica = obtenerRutaUnica(archivoDestinoBase.getAbsolutePath());
            File archivoDestinoFinal = new File(rutaUnica);

            // Generamos el Excel apuntando a la ruta única garantizada
            new ExcelGenerator().generarExcel(nombreUltimoArchivoCSV, archivoDestinoFinal.getAbsolutePath());

            // Mostramos el mensaje con el nombre final que se le asignó
            view.showMessage("Reporte generado exitosamente en:\n" + archivoDestinoFinal.getAbsolutePath());

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


    /**
     * Verifica si un archivo ya existe y genera un nombre único agregando (1), (2), etc.
     */
    private String obtenerRutaUnica(String rutaCompleta) {
        java.io.File archivo = new java.io.File(rutaCompleta);

        // Si el archivo no existe, la ruta original está perfecta
        if (!archivo.exists()) {
            return rutaCompleta;
        }

        // Si existe, separamos la ruta, el nombre y la extensión
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

        // Bucle hasta encontrar un número que no esté en uso
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

            // Usamos un Map para guardar los tipos únicos (evitando duplicados por ID)
            java.util.Map<Integer, TipoValvula> tiposUnicos = new java.util.HashMap<>();

            for (Valvula v : planta.getValvulas()) {
                if (v.getTipoValvula() != null && v.getTipoValvula().getId() != null) {
                    tiposUnicos.put(v.getTipoValvula().getId(), v.getTipoValvula());
                }
            }

            // Convertimos a lista y ordenamos alfabéticamente
            java.util.List<TipoValvula> listaTipos = new java.util.ArrayList<>(tiposUnicos.values());
            listaTipos.sort((t1, t2) -> {
                String n1 = t1.getNombre() == null ? "" : t1.getNombre();
                String n2 = t2.getNombre() == null ? "" : t2.getNombre();
                return n1.compareToIgnoreCase(n2);
            });

            // Agregamos al combobox
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

            Ubicacion u = ubicacionGestor.obtenerUbicacion();
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

    // NUEVO: Método para actualizar Válvulas Filtradas con ORDENAMIENTO INCLUIDO
    public void updateValvulasFiltradas(JComboBox<Valvula> cmbValvula, Planta planta, TipoValvula tipoFiltro) {
        if (planta == null || cmbValvula == null) return;

        List<Valvula> valvulas = planta.getValvulas();
        cmbValvula.removeAllItems();

        if (valvulas != null) {
            // Creamos una copia para ordenarlas por Tag alfabéticamente
            List<Valvula> valvulasOrdenadas = new ArrayList<>(valvulas);
            valvulasOrdenadas.sort((v1, v2) -> {
                String tag1 = v1.getTag() == null ? "" : v1.getTag();
                String tag2 = v2.getTag() == null ? "" : v2.getTag();
                return tag1.compareToIgnoreCase(tag2);
            });

            for (Valvula v : valvulasOrdenadas) {
                // Si el filtro es nulo, es 0 ("Todos los tipos"), o coincide con el de la válvula
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

        for (Planta planta : plantasDeCliente){ {
            cmbPlanta.addItem(planta);
        }}
    }
}