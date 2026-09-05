package com.mediciones.gestor;

import com.fazecast.jSerialComm.SerialPort;
import com.mediciones.config.AppConfig;
import com.mediciones.model.SensorCalibracion;
import com.mediciones.utils.ConversionUnidadesUtil;
import com.mediciones.view.FrmInicio;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FrmInicioGestor {
    private static final Logger LOGGER = Logger.getLogger(FrmInicioGestor.class.getName());
    private final FrmInicio view;
    private final SensorCalibracionGestor calibracionController;
    private final UbicacionGestor ubicacionGestor;

    private SerialPort comPort;
    private Thread captureThread;
    private volatile boolean capturing = false;

    private double a1 = 1.0, c1 = 0.0;
    private double a2 = 1.0, c2 = 0.0;

    // --- NUEVA VARIABLE GLOBAL PARA ESCUCHAR CAMBIOS DE UNIDAD ---
    private volatile ConversionUnidadesUtil.UnidadPresion currentUnidadDestino = ConversionUnidadesUtil.UnidadPresion.KGCM2;

    public FrmInicioGestor(FrmInicio view) {
        this.view = view;
        this.calibracionController = new SensorCalibracionGestor();
        this.ubicacionGestor = new UbicacionGestor();
    }

    // --- NUEVO MÉTODO QUE FRMINICIO ESTABA BUSCANDO ---
    public void setUnidadDestino(ConversionUnidadesUtil.UnidadPresion unidad) {
        this.currentUnidadDestino = unidad;
    }

    public boolean existeConfiguracion() {
        ConfiguracionGestor configuracionGestor = new ConfiguracionGestor();
        return configuracionGestor.existeConfiguracion();
    }

    public void cargarConstantesCalibracion() {
        try {
            SensorCalibracion calibM = calibracionController.obtenerUltimaCalibracionPorSensor("CS-PT1200");
            if (calibM != null) {
                a1 = (calibM.getA1() != null) ? calibM.getA1() : 1.0;
                c1 = (calibM.getC1() != null) ? calibM.getC1() : 0.0;
            }
            SensorCalibracion calibE = calibracionController.obtenerUltimaCalibracionPorSensor("Endress-Hauser");
            if (calibE != null) {
                a2 = (calibE.getA2() != null) ? calibE.getA2() : 1.0;
                c2 = (calibE.getC2() != null) ? calibE.getC2() : 0.0;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar las constantes de calibración", e);
        }
    }

    public void actualizarListaPuertos(JComboBox<String> cbPuertos) {
        if(AppConfig.modoSimulado) {
            cbPuertos.removeAllItems();
            cbPuertos.addItem("SIMULADOR");
        }
        else {
            String seleccionActual = (String) cbPuertos.getSelectedItem();
            cbPuertos.removeAllItems();
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort p : ports) {
                cbPuertos.addItem(p.getSystemPortName());
            }
            if (seleccionActual != null) {
                cbPuertos.setSelectedItem(seleccionActual);
            }
        }
    }

    public void iniciarComunicacionSerial(
            String portName,
            ConversionUnidadesUtil.UnidadPresion unidadDestino,
            Consumer<Double> updateSensorUno,
            Consumer<Double> updateSensorDos) {

        // 1. Guardamos la unidad apenas arranca
        this.currentUnidadDestino = unidadDestino;

        if (capturing) {
            detenerComunicacionSerial();
        }

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0 && !"SIMULADOR".equals(portName)) {
            LOGGER.warning("No se encontraron puertos seriales disponibles.");
            view.setEstadoComunicacion(false);
            return;
        }

        if ("SIMULADOR".equals(portName)) {
            iniciarSimulador(updateSensorUno, updateSensorDos);
            return;
        }

        comPort = (portName != null)
                ? SerialPort.getCommPort(portName)
                : ports[0];

        comPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (comPort.openPort()) {

            capturing = true;
            view.setEstadoComunicacion(true);

            captureThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(comPort.getInputStream()))) {

                    while (capturing && comPort.isOpen()) {
                        if (comPort.bytesAvailable() > 0) {
                            String line = reader.readLine();

                            if (line != null && !line.trim().isEmpty()) {
                                // INYECTAMOS LA VARIABLE GLOBAL EN EL CÁLCULO
                                procesarDatosSerial(
                                        line.trim(),
                                        this.currentUnidadDestino,
                                        updateSensorUno,
                                        updateSensorDos
                                );
                            }
                        }
                        Thread.sleep(50);
                    }

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error durante la captura de datos seriales", ex);
                    SwingUtilities.invokeLater(() -> view.setEstadoComunicacion(false));
                }
            });

            captureThread.setDaemon(true);
            captureThread.start();

        } else {
            LOGGER.severe("No se pudo abrir el puerto serial: " + comPort.getSystemPortName());
            view.setEstadoComunicacion(false);
        }
    }

    public void detenerComunicacionSerial() {
        capturing = false;
        if (comPort != null && comPort.isOpen()) comPort.closePort();
        view.setEstadoComunicacion(false);
    }

    private void procesarDatosSerial(
            String rawLine,
            ConversionUnidadesUtil.UnidadPresion unidadDestino,
            Consumer<Double> updateSensorUno,
            Consumer<Double> updateSensorDos) {

        try {
            String[] parts = rawLine.split(",");

            if (parts.length >= 3) {
                double pr1 = Double.parseDouble(parts[1].trim());
                double pr2 = Double.parseDouble(parts[2].trim());

                // Calibración
                double presionCalibrada1 = Math.max(0, (pr1 - c1) * a1);
                double presionCalibrada2 = Math.max(0, (pr2 - c2) * a2);

                // Conversión de unidad en tiempo real
                double presionFinal1 = ConversionUnidadesUtil.convertirPresion(
                        presionCalibrada1,
                        ConversionUnidadesUtil.UnidadPresion.BARG,
                        unidadDestino);

                double presionFinal2 = ConversionUnidadesUtil.convertirPresion(
                        presionCalibrada2,
                        ConversionUnidadesUtil.UnidadPresion.BARG,
                        unidadDestino);

                SwingUtilities.invokeLater(() -> {
                    updateSensorUno.accept(presionFinal1);
                    updateSensorDos.accept(presionFinal2);
                });
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            LOGGER.severe("Error al procesar la lectura serial: " + e.getMessage());
        }
    }

    public void openFrmReportes(JFrame parentComponent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar carpeta para guardar reportes Excel");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(parentComponent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            String ruta = selectedDir.getAbsolutePath();

            boolean ok = ubicacionGestor.guardarUbicacion(ruta);

            if (ok) {
                JOptionPane.showMessageDialog(
                        parentComponent,
                        "Ubicación de reportes guardada:\n" + ruta,
                        "Ubicación guardada",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                LOGGER.severe("No se pudo guardar la ubicación del reporte: " + ruta);
                JOptionPane.showMessageDialog(
                        parentComponent,
                        "No se pudo guardar la ubicación.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void iniciarSimulador(
            Consumer<Double> updateSensorUno,
            Consumer<Double> updateSensorDos) {

        capturing = true;

        Thread simuladorThread = new Thread(() -> {
            double p1 = 1.0;
            double p2 = 100.0;

            while (capturing && !Thread.currentThread().isInterrupted()) {
                try {
                    String tramaFalsa = "0," + p1 + "," + p2;

                    // EL SIMULADOR TAMBIÉN ESCUCHA LOS CLICS EN TIEMPO REAL
                    procesarDatosSerial(
                            tramaFalsa,
                            this.currentUnidadDestino,
                            updateSensorUno,
                            updateSensorDos
                    );

                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        simuladorThread.setDaemon(true);
        simuladorThread.start();
    }
}