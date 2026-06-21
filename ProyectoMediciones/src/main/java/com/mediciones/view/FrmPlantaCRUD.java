package com.mediciones.view;

import com.mediciones.gestor.ClienteGestor;
import com.mediciones.gestor.PlantaGestor;
import com.mediciones.model.Planta;
import com.mediciones.model.Cliente;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario para la gestión de Plantas (CRUD).
 * Extiende JFrame manteniendo el escalado proporcional y diseño idéntico a FrmOperadorCRUD.
 */
public class FrmPlantaCRUD extends JFrame {

    private JLabel lblNombre;
    private JLabel lblCliente;
    private TitledBorder formBorder;
    private TitledBorder tableBorder;

    // Componentes de la UI para el formulario de entrada
    private JTextField txtNombre;
    private JComboBox<Cliente> cmbCliente;
    private Button3D btnGuardar;
    private Button3D btnCancelar;

    // Componentes para la tabla de visualización
    private JTable tblPlantas;
    private DefaultTableModel tableModel;
    private Button3D btnActualizar;
    private Button3D btnEditar;
    private Button3D btnEliminar;

    private final PlantaGestor gestor;
    private final ClienteGestor clienteGestor; // Agregado para separar responsabilidades
    private Planta plantaSeleccionada;

    // Variables para manejo de escalado proporcional
    private int originalWidth = 600;
    private int originalHeight = 500;
    private JPanel contentPanel;

    private static final Logger logger = LoggerFactory.getLogger(FrmPlantaCRUD.class);

    /**
     * Constructor del formulario CRUD de Plantas.
     */
    public FrmPlantaCRUD() {
        super("Gestión de Plantas");

        setExtendedState(JFrame.MAXIMIZED_BOTH); // Estado inicial maximizado

        this.gestor = new PlantaGestor();
        this.clienteGestor = new ClienteGestor(); // Inicialización
        setResizable(true);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(600, 500));

        initComponents();
        cargarClientes(); // Carga el ComboBox de clientes externos
        cargarPlantas();  // Carga la JTable de plantas registradas

        // Establecer tamaño inicial
        setSize(originalWidth, originalHeight);
        setLocationRelativeTo(null);

        // Configurar tamaño máximo para permitir maximización
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = env.getMaximumWindowBounds();
        setMaximumSize(bounds.getSize());

        // Listener para manejar el escalado proporcional
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeProportionally();
            }
        });
    }

    private void resizeProportionally() {
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
        // Fuente escalada
        Font scaledFont = new Font("Segoe UI", Font.BOLD, (int)(14 * scaleFactor));

        // Ajustar fuente de las etiquetas y componentes
        lblNombre.setFont(scaledFont);
        lblCliente.setFont(scaledFont);
        txtNombre.setFont(scaledFont);
        cmbCliente.setFont(scaledFont);
        btnGuardar.setFont(scaledFont);
        btnCancelar.setFont(scaledFont);
        btnEditar.setFont(scaledFont);
        btnEliminar.setFont(scaledFont);
        btnActualizar.setFont(scaledFont);
        tblPlantas.setFont(scaledFont);
        tblPlantas.setRowHeight((int)(25 * scaleFactor));

        // Ajustar fuente de los títulos de los bordes
        formBorder.setTitleFont(scaledFont);
        ((JPanel) contentPanel.getComponent(0)).setBorder(formBorder);

        JScrollPane scrollPane = (JScrollPane) contentPanel.getComponent(1);
        tableBorder.setTitleFont(scaledFont);
        scrollPane.setBorder(tableBorder);

        // Ajustar tabla
        if (tblPlantas.getColumnModel().getColumnCount() > 0) {
            for (int i = 0; i < tblPlantas.getColumnModel().getColumnCount(); i++) {
                tblPlantas.getColumnModel().getColumn(i).setPreferredWidth((int)(100 * scaleFactor));
            }
        }

        // Actualizar layout
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        // Panel contenedor principal para controlar escalado
        contentPanel = new JPanel(new GridBagLayout());
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
        Font baseFont = new Font("Segoe UI", Font.BOLD, 14);

        // Título del formulario
        formBorder = BorderFactory.createTitledBorder("Nueva/Editar Planta");
        formBorder.setTitleFont(baseFont);
        formPanel.setBorder(formBorder);

        // Etiqueta y campo Nombre
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(6, 10, 6, 4);
        lblNombre = new JLabel("Nombre:");
        lblNombre.setFont(baseFont);
        formPanel.add(lblNombre, gbc);
        txtNombre = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.insets = new Insets(6, 4, 6, 10);
        gbc.weightx = 1.0;
        formPanel.add(txtNombre, gbc);

        // Etiqueta y combobox Cliente (relación de la entidad)
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(6, 10, 6, 4);
        gbc.weightx = 0.0;
        lblCliente = new JLabel("Cliente:");
        lblCliente.setFont(baseFont);
        formPanel.add(lblCliente, gbc);
        cmbCliente = new JComboBox<>();
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.insets = new Insets(6, 4, 6, 10);
        gbc.weightx = 1.0;
        formPanel.add(cmbCliente, gbc);

        // Panel de Botones del Formulario
        JPanel formButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnGuardar = new Button3D("Guardar nueva", new Color(200, 255, 200)); // verde claro
        btnCancelar = new Button3D("Salir", new Color(255, 200, 200)); // rosa claro

        formButtonPanel.add(btnGuardar);
        formButtonPanel.add(btnCancelar);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        formPanel.add(formButtonPanel, gbc);

        // Agregar el formulario al contentPanel
        gbcContent.gridx = 0;
        gbcContent.gridy = 0;
        gbcContent.weightx = 1.0;
        gbcContent.weighty = 0.0;
        contentPanel.add(formPanel, gbcContent);

        // --- 2. Panel de Visualización (Centro) ---
        String[] columnNames = {"ID", "Nombre Planta", "Cliente Asociado"};
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
        tblPlantas = new JTable(tableModel);
        tblPlantas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(tblPlantas);
        tableBorder = BorderFactory.createTitledBorder("Plantas Registradas");
        tableBorder.setTitleFont(baseFont);
        scrollPane.setBorder(tableBorder);

        // Agregar la tabla al contentPanel
        gbcContent.gridx = 0;
        gbcContent.gridy = 1;
        gbcContent.weightx = 1.0;
        gbcContent.weighty = 1.0; // Expandir verticalmente
        contentPanel.add(scrollPane, gbcContent);

        // --- 3. Panel de Control de la Tabla (Sur) ---
        btnEditar = new Button3D("Editar Seleccionada", new Color(255, 255, 200)); // amarillo claro
        btnEliminar = new Button3D("Eliminar Seleccionada", new Color(255, 200, 200)); // rosa claro
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
        gbcContent.weighty = 0.0;
        contentPanel.add(controlPanel, gbcContent);

        // --- Listeners ---
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);
        btnCancelar.addActionListener(e -> limpiarFormularioYCerrar());
        btnActualizar.addActionListener(e -> cargarPlantas());

        btnEditar.addActionListener(this::btnEditarActionPerformed);
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);

        tblPlantas.addMouseListener(new MouseAdapter() {
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
        if (cmbCliente.getItemCount() > 0) {
            cmbCliente.setSelectedIndex(0);
        }
        btnGuardar.setText("Guardar nueva");
        plantaSeleccionada = null;
        tblPlantas.clearSelection();
    }

    private void limpiarFormularioYCerrar() {
        limpiarFormulario();
        dispose();
    }

    /**
     * Llena el ComboBox con los clientes activos usando el ClienteGestor.
     */
    private void cargarClientes() {
        try {
            cmbCliente.removeAllItems();
            List<Cliente> clientes = clienteGestor.obtenerTodosClientes();
            for (Cliente cliente : clientes) {
                cmbCliente.addItem(cliente);
            }
        } catch (Exception ex) {
            logger.error("Error al cargar los clientes en el combobox", ex);
        }
    }

    /**
     * Carga TODAS las plantas activas en la tabla general.
     */
    private void cargarPlantas() {
        try {
            tableModel.setRowCount(0);

            List<Planta> plantas = gestor.obtenerTodasPlantas();

            for (Planta planta : plantas) {
                Object[] rowData = new Object[] {
                        planta.getId(),
                        planta.getNombre(),
                        planta.getCliente() != null ? planta.getCliente().getNombre() : "Sin Cliente"
                };
                tableModel.addRow(rowData);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar las plantas.",
                    "Error de Carga",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar las plantas", ex);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN GUARDAR (Crear/Actualizar)
    // --------------------------------------------------------------------

    private void btnGuardarActionPerformed(ActionEvent e) {
        String nombre = txtNombre.getText().trim();
        Cliente clienteSeleccionado = (Cliente) cmbCliente.getSelectedItem();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo Nombre de la Planta es obligatorio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (clienteSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Debe asociar la planta a un Cliente.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Planta plantaAGuardar;
        String mensajeExito;

        if (plantaSeleccionada == null) {
            plantaAGuardar = new Planta(null, nombre, new ArrayList<>(), clienteSeleccionado);
            mensajeExito = "Planta guardada exitosamente.";
        } else {
            plantaAGuardar = plantaSeleccionada;
            plantaAGuardar.setNombre(nombre);
            plantaAGuardar.setCliente(clienteSeleccionado);
            mensajeExito = "Planta actualizada exitosamente.";
        }

        try {
            boolean success = gestor.guardarOActualizarPlanta(plantaAGuardar);

            if (success) {
                JOptionPane.showMessageDialog(this, mensajeExito, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario(); // Limpiamos la UI luego del éxito
                cargarPlantas();     // Refrescamos la tabla para que se vea el cambio
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar/actualizar la planta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de sistema al guardar/actualizar la planta", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            logger.error("Error al guardar/actualizar la planta", ex);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN EDITAR (Cargar datos)
    // --------------------------------------------------------------------

    private void btnEditarActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblPlantas.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idPlanta = (int) tblPlantas.getValueAt(filaSeleccionada, 0);

                plantaSeleccionada = gestor.obtenerPlantaPorId(idPlanta);

                if (plantaSeleccionada != null) {
                    txtNombre.setText(plantaSeleccionada.getNombre());

                    if (plantaSeleccionada.getCliente() != null) {
                        for (int i = 0; i < cmbCliente.getItemCount(); i++) {
                            Cliente item = cmbCliente.getItemAt(i);
                            if (item.getId() == (plantaSeleccionada.getCliente().getId())) {
                                cmbCliente.setSelectedIndex(i);
                                break;
                            }
                        }
                    }

                    btnGuardar.setText("Guardar Cambios (ID: " + idPlanta + ")");
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los datos para edición", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al cargar los datos para edición", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una planta de la lista para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --------------------------------------------------------------------
    // ACCIÓN ELIMINAR
    // --------------------------------------------------------------------

    private void btnEliminarActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblPlantas.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idPlanta = (int) tblPlantas.getValueAt(filaSeleccionada, 0);
                String nombrePlanta = (String) tblPlantas.getValueAt(filaSeleccionada, 1);

                // --- LÓGICA DE ADVERTENCIA DINÁMICA ---
                int cantidadValvulas = gestor.contarValvulasAsociadas(idPlanta);
                String mensaje;

                if (cantidadValvulas > 0) {
                    mensaje = "⚠️ ATENCIÓN: La planta '" + nombrePlanta + "' tiene " + cantidadValvulas +
                            " válvula(s) activa(s) asociada(s).\n\nSi continúa, se eliminará la planta Y TODAS sus válvulas en cascada.\n\n¿Desea continuar?";
                } else {
                    mensaje = "¿Está seguro de que desea eliminar la planta: " + nombrePlanta + " (ID: " + idPlanta + ")?";
                }

                int confirmacion = JOptionPane.showConfirmDialog(
                        this, mensaje, "Confirmar Eliminación",
                        JOptionPane.YES_NO_OPTION,
                        cantidadValvulas > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE
                );

                if (confirmacion == JOptionPane.YES_OPTION) {
                    if (gestor.eliminarPlanta(idPlanta)) {
                        JOptionPane.showMessageDialog(this, "Planta eliminada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        limpiarFormulario();
                        cargarPlantas();
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: No se pudo eliminar la planta.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al obtener el ID de la planta para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al obtener el ID de la planta para eliminar", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una planta de la lista para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
}