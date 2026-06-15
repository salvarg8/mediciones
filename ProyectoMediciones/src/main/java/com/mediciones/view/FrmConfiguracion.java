package com.mediciones.view;

import com.mediciones.controller.ConfiguracionController;
import com.mediciones.model.Configuracion;
import com.mediciones.view.components.Button3D;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class FrmConfiguracion extends JFrame {

    // Labels
    private JLabel lblOrigen;
    private JLabel lblRuta;

    // Radio buttons
    private JRadioButton rbBaseDatos;
    private JRadioButton rbArchivoTxt;
    private ButtonGroup grupoOrigen;

    // Ruta
    private JTextField txtRutaArchivo;

    // Botones
    private Button3D btnBuscar;
    private Button3D btnGuardar;
    private Button3D btnCancelar;

    // Controller
    private final ConfiguracionController controller;

    public FrmConfiguracion() {

        super("Configuración");

        controller = new ConfiguracionController();

        // Evitar que la ventana se cierre automáticamente al darle a la 'X'
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setSize(600, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();

        cargarConfiguracion();

        // Interceptar el evento de cierre de la ventana ('X')
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                intentarCerrar();
            }
        });
    }

    private void initComponents() {

        setLayout(new BorderLayout());

        JPanel panelPrincipal = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font font = new Font("Segoe UI", Font.BOLD, 14);

        // ============================================
        // ORIGEN DE DATOS
        // ============================================

        lblOrigen = new JLabel("Origen de datos:");
        lblOrigen.setFont(font);

        rbBaseDatos = new JRadioButton("Base de Datos");
        rbBaseDatos.setFont(font);
        rbBaseDatos.setOpaque(false);

        rbArchivoTxt = new JRadioButton("Archivo TXT");
        rbArchivoTxt.setFont(font);
        rbArchivoTxt.setOpaque(false);

        grupoOrigen = new ButtonGroup();
        grupoOrigen.add(rbBaseDatos);
        grupoOrigen.add(rbArchivoTxt);

        JPanel panelOrigen = new JPanel();
        panelOrigen.setLayout(new BoxLayout(panelOrigen, BoxLayout.Y_AXIS));
        panelOrigen.setOpaque(false);

        panelOrigen.add(rbBaseDatos);
        panelOrigen.add(Box.createVerticalStrut(5));
        panelOrigen.add(rbArchivoTxt);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        panelPrincipal.add(lblOrigen, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;

        panelPrincipal.add(panelOrigen, gbc);

        // ============================================
        // RUTA DEL ARCHIVO
        // ============================================

        lblRuta = new JLabel("Ruta Portal:");
        lblRuta.setFont(font);

        txtRutaArchivo = new JTextField(35);
        txtRutaArchivo.setFont(font);

        btnBuscar = new Button3D("...", new Color(220, 220, 255));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panelPrincipal.add(lblRuta, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;

        panelPrincipal.add(txtRutaArchivo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        panelPrincipal.add(btnBuscar, gbc);

        // ============================================
        // BOTONES
        // ============================================

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));

        btnGuardar = new Button3D("Guardar", new Color(200, 255, 200));
        btnCancelar = new Button3D("Cancelar", new Color(255, 200, 200));

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        add(panelPrincipal, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        // ============================================
        // EVENTOS
        // ============================================

        rbBaseDatos.addActionListener(e -> actualizarEstadoControles());
        rbArchivoTxt.addActionListener(e -> actualizarEstadoControles());

        btnBuscar.addActionListener(this::buscarArchivo);
        btnGuardar.addActionListener(this::guardarConfiguracion);

        // El botón cancelar ahora llama a la validación en lugar de hacer dispose() directo
        btnCancelar.addActionListener(e -> intentarCerrar());
    }

    /**
     * Valida si existe una configuración antes de permitir cerrar el formulario.
     */
    private void intentarCerrar() {
        if (!controller.existeConfiguracion()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Debe guardar una configuración inicial antes de salir del formulario.",
                    "Configuración Requerida",
                    JOptionPane.WARNING_MESSAGE
            );
        } else {
            dispose();
        }
    }

    private void cargarConfiguracion() {

        try {

            Configuracion configuracion = controller.obtenerConfiguracion();

            if (configuracion == null) {
                rbBaseDatos.setSelected(true);
                txtRutaArchivo.setText("");
            } else {
                if ("TXT".equalsIgnoreCase(configuracion.getOrigenDatos())) {
                    rbArchivoTxt.setSelected(true);
                } else {
                    rbBaseDatos.setSelected(true);
                }

                txtRutaArchivo.setText(
                        configuracion.getRutaArchivo() != null
                                ? configuracion.getRutaArchivo()
                                : ""
                );
            }

            actualizarEstadoControles();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error al cargar la configuración.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            ex.printStackTrace();
        }
    }

    private void guardarConfiguracion(ActionEvent e) {

        String origen;
        String ruta = txtRutaArchivo.getText().trim();

        if (rbBaseDatos.isSelected()) {
            origen = "BD";
        } else {
            origen = "TXT";

            if (ruta.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Debe seleccionar el archivo Portal.",
                        "Error de validación",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            File archivo = new File(ruta);

            if (!archivo.exists()) {
                JOptionPane.showMessageDialog(
                        this,
                        "El archivo seleccionado no existe.",
                        "Error de validación",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if (!archivo.isFile()) {
                JOptionPane.showMessageDialog(
                        this,
                        "La ruta seleccionada no corresponde a un archivo.",
                        "Error de validación",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        Configuracion configuracion = new Configuracion();
        configuracion.setId(1);
        configuracion.setOrigenDatos(origen);
        configuracion.setRutaArchivo(ruta);

        boolean ok = controller.guardarConfiguracion(configuracion);

        if (ok) {
            JOptionPane.showMessageDialog(
                    this,
                    "Configuración guardada correctamente.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "No fue posible guardar la configuración.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void buscarArchivo(ActionEvent e) {

        JFileChooser fileChooser;

        // Si ya hay una ruta cargada, abrir en esa carpeta
        String rutaActual = txtRutaArchivo.getText().trim();

        if (!rutaActual.isEmpty()) {
            File archivoActual = new File(rutaActual);
            if (archivoActual.exists()) {
                fileChooser = new JFileChooser(archivoActual.getParentFile());
            } else {
                fileChooser = new JFileChooser();
            }
        } else {
            fileChooser = new JFileChooser();
        }

        fileChooser.setDialogTitle("Seleccionar archivo Portal");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Archivos de texto (*.txt)",
                        "txt"
                )
        );

        int resultado = fileChooser.showOpenDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = fileChooser.getSelectedFile();
            txtRutaArchivo.setText(archivoSeleccionado.getAbsolutePath());
        }
    }

    private void actualizarEstadoControles() {
        boolean usarTxt = rbArchivoTxt.isSelected();
        txtRutaArchivo.setEnabled(usarTxt);
        btnBuscar.setEnabled(usarTxt);
    }
}