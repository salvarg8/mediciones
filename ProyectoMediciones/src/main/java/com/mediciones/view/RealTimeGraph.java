package com.mediciones.view;

import com.fazecast.jSerialComm.*;
import com.mediciones.controller.RealTimeGraphController;
import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.model.Operador;
import com.mediciones.model.Valvula;
import com.mediciones.view.components.Button3D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.border.TitledBorder;

public class RealTimeGraph extends JFrame {
    private final RealTimeGraphController controller;
    private JFreeChart chart;
    private XYSeries series;
    private final int MAX_POINTS = 500;

    private JLabel valueLabel, maxLabel, recLabel, tempLabel, ledLabel;
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private Button3D startStopButton, generateReportButton, returnButton;
    private JComboBox<Valvula> cmbValvula;
    private JComboBox<Operador> cmbOperador;
    private JComboBox<Fluido> cmbFluido;
    private JComboBox<Cliente> cmbCliente;
    private Button3D btnSalir;
    private Button3D btnRecargarPortal;

    private int originalWidth = 900;
    private int originalHeight = 750;

    private String selectedPressureUnit = "Kgr/cm²";

    private JPanel northPanel;
    private JComboBox<String> cmbSensor;
    private JPanel indicatorPanel;
    private JTextField pressureRequestedField;

    public RealTimeGraph() {
        this.controller = new RealTimeGraphController(this);
        setTitle("Medición en Tiempo Real");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(originalWidth, originalHeight);
        setMinimumSize(new Dimension(800, 650));
        setLayout(new BorderLayout(10, 10));

        initComponents();
        controller.loadComboBoxData(cmbCliente, cmbOperador, cmbFluido);
        controller.init();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeProportionally();
            }
        });

        controller.autoSelectAndOpenPort(portCombo);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.closePort();
            }
        });
    }

    private void resizeProportionally() {
        int currentHeight = getHeight();
        double scaleFactor = (double) currentHeight / originalHeight;
        adjustLayout(scaleFactor);
        revalidate();
        repaint();
    }

    private void adjustLayout(double scaleFactor) {
        Font scaledFont = new Font("Segoe UI", Font.BOLD, Math.max(10, (int)(12 * scaleFactor)));
        Font comboBoxFont = new Font("Segoe UI", Font.PLAIN, Math.max(10, (int)(12 * scaleFactor)));
        Font labelFont = new Font("Segoe UI", Font.PLAIN, Math.max(10, (int)(12 * scaleFactor)));
        Font indicatorTitleFont = new Font("Segoe UI", Font.BOLD, Math.max(10, (int)(12 * scaleFactor)));
        Font indicatorValueFont = new Font("Consolas", Font.BOLD, Math.max(28, (int)(70 * scaleFactor)));

        if (northPanel != null && northPanel.getBorder() instanceof TitledBorder) {
            ((TitledBorder) northPanel.getBorder()).setTitleFont(scaledFont);
        }
        cmbCliente.setFont(comboBoxFont);
        cmbValvula.setFont(comboBoxFont);
        cmbOperador.setFont(comboBoxFont);
        cmbFluido.setFont(comboBoxFont);
        if (cmbSensor != null) cmbSensor.setFont(comboBoxFont);

        valueLabel.setFont(indicatorValueFont);
        maxLabel.setFont(indicatorValueFont);
        recLabel.setFont(indicatorValueFont);
        tempLabel.setFont(indicatorValueFont);

        if (indicatorPanel != null) {
            for (Component comp : indicatorPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getBorder() instanceof TitledBorder) {
                        TitledBorder border = (TitledBorder) panel.getBorder();
                        border.setTitleFont(indicatorTitleFont);
                        panel.setBorder(border);
                    }
                }
            }
        }

        startStopButton.setFont(scaledFont);
        generateReportButton.setFont(scaledFont);
        returnButton.setFont(scaledFont);
        btnSalir.setFont(scaledFont);

        if (portCombo != null) portCombo.setFont(comboBoxFont);
        if (baudCombo != null) baudCombo.setFont(comboBoxFont);

        adjustLabelFontsInContainer(getContentPane(), labelFont);
    }

    private void adjustLabelFontsInContainer(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setFont(font);
            } else if (comp instanceof Container) {
                adjustLabelFontsInContainer((Container) comp, font);
            }
        }
    }

    private void initComponents() {
        northPanel = new JPanel(new BorderLayout(0, 15));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("Medición", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        northPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel northCenterPanel = new JPanel(new BorderLayout(10, 10));
        JPanel valvulaSeleccionadaPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        valvulaSeleccionadaPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1), "Válvula Seleccionada",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Segoe UI", Font.BOLD, 12)));

        JPanel clientePanel = new JPanel(new BorderLayout(5, 0));
        clientePanel.add(new JLabel("Cliente:"), BorderLayout.WEST);
        cmbCliente = new JComboBox<>();
        valvulaSeleccionadaPanel.add(clientePanel);
        clientePanel.add(cmbCliente, BorderLayout.CENTER);

        JPanel valvulaPanel = new JPanel(new BorderLayout(5, 0));
        valvulaPanel.add(new JLabel("Válvula (TAG):"), BorderLayout.WEST);
        cmbValvula = new JComboBox<>();
        valvulaPanel.add(cmbValvula, BorderLayout.CENTER);
        valvulaSeleccionadaPanel.add(valvulaPanel);

        JPanel operadorPanel = new JPanel(new BorderLayout(5, 0));
        operadorPanel.add(new JLabel("Operador:"), BorderLayout.WEST);
        cmbOperador = new JComboBox<>();
        operadorPanel.add(cmbOperador, BorderLayout.CENTER);
        valvulaSeleccionadaPanel.add(operadorPanel);

        JPanel fluidoPanel = new JPanel(new BorderLayout(5, 0));
        fluidoPanel.add(new JLabel("Fluido:"), BorderLayout.WEST);
        cmbFluido = new JComboBox<>();
        fluidoPanel.add(cmbFluido, BorderLayout.CENTER);
        valvulaSeleccionadaPanel.add(fluidoPanel);

        northCenterPanel.add(valvulaSeleccionadaPanel, BorderLayout.NORTH);

        JPanel sensorAndPressurePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        sensorAndPressurePanel.setBorder(BorderFactory.createTitledBorder("Configuración del Sensor"));

        JPanel pressureRequestedPanel = new JPanel(new BorderLayout());
        pressureRequestedPanel.setBorder(BorderFactory.createTitledBorder("Presión Solicitada"));
        pressureRequestedField = new JTextField();
        pressureRequestedField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        pressureRequestedField.addActionListener(e -> updatePressureRequestedValue());
        pressureRequestedField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePressureRequestedValue(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePressureRequestedValue(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePressureRequestedValue(); }
        });

        pressureRequestedPanel.add(pressureRequestedField, BorderLayout.CENTER);

        JPanel sensorPanel = new JPanel(new GridBagLayout());
        sensorPanel.setBorder(BorderFactory.createTitledBorder("Elegir Sensor de Presión"));
        GridBagConstraints gbcSensor = new GridBagConstraints();
        gbcSensor.insets = new Insets(5, 5, 5, 5);
        gbcSensor.fill = GridBagConstraints.HORIZONTAL;
        String[] sensores = {"Motorola (0-7 Bar)", "Endress-Hauser (0-100 Bar)"};
        cmbSensor = new JComboBox<>(sensores);
        gbcSensor.gridx = 0;
        gbcSensor.gridy = 0;
        gbcSensor.gridwidth = 2;
        gbcSensor.weightx = 1.0;
        sensorPanel.add(cmbSensor, gbcSensor);
        cmbSensor.addActionListener(e -> {
            String selected = (String) cmbSensor.getSelectedItem();
            if (selected != null) {
                String sensorType = selected.contains("Motorola") ? "Motorola" : "Endress-Hauser";
                controller.updateSensorType(sensorType);
            }
        });

        sensorAndPressurePanel.add(pressureRequestedPanel);
        sensorAndPressurePanel.add(sensorPanel);

        northCenterPanel.add(sensorAndPressurePanel, BorderLayout.CENTER);

        indicatorPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        indicatorPanel.add(createIndicatorPanel("Valor actual", valueLabel = new JLabel("0", SwingConstants.CENTER), new Font("Consolas", Font.BOLD, 32), Color.BLACK));
        indicatorPanel.add(createIndicatorPanel("Valor máximo", maxLabel = new JLabel("0", SwingConstants.CENTER), new Font("Consolas", Font.BOLD, 32), Color.RED));
        indicatorPanel.add(createIndicatorPanel("Recuperación", recLabel = new JLabel("-", SwingConstants.CENTER), new Font("Consolas", Font.BOLD, 32), new Color(0, 100, 0)));
        indicatorPanel.add(createIndicatorPanel("Temperatura", tempLabel = new JLabel("0", SwingConstants.CENTER), new Font("Consolas", Font.BOLD, 32), Color.BLUE));

        northCenterPanel.add(indicatorPanel, BorderLayout.SOUTH);
        northPanel.add(northCenterPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        initChart();

        JPanel southPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcSouth = new GridBagConstraints();
        gbcSouth.insets = new Insets(5, 5, 5, 5);
        gbcSouth.fill = GridBagConstraints.HORIZONTAL;
        startStopButton = new Button3D("Iniciar toma de datos", new Color(200, 255, 200));
        generateReportButton = new Button3D("Generar Reporte", new Color(255, 255, 200));
        returnButton = new Button3D("Descartar", new Color(255, 200, 200));
        btnRecargarPortal = new Button3D("Recargar Datos  ",new Color(255, 255, 200));
        btnSalir = new Button3D("Salir", new Color(255, 200, 200));

        gbcSouth.gridx = 0;
        gbcSouth.gridy = 0;
        gbcSouth.weightx = 1.0;
        southPanel.add(startStopButton, gbcSouth);
        gbcSouth.gridx = 1;
        southPanel.add(generateReportButton, gbcSouth);
        gbcSouth.gridx = 2;
        southPanel.add(returnButton, gbcSouth);
        gbcSouth.gridx = 3;
        southPanel.add(btnRecargarPortal, gbcSouth);
        gbcSouth.gridx = 4;
        southPanel.add(btnSalir, gbcSouth);

        JPanel comPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        comPanel.add(new JLabel("Puerto:", SwingConstants.RIGHT));
        portCombo = new JComboBox<>();
        for (SerialPort port : SerialPort.getCommPorts()) portCombo.addItem(port.getSystemPortName());
        comPanel.add(portCombo);
        comPanel.add(new JLabel("Baudios:", SwingConstants.RIGHT));
        baudCombo = new JComboBox<>(new Integer[]{9600, 19200, 38400, 57600, 115200});
        comPanel.add(baudCombo);

        gbcSouth.gridx = 0;
        gbcSouth.gridy = 1;
        gbcSouth.gridwidth = 6;
        gbcSouth.fill = GridBagConstraints.NONE;
        southPanel.add(comPanel, gbcSouth);

        JPanel ledPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ledLabel = new JLabel(" ");
        ledLabel.setOpaque(true);
        ledLabel.setBackground(Color.RED);
        ledLabel.setPreferredSize(new Dimension(20, 20));
        ledPanel.add(ledLabel);
        ledPanel.add(new JLabel("Comunicación con sensores"));
        gbcSouth.gridx = 0;
        gbcSouth.gridy = 2;
        gbcSouth.anchor = GridBagConstraints.WEST;
        southPanel.add(ledPanel, gbcSouth);

        add(southPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    private void updatePressureRequestedValue() {
        String pressureText = pressureRequestedField.getText().trim();
        if (!pressureText.isEmpty()) {
            try {
                double newValue = Double.parseDouble(pressureText);
                if (newValue > 0) {
                    controller.updatePressureRequestedValue(newValue);
                } else {
                    showErrorMessage("La Presión Solicitada debe ser mayor que cero.");
                }
            } catch (NumberFormatException ex) {
                showErrorMessage("Ingrese un valor numérico válido para la Presión Solicitada.");
            }
        } else {
            controller.updatePressureRequestedValue(0.0);
        }
    }

    private void initChart() {
        series = new XYSeries("Presión (" + selectedPressureUnit + ")");
        chart = ChartFactory.createXYLineChart("", "Tiempo", "Presión (" + selectedPressureUnit + ")",
                new XYSeriesCollection(series), PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setLowerBound(0);
        rangeAxis.setUpperBound(10);

        add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    private JPanel createIndicatorPanel(String title, JLabel label, Font font, Color foreground) {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1), title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.setBorder(border); label.setFont(font); label.setForeground(foreground);
        panel.add(label, BorderLayout.CENTER); panel.setPreferredSize(new Dimension(150, 70));
        return panel;
    }

    private void setupListeners() {
        cmbCliente.addActionListener(e -> {
            Object selected = cmbCliente.getSelectedItem();
            controller.updateValvulas(cmbValvula, (selected instanceof Cliente) ? (Cliente) selected : null);
        });

        startStopButton.addActionListener(e -> {
            if (!controller.isRunning()) {
                double pressure = 0;
                try {
                    pressure = Double.parseDouble(pressureRequestedField.getText().trim());
                } catch(Exception ignored){}
                controller.startDataCapture(portCombo, baudCombo, (Cliente)cmbCliente.getSelectedItem(), (Valvula)cmbValvula.getSelectedItem(), pressure);
            } else {
                controller.stopDataCapture((Valvula)cmbValvula.getSelectedItem(), (Operador)cmbOperador.getSelectedItem(), (Fluido)cmbFluido.getSelectedItem());
            }
        });

        generateReportButton.addActionListener(e -> controller.guardarExcel((Cliente)cmbCliente.getSelectedItem(), (Valvula)cmbValvula.getSelectedItem(), (Operador)cmbOperador.getSelectedItem(), (Fluido)cmbFluido.getSelectedItem()));

        btnSalir.addActionListener(e -> {
            controller.closePort();
            dispose();
        });

        btnRecargarPortal.addActionListener(e -> controller.recargarPortal());

        returnButton.addActionListener(e -> controller.discardData());
    }

    // Public methods for Controller to update UI
    public void setLedColor(Color color) {
        ledLabel.setBackground(color);
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public void resetIndicators() {
        valueLabel.setText("0");
        maxLabel.setText("0");
        recLabel.setText("-");
        tempLabel.setText("0");
    }

    public void clearChart() {
        series.clear();
    }

    public void setStartButtonText(String text) {
        startStopButton.setText(text);
    }

    public void setInfoFieldsEnabled(boolean enabled) {
        cmbCliente.setEnabled(enabled);
        cmbValvula.setEnabled(enabled);
        cmbOperador.setEnabled(enabled);
        cmbFluido.setEnabled(enabled);
    }

    public void updateCurrentValue(double value) {
        valueLabel.setText(String.format("%.2f", value));
    }

    public void updateMaxValue(double value) {
        maxLabel.setText(String.format("%.2f", value));
    }

    public void updateRecValue(String text) {
        recLabel.setText(text);
    }

    public void updateTempValue(double value) {
        tempLabel.setText(String.format("%.2f", value));
    }

    public void addChartPoint(double time, double value) {
        if (series.getItemCount() > MAX_POINTS) series.remove(0);
        series.add(time, value);
    }

    public void setChartBounds(double lowerBound, double upperBound) {
        XYPlot plot = chart.getXYPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLowerBound(lowerBound);
        rangeAxis.setUpperBound(upperBound);
    }

    public void resetCaptureUI() {
        SwingUtilities.invokeLater(() -> {
            startStopButton.setText("Iniciar toma de datos");
            setInfoFieldsEnabled(true);
        });
    }

    public void cargarClientes(List<Cliente> clientes) {
        cmbCliente.removeAllItems();
        for (Cliente cliente : clientes) {
            cmbCliente.addItem(cliente);
        }
    }

    public String getSelectedPressureUnit() {
        return selectedPressureUnit;
    }

    public Operador getSelectedOperador() {
        return (Operador) cmbOperador.getSelectedItem();
    }

    public Fluido getSelectedFluido() {
        return (Fluido) cmbFluido.getSelectedItem();
    }

}