package com.mediciones.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.LineBorder;

import com.mediciones.gestor.FrmInicioGestor;
import com.mediciones.view.components.Button3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrmInicio extends JFrame implements ActionListener {

    private final FrmInicioGestor controller;

    private JTextArea txtMotorola;
    private JTextArea txtEndressHauser;
    private JRadioButton rbPSIG;
    private JRadioButton rbKgCm2;
    private JRadioButton rbLbIn2;
    private ButtonGroup unidadGroup;
    private JComboBox<String> cbPuertos;

    private Button3D btnClientes;
    private Button3D btnOperador;
    private Button3D btnValvulas;
    private Button3D btnFluidos;
    private Button3D btnPlantas;
    private Button3D btnTiposValvula; // NUEVO: Declarado aquí
    private Button3D btnCalibracion;
    private Button3D btnMedicion;
    private Button3D btnSalir;
    private Button3D btnReconectar;
    private Button3D btnConfiguracion;

    private JLabel lblIndicadorComunicacion;
    private JLabel lblComunicacion;

    private static final int BASE_WIDTH = 850;
    private static final int BASE_HEIGHT = 550;
    private JPanel scalablePanel;
    private Map<Component, Rectangle> originalBounds = new HashMap<>();
    private static final float BASE_FONT_SENSOR = 30f;
    private Map<Component, Font> originalFonts = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(FrmInicio.class);


    public FrmInicio() {
        this.controller = new FrmInicioGestor(this);

        setTitle("Sistema de Documentación de Banco de Pruebas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        initComponents();
        controller.cargarConstantesCalibracion();

        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setMinimumSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        pack();

        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500);

                if (validarSiExisteConfiguracion()) {
                    iniciarComunicacionSerial();
                }
            } catch (Exception e) {
                logger.error("Error al iniciar la aplicación", e);
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                adjustScale();
            }
        });
    }

    private boolean validarSiExisteConfiguracion() {
        if (!controller.existeConfiguracion()) {
            FrmConfiguracion frmConfig = new FrmConfiguracion();
            frmConfig.setLocationRelativeTo(this);

            frmConfig.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    controller.cargarConstantesCalibracion();
                    iniciarComunicacionSerial();
                }
            });

            frmConfig.setVisible(true);
            return false;
        }
        return true;
    }

    private void iniciarComunicacionSerial() {
        controller.iniciarComunicacionSerial(
                (String) cbPuertos.getSelectedItem(),
                rbPSIG.isSelected(),
                rbLbIn2.isSelected(),
                val -> actualizarTextoMotorola(val),
                val -> actualizarTextoEndress(val)
        );
    }

    private void actualizarTextoMotorola(double val) {
        String unidad = rbPSIG.isSelected() ? "PSIG" : rbLbIn2.isSelected() ? "Barg" : "kg/cm²";
        txtMotorola.setText(String.format("\n  %.2f %s", Double.valueOf(val), unidad));
    }

    private void actualizarTextoEndress(double val) {
        String unidad = rbPSIG.isSelected() ? "PSIG" : rbLbIn2.isSelected() ? "Barg" : "kg/cm²";
        txtEndressHauser.setText(String.format("\n  %.2f %s", Double.valueOf(val), unidad));
    }

    private void initComponents() {
        scalablePanel = new JPanel(null);
        scalablePanel.setBackground(Color.WHITE);
        scalablePanel.setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));

        JLabel titleLabel = new JLabel("Sistema de documentacion de banco de pruebas", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(0, 10, BASE_WIDTH, 40);
        scalablePanel.add(titleLabel);
        originalBounds.put(titleLabel, titleLabel.getBounds());

        int startXTop = (BASE_WIDTH - 660) / 2;

        JLabel lblM = new JLabel("Sensor Motorola (0-7 PSIG)");
        lblM.setBounds(startXTop, 70, 230, 20);
        scalablePanel.add(lblM);
        originalBounds.put(lblM, lblM.getBounds());

        txtMotorola = new JTextArea();
        txtMotorola.setEditable(false);
        txtMotorola.setBackground(Color.BLUE.darker().darker());
        txtMotorola.setForeground(Color.WHITE);
        txtMotorola.setFont(new Font("Arial", Font.BOLD, (int) BASE_FONT_SENSOR));
        txtMotorola.setBounds(startXTop, 90, 230, 100);
        scalablePanel.add(txtMotorola);
        originalBounds.put(txtMotorola, txtMotorola.getBounds());

        JLabel lblE = new JLabel("Sensor Endress-Hauser (0-7 PSIG)");
        lblE.setBounds(startXTop + 280, 70, 230, 20);
        scalablePanel.add(lblE);
        originalBounds.put(lblE, lblE.getBounds());

        txtEndressHauser = new JTextArea();
        txtEndressHauser.setEditable(false);
        txtEndressHauser.setBackground(Color.BLUE.darker().darker());
        txtEndressHauser.setForeground(Color.WHITE);
        txtEndressHauser.setFont(new Font("Arial", Font.BOLD, (int) BASE_FONT_SENSOR));
        txtEndressHauser.setBounds(startXTop + 280, 90, 230, 100);
        scalablePanel.add(txtEndressHauser);
        originalBounds.put(txtEndressHauser, txtEndressHauser.getBounds());

        JPanel unidadPanel = new JPanel();
        unidadPanel.setLayout(new BoxLayout(unidadPanel, BoxLayout.Y_AXIS));
        unidadPanel.setBorder(LineBorder.createBlackLineBorder());
        unidadPanel.setBounds(startXTop + 560, 90, 100, 100);
        rbPSIG = new JRadioButton("PSIG");
        rbKgCm2 = new JRadioButton("Kg/cm²");
        rbLbIn2 = new JRadioButton("Barg");
        rbKgCm2.setSelected(true);
        unidadGroup = new ButtonGroup();
        unidadGroup.add(rbPSIG);
        unidadGroup.add(rbKgCm2);
        unidadGroup.add(rbLbIn2);
        unidadPanel.add(rbPSIG);
        unidadPanel.add(rbKgCm2);
        unidadPanel.add(rbLbIn2);
        scalablePanel.add(unidadPanel);
        originalBounds.put(unidadPanel, unidadPanel.getBounds());

        int startXBtn = (BASE_WIDTH - 680) / 2;

        // --- FILA 1 ---
        btnClientes = new Button3D("Clientes", new Color(200, 255, 200), true);
        btnClientes.setBounds(startXBtn, 230, 120, 40);
        btnOperador = new Button3D("Operador", new Color(200, 255, 200), true);
        btnOperador.setBounds(startXBtn + 140, 230, 120, 40);
        btnPlantas = new Button3D("Plantas", new Color(200, 255, 200), true);
        btnPlantas.setBounds(startXBtn + 280, 230, 120, 40);
        btnMedicion = new Button3D("Medicion", Color.GREEN.brighter(), true);
        btnMedicion.setBounds(startXBtn + 420, 230, 120, 40);
        btnReconectar = new Button3D("Refrescar", new Color(200, 220, 255), true);
        btnReconectar.setBounds(startXBtn + 560, 230, 120, 40);

        // --- FILA 2 ---
        btnValvulas = new Button3D("Valvulas", new Color(200, 255, 200), true);
        btnValvulas.setBounds(startXBtn, 290, 120, 40);
        btnFluidos = new Button3D("Fluidos", new Color(200, 255, 200), true);
        btnFluidos.setBounds(startXBtn + 140, 290, 120, 40);
        btnTiposValvula = new Button3D("Tipos Válvula", new Color(200, 255, 200), true);
        btnTiposValvula.setBounds(startXBtn + 280, 290, 120, 40);
        btnConfiguracion = new Button3D("Configuración", new Color(255, 255, 200), true);
        btnConfiguracion.setBounds(startXBtn + 420, 290, 120, 40);
        btnCalibracion = new Button3D("Calibrar", Color.YELLOW, true);
        btnCalibracion.setBounds(startXBtn + 560, 290, 120, 40);

        // --- FILA 3 ---
        btnSalir = new Button3D("Salir", new Color(255, 200, 200), true);
        btnSalir.setBounds((BASE_WIDTH - 120) / 2, 350, 120, 40);

        scalablePanel.add(btnClientes);
        scalablePanel.add(btnOperador);
        scalablePanel.add(btnPlantas);
        scalablePanel.add(btnMedicion);
        scalablePanel.add(btnReconectar);
        scalablePanel.add(btnValvulas);
        scalablePanel.add(btnFluidos);
        scalablePanel.add(btnTiposValvula);
        scalablePanel.add(btnConfiguracion);
        scalablePanel.add(btnCalibracion);
        scalablePanel.add(btnSalir);

        originalBounds.put(btnClientes, btnClientes.getBounds());
        originalBounds.put(btnOperador, btnOperador.getBounds());
        originalBounds.put(btnPlantas, btnPlantas.getBounds());
        originalBounds.put(btnMedicion, btnMedicion.getBounds());
        originalBounds.put(btnReconectar, btnReconectar.getBounds());
        originalBounds.put(btnValvulas, btnValvulas.getBounds());
        originalBounds.put(btnFluidos, btnFluidos.getBounds());
        originalBounds.put(btnTiposValvula, btnTiposValvula.getBounds());
        originalBounds.put(btnConfiguracion, btnConfiguracion.getBounds());
        originalBounds.put(btnCalibracion, btnCalibracion.getBounds());
        originalBounds.put(btnSalir, btnSalir.getBounds());

        JPanel comPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        comPanel.setBounds(BASE_WIDTH / 2 - 140, 400, 280, 35);
        comPanel.setBorder(LineBorder.createBlackLineBorder());
        lblIndicadorComunicacion = new JLabel(" ");
        lblIndicadorComunicacion.setPreferredSize(new Dimension(15, 15));
        lblIndicadorComunicacion.setOpaque(true);
        lblIndicadorComunicacion.setBackground(Color.RED);
        cbPuertos = new JComboBox<>();
        controller.actualizarListaPuertos(cbPuertos);

        cbPuertos.addActionListener(e -> {
            controller.detenerComunicacionSerial();
            iniciarComunicacionSerial();
        });

        comPanel.add(lblIndicadorComunicacion);
        comPanel.add(new JLabel("Puerto:"));
        comPanel.add(cbPuertos);
        scalablePanel.add(comPanel);
        originalBounds.put(comPanel, comPanel.getBounds());

        // Agregar listeners a los botones
        btnClientes.addActionListener(this);
        btnOperador.addActionListener(this);
        btnValvulas.addActionListener(this);
        btnFluidos.addActionListener(this);
        btnPlantas.addActionListener(this);
        btnTiposValvula.addActionListener(this);
        btnConfiguracion.addActionListener(this);
        btnCalibracion.addActionListener(this);
        btnMedicion.addActionListener(this);
        btnSalir.addActionListener(e -> {
            controller.detenerComunicacionSerial();
            System.exit(0);
        });
        btnReconectar.addActionListener(e -> {
            controller.detenerComunicacionSerial();
            iniciarComunicacionSerial();
        });

        // deshabilito botones (temporal si así lo tenías)
        btnClientes.setEnabled(true);
        btnValvulas.setEnabled(true);
        btnOperador.setEnabled(true);

        getContentPane().add(scalablePanel);
        collectFonts(getContentPane());
        adjustScale();
    }

    private void adjustScale() {
        double scale = Math.min((double) getWidth() / BASE_WIDTH, (double) getHeight() / BASE_HEIGHT);
        int offsetX = (getWidth() - (int) (BASE_WIDTH * scale)) / 2;
        int offsetY = (getHeight() - (int) (BASE_HEIGHT * scale)) / 2;
        for (Map.Entry<Component, Rectangle> entry : originalBounds.entrySet()) {
            Rectangle r = entry.getValue();
            entry.getKey().setBounds((int) (r.x * scale) + offsetX, (int) (r.y * scale) + offsetY, (int) (r.width * scale), (int) (r.height * scale));
        }
        adjustFonts(scale);
        revalidate();
        repaint();
    }

    private void adjustFonts(double scale) {
        for (Map.Entry<Component, Font> entry : originalFonts.entrySet()) {
            entry.getKey().setFont(entry.getValue().deriveFont(Math.max(8f, entry.getValue().getSize2D() * (float) scale)));
        }
    }

    private void collectFonts(Container root) {
        for (Component comp : root.getComponents()) {
            if (comp.getFont() != null) originalFonts.put(comp, comp.getFont());
            if (comp instanceof Container) collectFonts((Container) comp);
        }
    }

    public void setEstadoComunicacion(boolean conectado) {
        if (lblIndicadorComunicacion != null)
            lblIndicadorComunicacion.setBackground(conectado ? Color.GREEN : Color.RED);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handleButtonAction(e.getSource());
    }

    private void handleButtonAction(Object source) {
        JButton button = (JButton) source;
        button.setEnabled(false); // Deshabilitar el botón inmediatamente

        // Usar un Timer para reactivar el botón después de 1 segundo
        Timer timer = new Timer(1000, evt -> button.setEnabled(true));
        timer.setRepeats(false);
        timer.start();

        controller.detenerComunicacionSerial();
        Window f = null;
        if (source == btnClientes) f = new FrmClienteCRUD();
        else if (source == btnOperador) f = new FrmOperadorCRUD();
        else if (source == btnValvulas) f = new FrmValvulasCRUD();
        else if (source == btnFluidos) f = new FrmFluidosCRUD();
        else if (source == btnPlantas) f = new FrmPlantaCRUD();
        else if (source == btnTiposValvula) f = new FrmTipoValvulaCRUD();
        else if (source == btnConfiguracion) f = new FrmConfiguracion();
        else if (source == btnCalibracion) f = new FrmCalibracionSensor();
        else if (source == btnMedicion) {
            RealTimeGraph graph = RealTimeGraph.getInstance();
            graph.setUnidadSeleccionada(getUnidadSeleccionada());
            f = graph;
        }

        if (f != null) {
            f.setLocationRelativeTo(this);
            f.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    controller.cargarConstantesCalibracion();
                    iniciarComunicacionSerial();
                }
            });
            f.setVisible(true);
        }
    }

    public String getUnidadSeleccionada() {
        if (rbKgCm2.isSelected()) return "Kg/cm²";
        if (rbLbIn2.isSelected()) return "Barg";
        return "PSIG";
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("Error al configurar el Look and Feel", e);
        }

        SwingUtilities.invokeLater(() -> {
            FrmInicio frame = new FrmInicio();
            frame.setVisible(true);
        });
    }
}