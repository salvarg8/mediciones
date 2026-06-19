package com.mediciones.view;

import com.fazecast.jSerialComm.SerialPort;
import com.mediciones.gestor.SensorCalibracionGestor;
import com.mediciones.model.SensorCalibracion;
import com.mediciones.view.components.Button3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.border.AbstractBorder;

public class FrmCalibracionSensor extends JFrame {

    private SerialPort comPort;
    private volatile boolean capturing = false;
    private Thread captureThread;
    private double voltageAtZeroBar = Double.NaN;
    private double voltageAtKnownBar = Double.NaN;
    private JTextField txtRawData;

    private volatile double currentRawVoltage = Double.NaN;

    private double a1 = 1.0, c1 = 0.0;
    private double a2 = 1.0, c2 = 0.0;
    private double a3 = 1.0, c3 = 0.0;
    private String selectedSensor = "Motorola";

    private JLabel lblPresionActual;
    private JTextField txtPresion1, txtPresion2;
    private Button3D btnConfirmar1, btnConfirmar2, btnAceptar, btnCancel;
    private JLabel lblSignal0, lblSignal7;
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private JComboBox<String> sensorCombo;

    private SensorCalibracionGestor calibracionController;

    private JLabel lblAValue;
    private JLabel lblCValue;
    private JLabel lblA;
    private JLabel lblC;

    private JPanel puntosPanel;

    private static final Logger logger = LoggerFactory.getLogger(FrmCalibracionSensor.class);

    public FrmCalibracionSensor() {
        this.calibracionController = new SensorCalibracionGestor();
        setTitle("Calibración de Sensores");

        setSize(1000, 800);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        setLayout(new GridBagLayout());
        setResizable(true);

        initComponents();
        setupListeners();
        loadCurrentCalibration();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int fontSize = Math.max(20, getHeight() / 20);
                lblPresionActual.setFont(new Font("Consolas", Font.BOLD, fontSize));
            }
        });

        autoSelectPort();
        startCaptureOnFormOpen();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void autoSelectPort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length > 0) {
            for (int i = 0; i < ports.length; i++) {
                String name = ports[i].getDescriptivePortName().toLowerCase();
                if (name.contains("arduino") || name.contains("usb") || name.contains("ch340")) {
                    portCombo.setSelectedIndex(i);
                    return;
                }
            }
            portCombo.setSelectedIndex(ports.length - 1);
        }
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Sensor a Calibrar: ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        sensorCombo = new JComboBox<>(new String[]{
                "Motorola (0-7 Bar)",
                "Endress-Hauser (0-100 Bar)",
                "LM35 (Temperatura)"
        });
        sensorCombo.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel titleAndSensor = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        titleAndSensor.add(titleLabel); titleAndSensor.add(sensorCombo);
        topPanel.add(titleAndSensor, BorderLayout.NORTH);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.1;
        add(topPanel, gbc);

        puntosPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        puntosPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Puntos de calibración"));

        txtPresion1 = new JTextField("0.0"); txtPresion1.setEditable(false);
        txtPresion1.setFont(new Font("Arial", Font.PLAIN, 18));
        btnConfirmar1 = new Button3D("Confirmar 1", Color.GREEN.brighter(), true);

        txtPresion2 = new JTextField("");
        txtPresion2.setFont(new Font("Arial", Font.PLAIN, 18));
        btnConfirmar2 = new Button3D("Confirmar 2", Color.GREEN.brighter(), true);

        puntosPanel.add(new JLabel("Valor 1 (Ref):", SwingConstants.RIGHT)); puntosPanel.add(txtPresion1); puntosPanel.add(btnConfirmar1);
        puntosPanel.add(new JLabel("Valor 2 (Ref):", SwingConstants.RIGHT)); puntosPanel.add(txtPresion2); puntosPanel.add(btnConfirmar2);

        gbc.gridy = 1; gbc.weighty = 0.2; add(puntosPanel, gbc);

        lblPresionActual = new JLabel("0.00 Barg", SwingConstants.CENTER);
        lblPresionActual.setForeground(Color.WHITE); lblPresionActual.setOpaque(true);
        lblPresionActual.setBackground(new Color(0, 0, 150)); lblPresionActual.setFont(new Font("Consolas", Font.BOLD, 48));
        gbc.gridy = 2; gbc.weighty = 0.3; add(lblPresionActual, gbc);

        JPanel voltajePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        voltajePanel.setBorder(BorderFactory.createTitledBorder("Voltajes detectados"));
        lblSignal0 = new JLabel("Voltaje en Punto 1: --- V"); lblSignal0.setFont(new Font("Arial", Font.PLAIN, 16));
        lblSignal7 = new JLabel("Voltaje en Punto 2: --- V"); lblSignal7.setFont(new Font("Arial", Font.PLAIN, 16));
        voltajePanel.add(lblSignal0); voltajePanel.add(lblSignal7);
        gbc.gridy = 3; gbc.weighty = 0.1; add(voltajePanel, gbc);

        JPanel constantesPanel = new JPanel(new BorderLayout(10, 10));
        constantesPanel.setBorder(BorderFactory.createTitledBorder("Constantes Calculadas"));

        JPanel labelsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        lblA = new JLabel("a =", SwingConstants.RIGHT); lblA.setFont(new Font("Arial", Font.BOLD, 18));
        lblAValue = new JLabel("???"); lblAValue.setFont(new Font("Arial", Font.BOLD, 18));
        lblC = new JLabel("c =", SwingConstants.RIGHT); lblC.setFont(new Font("Arial", Font.BOLD, 18));
        lblCValue = new JLabel("???"); lblCValue.setFont(new Font("Arial", Font.BOLD, 18));
        labelsPanel.add(lblA); labelsPanel.add(lblAValue); labelsPanel.add(lblC); labelsPanel.add(lblCValue);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        btnAceptar = new Button3D("Guardar", Color.GREEN.brighter(), true);
        btnAceptar.setPreferredSize(new Dimension(150, 50));
        btnCancel = new Button3D("Salir", new Color(255, 200, 200), true);
        btnCancel.setPreferredSize(new Dimension(150, 50));
        buttonsPanel.add(btnAceptar); buttonsPanel.add(btnCancel);

        constantesPanel.add(labelsPanel, BorderLayout.NORTH); constantesPanel.add(buttonsPanel, BorderLayout.CENTER);
        gbc.gridy = 4; gbc.weighty = 0.2; add(constantesPanel, gbc);

        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        txtRawData = new JTextField(20); txtRawData.setEditable(false);
        txtRawData.setFont(new Font("Consolas", Font.PLAIN, 14));
        portCombo = new JComboBox<>();
        for (SerialPort port : SerialPort.getCommPorts()) portCombo.addItem(port.getSystemPortName());
        baudCombo = new JComboBox<>(new Integer[]{9600, 19200, 38400, 57600, 115200});
        portPanel.add(new JLabel("RAW:")); portPanel.add(txtRawData);
        portPanel.add(new JLabel("Puerto:")); portPanel.add(portCombo);
        portPanel.add(new JLabel("Baud:")); portPanel.add(baudCombo);
        gbc.gridy = 5; gbc.weighty = 0.1; add(portPanel, gbc);
    }

    private void setupListeners() {
        sensorCombo.addActionListener(e -> {
            String selected = (String) sensorCombo.getSelectedItem();
            if (selected != null) {
                selectedSensor = selected.contains("Motorola") ? "Motorola" :
                        (selected.contains("Endress-Hauser") ? "Endress-Hauser" : "LM35");

                // ✅ Solo lo solicitado:
                if (selectedSensor.equals("LM35")) {
                    btnConfirmar1.setText("Innecesario");
                    btnConfirmar1.setEnabled(false); // <-- clave: inactivo
                } else {
                    btnConfirmar1.setText("Confirmar 1");
                    btnConfirmar1.setEnabled(true);
                }

                voltageAtZeroBar = Double.NaN;
                voltageAtKnownBar = Double.NaN;
                currentRawVoltage = Double.NaN;
                lblSignal0.setText("Voltaje en Punto 1: --- V");
                lblSignal7.setText("Voltaje en Punto 2: --- V");
                loadCurrentCalibration();
            }
        });

        btnConfirmar1.addActionListener(e -> {
            if (!startPortIfNotOpen()) return;
            if (Double.isNaN(currentRawVoltage)) {
                JOptionPane.showMessageDialog(this, "Sin señal del sensor.");
                return;
            }
            voltageAtZeroBar = currentRawVoltage;
            lblSignal0.setText(String.format("Voltaje en Punto 1: %.3f V", voltageAtZeroBar));
        });

        btnConfirmar2.addActionListener(e -> {
            // ✅ Para LM35: no exigir voltageAtZeroBar (ya está deshabilitado, pero por seguridad)
            if (!selectedSensor.equals("LM35") && Double.isNaN(voltageAtZeroBar)) {
                JOptionPane.showMessageDialog(this, "Capture Punto 1 primero.");
                return;
            }
            if (!startPortIfNotOpen()) return;
            if (Double.isNaN(currentRawVoltage)) {
                JOptionPane.showMessageDialog(this, "Sin señal del sensor.");
                return;
            }
            try {
                double val2 = Double.parseDouble(txtPresion2.getText());
                voltageAtKnownBar = currentRawVoltage;
                lblSignal7.setText(String.format("Voltaje en Punto 2: %.3f V", voltageAtKnownBar));
                double c = selectedSensor.equals("LM35") ? 0.0 : voltageAtZeroBar; // ⭐️ c = 0 para LM35
                double a = val2 / (voltageAtKnownBar - c);
                if (selectedSensor.equals("Motorola")) { a1 = a; c1 = c; }
                else if (selectedSensor.equals("Endress-Hauser")) { a2 = a; c2 = c; }
                else { a3 = a; c3 = 0.0; } // ⭐️ c3 siempre 0 para LM35
                lblAValue.setText(String.format("%.6f", a)); lblCValue.setText(String.format("%.6f", c));
            } catch (Exception ex) {
                logger.error("valor 2 inválido", ex);
                JOptionPane.showMessageDialog(this, "Valor 2 inválido.");
            }
        });

        btnAceptar.addActionListener(e -> {
            if (Double.isNaN(voltageAtKnownBar)) {
                JOptionPane.showMessageDialog(this, "Falta capturar Punto 2.");
                return;
            }
            SensorCalibracion calib = new SensorCalibracion();
            calib.setSensorType(selectedSensor);
            if (selectedSensor.equals("Motorola")) { calib.setA1(a1); calib.setC1(c1); }
            else if (selectedSensor.equals("Endress-Hauser")) { calib.setA2(a2); calib.setC2(c2); }
            else { calib.setA3(a3); calib.setC3(0.0); } // ⭐️ forzar c3 = 0 al guardar
            calib.setPresionConocida(Double.parseDouble(txtPresion2.getText()));
            calib.setVoltajeConocido(voltageAtKnownBar);
            calib.setFechaCalibracion(new java.util.Date());
            if (calibracionController.guardarCalibracion(calib)) {
                JOptionPane.showMessageDialog(this, "Calibración guardada."); dispose();
            }
        });

        btnCancel.addActionListener(e -> dispose());
    }

    private boolean startPortIfNotOpen() {
        if (comPort != null && comPort.isOpen()) return true;
        try {
            comPort = SerialPort.getCommPort((String) portCombo.getSelectedItem());
            comPort.setComPortParameters((int) baudCombo.getSelectedItem(), 8, 1, SerialPort.NO_PARITY);
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
            if (!comPort.openPort()) return false;
            Thread.sleep(1000); startVoltageCapture(); return true;
        } catch (Exception ex) {
            logger.error("error en startPortIfNotOpen()", ex);
            return false;
        }
    }

    private void startVoltageCapture() {
        if (capturing) return;
        capturing = true;
        captureThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(comPort.getInputStream()))) {
                while (capturing && comPort.isOpen()) {
                    if (comPort.bytesAvailable() > 0) {
                        String line = reader.readLine();
                        if (line != null && line.contains(",")) {
                            String[] p = line.split(",");
                            if (p.length >= 3) {
                                try {
                                    double v;
                                    if (selectedSensor.equals("Motorola"))
                                        v = Double.parseDouble(p[1].replace(",", "."));
                                    else if (selectedSensor.equals("Endress-Hauser"))
                                        v = Double.parseDouble(p[2].replace(",", "."));
                                    else
                                        v = (p.length >= 4) ? Double.parseDouble(p[3].replace(",", ".")) : 0.0;

                                    currentRawVoltage = v;
                                    SwingUtilities.invokeLater(() -> {
                                        txtRawData.setText(line.trim());
                                        double a = selectedSensor.equals("Motorola") ? a1 : (selectedSensor.equals("Endress-Hauser") ? a2 : a3);
                                        double c = selectedSensor.equals("Motorola") ? c1 : (selectedSensor.equals("Endress-Hauser") ? c2 : 0.0); // LM35: c=0
                                        double finalVal = a * (currentRawVoltage - c);
                                        lblPresionActual.setText(String.format("%.2f %s", Math.max(0, finalVal),
                                                selectedSensor.equals("LM35") ? "°C" : "Barg"));
                                    });
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (Exception ignored) {}
        });
        captureThread.start();
    }

    private void loadCurrentCalibration() {
        try {
            SensorCalibracion m = calibracionController.obtenerUltimaCalibracionPorSensor("Motorola");
            if (m != null) { a1 = m.getA1() != null ? m.getA1() : 1.0; c1 = m.getC1() != null ? m.getC1() : 0.0; }
            SensorCalibracion e = calibracionController.obtenerUltimaCalibracionPorSensor("Endress-Hauser");
            if (e != null) { a2 = e.getA2() != null ? e.getA2() : 1.0; c2 = e.getC2() != null ? e.getC2() : 0.0; }
            SensorCalibracion l = calibracionController.obtenerUltimaCalibracionPorSensor("LM35");
            if (l != null) { a3 = l.getA3() != null ? l.getA3() : 1.0; c3 = l.getC3() != null ? l.getC3() : 0.0; }

            double a = selectedSensor.equals("Motorola") ? a1 : (selectedSensor.equals("Endress-Hauser") ? a2 : a3);
            double c = selectedSensor.equals("Motorola") ? c1 : (selectedSensor.equals("Endress-Hauser") ? c2 : c3);
            lblAValue.setText(String.format("%.6f", a)); lblCValue.setText(String.format("%.6f", c));
        } catch (Exception ex) {
            logger.error("Error en loadCurrentCalibration()",ex);
        }
    }

    private void startCaptureOnFormOpen() {
        startPortIfNotOpen();
    }

    @Override
    public void dispose() {
        capturing = false;
        if (comPort != null) comPort.closePort();
        super.dispose();
    }

    private class RoundedBorder extends AbstractBorder {
        private int r;
        public RoundedBorder(int r) { this.r = r; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.drawRoundRect(x, y, w - 1, h - 1, r, r);
        }
    }
}