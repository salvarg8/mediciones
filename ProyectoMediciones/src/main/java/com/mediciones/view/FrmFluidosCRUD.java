package com.mediciones.view;

import com.mediciones.gestor.FluidoGestor;
import com.mediciones.model.Fluido;
import com.mediciones.view.components.Button3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Formulario para la gestión de Fluidos (CRUD).
 * Extiende JFrame para ser usado como ventana modal, compatible con la llamada de FrmInicio.
 */
public class FrmFluidosCRUD extends JFrame {

    private JLabel lblNombre;
    private TitledBorder formBorder;
    private TitledBorder tableBorder;

    // Componentes de la UI para el formulario de entrada
    private JTextField txtNombre;
    private Button3D btnGuardar; 
    private Button3D btnCancelar; 

    // Componentes para la tabla de visualización
    private JTable tblFluidos;
    private DefaultTableModel tableModel;
    private Button3D btnActualizar; 
    private Button3D btnEditar; 
    private Button3D btnEliminar; 

    private final FluidoGestor controller;
    private Fluido fluidoSeleccionado;

    // Variables para manejo de escalado proporcional
    private int originalWidth = 600;
    private int originalHeight = 500;
    private JPanel contentPanel; // Panel contenedor para aplicar escalado

    private static final Logger logger = LoggerFactory.getLogger(FrmFluidosCRUD.class);


    /**
     * Constructor del formulario CRUD de Fluidos.
     */
    public FrmFluidosCRUD() {
        super("Gestión de Fluidos");

        setExtendedState(JFrame.MAXIMIZED_BOTH); // estado inicial

        this.controller = new FluidoGestor();
        setResizable(true); // Permitir redimensionamiento

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setMinimumSize(new Dimension(600, 500)); // Tamaño mínimo inicial

        initComponents();
        cargarFluidos();

        // Establecer tamaño inicial
        setSize(originalWidth, originalHeight);
        setLocationRelativeTo(null);

        // Configurar tamaño máximo para permitir maximización
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = env.getMaximumWindowBounds(); // Obtiene el tamaño máximo de la pantalla
        setMaximumSize(bounds.getSize()); // Permite que el formulario ocupe toda la pantalla

        // Listener para manejar el escalado proporcional
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeProportionally();
            }
        });
    }

    private void resizeProportionally() {
        int currentWidth = getWidth();
        int currentHeight = getHeight();

        // Calcular factor de escala basado en altura (vertical)
        double scaleFactor = (double) currentHeight / originalHeight;

        // Ajustar panel interno con layout proporcional
        adjustLayout(scaleFactor);

        // Forzar revalidación
        revalidate();
        repaint();
    }

    private void adjustLayout(double scaleFactor) {
        // Ajustar insets y gaps proporcionalmente
        int scaledInset = (int) (8 * scaleFactor); // Base inset
        int scaledGap = (int) (5 * scaleFactor);   // Base gap

        // Fuente escalada
        Font scaledFont = new Font("Segoe UI", Font.BOLD, (int)(14 * scaleFactor));

        // Ajustar fuente de las etiquetas
        lblNombre.setFont(scaledFont);
        txtNombre.setFont(scaledFont);
        btnGuardar.setFont(scaledFont);
        btnCancelar.setFont(scaledFont);
        btnEditar.setFont(scaledFont);
        btnEliminar.setFont(scaledFont);
        btnActualizar.setFont(scaledFont);
        tblFluidos.setFont(scaledFont);
        tblFluidos.setRowHeight((int)(25 * scaleFactor));

        // Ajustar fuente de los títulos de los bordes
        formBorder.setTitleFont(scaledFont);
        ((JPanel) contentPanel.getComponent(0)).setBorder(formBorder); // Reasignar el borde

        JScrollPane scrollPane = (JScrollPane) contentPanel.getComponent(1);
        tableBorder.setTitleFont(scaledFont);
        scrollPane.setBorder(tableBorder); // Reasignar el borde

        // Ajustar tabla
        if (tblFluidos.getColumnModel().getColumnCount() > 0) {
            for (int i = 0; i < tblFluidos.getColumnModel().getColumnCount(); i++) {
                tblFluidos.getColumnModel().getColumn(i).setPreferredWidth((int)(100 * scaleFactor));
            }
        }

        // Actualizar layout
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        // Panel contenedor principal para controlar escalado
        contentPanel = new JPanel(new GridBagLayout()); // Cambiar a GridBagLayout
        GridBagConstraints gbcContent = new GridBagConstraints();
        gbcContent.insets = new Insets(10, 10, 10, 10);
        gbcContent.fill = GridBagConstraints.BOTH;
        add(contentPanel, BorderLayout.CENTER);

        // --- 1. Panel de Formulario (Norte) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fuente base para los títulos y etiquetas
        Font baseFont = new Font("Segoe UI", Font.BOLD, 14); // Fuente inicial

        // Título del formulario
        formBorder = BorderFactory.createTitledBorder("Nuevo/Editar Fluido");
        formBorder.setTitleFont(baseFont);
        formPanel.setBorder(formBorder);

        // Etiqueta y campo Nombre (sin ":")
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(6, 10, 6, 4);
        lblNombre = new JLabel("Nombre:"); // Guardar referencia
        lblNombre.setFont(baseFont);
        formPanel.add(lblNombre, gbc);
        txtNombre = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.insets = new Insets(6, 4, 6, 10);
        gbc.weightx = 1.0;
        formPanel.add(txtNombre, gbc);

        // Panel de Botones del Formulario
        JPanel formButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Alineación centrada
        btnGuardar = new Button3D("Guardar nuevo", new Color(200, 255, 200)); // verde claro
        btnCancelar = new Button3D("Salir", new Color(255, 200, 200)); // rosa claro

        formButtonPanel.add(btnGuardar);
        formButtonPanel.add(btnCancelar);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        formPanel.add(formButtonPanel, gbc);

        // Agregar el formulario al contentPanel
        gbcContent.gridx = 0;
        gbcContent.gridy = 0;
        gbcContent.weightx = 1.0;
        gbcContent.weighty = 0.0; // No expandir verticalmente
        contentPanel.add(formPanel, gbcContent);

        // --- 2. Panel de Visualización (Centro) ---
        String[] columnNames = {"ID", "Nombre"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        tblFluidos = new JTable(tableModel);
        tblFluidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(tblFluidos);
        tableBorder = BorderFactory.createTitledBorder("Fluidos Registrados");
        tableBorder.setTitleFont(baseFont);
        scrollPane.setBorder(tableBorder);

        // Agregar la tabla al contentPanel
        gbcContent.gridx = 0;
        gbcContent.gridy = 1;
        gbcContent.weightx = 1.0;
        gbcContent.weighty = 1.0; // Expandir verticalmente
        contentPanel.add(scrollPane, gbcContent);

        // --- 3. Panel de Control de la Tabla (Sur) ---
        btnEditar = new Button3D("Editar Seleccionado", new Color(255, 255, 200)); // amarillo claro
        btnEliminar = new Button3D("Eliminar Seleccionado", new Color(255, 200, 200)); // rosa claro
        btnActualizar = new Button3D("Actualizar Lista", new Color(200, 255, 200)); // verde claro

        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcControl = new GridBagConstraints();

        gbcControl.fill = GridBagConstraints.BOTH;
        gbcControl.weightx = 1.0;
        gbcControl.weighty = 1.0;
        gbcControl.insets = new Insets(5, 10, 5, 10);
        gbcControl.gridy = 0;

        gbcControl.gridx = 0;
        controlPanel.add(btnEditar, gbcControl);

        gbcControl.gridx = 1;
        controlPanel.add(btnEliminar, gbcControl);

        gbcControl.gridx = 2;
        controlPanel.add(btnActualizar, gbcControl);

        // Agregar el panel de botones al contentPanel
        gbcContent.gridx = 0;
        gbcContent.gridy = 2;
        gbcContent.weightx = 1.0;
        gbcContent.weighty = 0.0; // No expandir demasiado verticalmente
        contentPanel.add(controlPanel, gbcContent);

        // --- Listeners ---
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);
        btnCancelar.addActionListener(e -> limpiarFormularioYCerrar());
        btnActualizar.addActionListener(e -> cargarFluidos());

        btnEditar.addActionListener(this::btnEditarActionPerformed);
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);

        tblFluidos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnEditarActionPerformed(null);
                }
            }
        });
    }

    // --------------------------------------------------------------------
    // MÉTODOS DE SOPORTE Y LÓGICA DE LA VISTA
    // --------------------------------------------------------------------

    private void limpiarFormulario() {
        txtNombre.setText("");
        btnGuardar.setText("Guardar");
        fluidoSeleccionado = null;
        tblFluidos.clearSelection();
    }

    private void limpiarFormularioYCerrar() {
        limpiarFormulario();
        dispose();
    }

    private void cargarFluidos() {
        try {
            tableModel.setRowCount(0);
            List<Fluido> fluidos = controller.obtenerTodosFluidos();

            for (Fluido fluido : fluidos) {
                Object[] rowData = new Object[] {
                        fluido.getId(),
                        fluido.getNombre()
                };
                tableModel.addRow(rowData);
            }
            limpiarFormulario();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar los fluidos" +
                            "\nAsegúrese de que FluidoController y la BD estén funcionales.",
                    "Error de Carga",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar los fluidos", ex);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN GUARDAR (Crear/Actualizar)
    // --------------------------------------------------------------------

    private void btnGuardarActionPerformed(ActionEvent e) {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo Nombre es obligatorio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Fluido fluidoAGuardar;
        String mensajeExito;

        if (fluidoSeleccionado == null) {
            fluidoAGuardar = new Fluido(nombre);
            mensajeExito = "Fluido guardado exitosamente.";
        } else {
            fluidoAGuardar = fluidoSeleccionado;
            fluidoAGuardar.setNombre(nombre);
            mensajeExito = "Fluido actualizado exitosamente.";
        }

        try {
            boolean success = controller.guardarOActualizarFluido(fluidoAGuardar);

            if (success) {
                JOptionPane.showMessageDialog(this, mensajeExito, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarFluidos();
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar/actualizar el fluido (Controller retornó false).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de sistema al guardar/actualizar", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            logger.error("Error de sistema al guardar/actualizar", ex);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN EDITAR (Cargar datos)
    // --------------------------------------------------------------------

    private void btnEditarActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblFluidos.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idFluido = (int) tblFluidos.getValueAt(filaSeleccionada, 0);
                String nombre = (String) tblFluidos.getValueAt(filaSeleccionada, 1);

                fluidoSeleccionado = new Fluido(idFluido, nombre);

                txtNombre.setText(nombre);
                btnGuardar.setText("Guardar Cambios (ID: " + idFluido + ")");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los datos para edición", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al cargar los datos para edición", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un fluido de la lista para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN ELIMINAR
    // --------------------------------------------------------------------

    private void btnEliminarActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblFluidos.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idFluido = (int) tblFluidos.getValueAt(filaSeleccionada, 0);
                String nombreFluido = (String) tblFluidos.getValueAt(filaSeleccionada, 1);

                int confirmacion = JOptionPane.showConfirmDialog(
                        this,
                        "¿Está seguro de que desea eliminar el fluido: " + nombreFluido + " (ID: " + idFluido + ")?",
                        "Confirmar Eliminación",
                        JOptionPane.YES_NO_OPTION
                    );

                if (confirmacion == JOptionPane.YES_OPTION) {
                    if (controller.eliminarFluido(idFluido)) {
                        JOptionPane.showMessageDialog(this, "Fluido eliminado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        limpiarFormulario();
                        cargarFluidos();
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar el fluido (Verifique si hay registros relacionados).", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al obtener el ID del fluido para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al obtener el ID del fluido para eliminar", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un fluido de la lista para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
}