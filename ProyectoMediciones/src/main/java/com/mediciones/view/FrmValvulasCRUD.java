package com.mediciones.view;

import com.mediciones.controller.ValvulaController;
import com.mediciones.model.Valvula;
import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.view.components.Button3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Formulario para la gestión de Válvulas (CRUD).
 * Extiende JFrame para ser usado como ventana modal, compatible con la llamada de FrmInicio.
 */
public class FrmValvulasCRUD extends JFrame {

    // Etiquetas
    private JLabel lblCliente;
    private JLabel lblFluidoServicio;
    private JLabel lblTag;
    private JLabel lblNroSerie;
    private JLabel lblLugarConexion;
    private JLabel lblMarca;
    private JLabel lblMaterialCuerpo;
    private JLabel lblEntradaRoscaTipo;
    private JLabel lblEntradaBridaDiametro;
    private JLabel lblEntradaBridaSerie;
    private JLabel lblSalidaRoscaTipo;
    private JLabel lblSalidaBridaDiametro;
    private JLabel lblSalidaBridaSerie;
    private JLabel lblBusquedaCliente;

    // Bordes
    private TitledBorder formBorder;
    private TitledBorder tableBorder;

    // Componentes de entrada
    private JComboBox<Cliente> cboCliente;
    private JComboBox<Fluido> cboFluidoServicio;
    private JComboBox<Cliente> cboBusquedaCliente; // Promoted to field
    private JTextField txtTag;
    private JTextField txtNroSerie;
    private JTextField txtLugarConexion;
    private JTextField txtMarca;
    private JTextField txtMaterialCuerpo;
    private JTextField txtEntradaRoscaTipo;
    private JTextField txtEntradaBridaDiametro;
    private JTextField txtEntradaBridaSerie;
    private JTextField txtSalidaRoscaTipo;
    private JTextField txtSalidaBridaDiametro;
    private JTextField txtSalidaBridaSerie;

    // Botones
    private Button3D btnGuardar;
    private Button3D btnSalir;
    private Button3D btnEditarSeleccionado;
    private Button3D btnEliminarSeleccionado;
    private Button3D btnActualizarLista;

    // Tabla y modelo
    private JTable tblValvulas;
    private DefaultTableModel tableModel;

    // Controlador y entidad seleccionada
    private final ValvulaController controller;
    private Valvula valvulaSeleccionada;

    // Paneles principales
    private JPanel topPanel;
    private JPanel camposGeneralesPanel;
    private JPanel entradaPanel;
    private JPanel salidaPanel;

    private static final Logger logger = LoggerFactory.getLogger(FrmValvulasCRUD.class);


    /**
     * Constructor del formulario CRUD de Válvulas.
     */
    public FrmValvulasCRUD() {
        super("Gestión de Válvulas");

        this.controller = new ValvulaController();
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));

        // Maximizar la ventana al iniciar
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        initComponents();
        cargarClientesYFluidos();
        cargarValvulas();

        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // --- Panel Superior: Datos de la Válvula ---
        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createTitledBorder("Datos de la Válvula"));
        topPanel.setPreferredSize(new Dimension(1200, 300)); // Tamaño fijo para no deformarse

        // Fila 1: Cliente y Fluido de servicio
        JPanel clienteFluidoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblCliente = new JLabel("Cliente:");
        clienteFluidoPanel.add(lblCliente);
        cboCliente = new JComboBox<>();
        clienteFluidoPanel.add(cboCliente);
        lblFluidoServicio = new JLabel("Fluido de servicio:");
        clienteFluidoPanel.add(lblFluidoServicio);
        cboFluidoServicio = new JComboBox<>();
        clienteFluidoPanel.add(cboFluidoServicio);
        topPanel.add(clienteFluidoPanel);

        // Crear los tres paneles principales (izquierda, central, derecha)
        camposGeneralesPanel = createInputPanel("Campos Generales",
                new String[]{"TAG:", "N° de serie:", "Lugar de conexión:", "Marca:", "Material del cuerpo:"},
                new JTextField[]{txtTag = new JTextField(), txtNroSerie = new JTextField(), txtLugarConexion = new JTextField(),
                        txtMarca = new JTextField(), txtMaterialCuerpo = new JTextField()});

        entradaPanel = createInputPanel("Entrada",
                new String[]{"Rosca Tipo:", "Brida Diametro:", "Brida Serie:"},
                new JTextField[]{txtEntradaRoscaTipo = new JTextField(), txtEntradaBridaDiametro = new JTextField(),
                        txtEntradaBridaSerie = new JTextField()});

        salidaPanel = createInputPanel("Salida",
                new String[]{"Rosca Tipo:", "Brida Diametro:", "Brida Serie:"},
                new JTextField[]{txtSalidaRoscaTipo = new JTextField(), txtSalidaBridaDiametro = new JTextField(),
                        txtSalidaBridaSerie = new JTextField()});

        // Agrupar los tres paneles en uno con GridLayout
        JPanel detailsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        detailsPanel.add(camposGeneralesPanel);
        detailsPanel.add(entradaPanel);
        detailsPanel.add(salidaPanel);
        topPanel.add(detailsPanel);

        add(topPanel, BorderLayout.NORTH);

        // --- Panel Central: Búsqueda y Tabla ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        // Búsqueda por cliente (encima de la tabla)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblBusquedaCliente = new JLabel("Buscar por Cliente:");
        cboBusquedaCliente = new JComboBox<>();
        cboBusquedaCliente.addItem(new Cliente(0, "Todos los clientes", ""));
        searchPanel.add(lblBusquedaCliente);
        searchPanel.add(cboBusquedaCliente);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        // Tabla
        String[] columnNames = {"ID", "Cliente", "TAG", "N° Serie", "L. de Conex"};
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
        tblValvulas = new JTable(tableModel);
        tblValvulas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tblValvulas);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- Panel Inferior: Botones ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnGuardar = new Button3D("Guardar nueva", new Color(200, 255, 200)); // Verde claro
        btnGuardar.setPreferredSize(new Dimension(200, 40));
        btnEditarSeleccionado = new Button3D("Editar Seleccionado", new Color(255, 255, 200)); // Amarillo claro
        btnEditarSeleccionado.setPreferredSize(new Dimension(200, 40));
        btnEliminarSeleccionado = new Button3D("Eliminar Seleccionado", new Color(255, 200, 200)); // Rosa claro
        btnEliminarSeleccionado.setPreferredSize(new Dimension(200, 40));
        btnSalir = new Button3D("Salir", new Color(255, 200, 200)); // Rosa claro
        btnSalir.setPreferredSize(new Dimension(200, 40));

        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnEditarSeleccionado);
        buttonPanel.add(btnEliminarSeleccionado);
        buttonPanel.add(btnSalir);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);
        btnSalir.addActionListener(e -> limpiarFormularioYCerrar());
        btnEditarSeleccionado.addActionListener(this::btnEditarSeleccionadoActionPerformed);
        btnEliminarSeleccionado.addActionListener(this::btnEliminarSeleccionadoActionPerformed);

        tblValvulas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnEditarSeleccionadoActionPerformed(null);
                }
            }
        });

        // Listener para búsqueda por cliente
        cboBusquedaCliente.addActionListener(e -> {
            Cliente clienteSeleccionado = (Cliente) cboBusquedaCliente.getSelectedItem();
            if (clienteSeleccionado != null) {
                if (clienteSeleccionado.getId() == 0) {
                    cargarValvulas(); // Todos los clientes
                } else {
                    cargarValvulasPorCliente(clienteSeleccionado);
                }
            }
        });
    }

    private JPanel createInputPanel(String title, String[] labels, JTextField[] fields) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }

        return panel;
    }

    // --------------------------------------------------------------------
    // MÉTODOS DE SOPORTE Y LÓGICA DE LA VISTA
    // --------------------------------------------------------------------

    private void limpiarFormulario() {
        cboCliente.setSelectedIndex(-1);
        cboFluidoServicio.setSelectedIndex(-1);
        txtTag.setText("");
        txtNroSerie.setText("");
        txtLugarConexion.setText("");
        txtMarca.setText("");
        txtMaterialCuerpo.setText("");
        txtEntradaRoscaTipo.setText("");
        txtEntradaBridaDiametro.setText("");
        txtEntradaBridaSerie.setText("");
        txtSalidaRoscaTipo.setText("");
        txtSalidaBridaDiametro.setText("");
        txtSalidaBridaSerie.setText("");
        btnGuardar.setText("Guardar nueva");
        valvulaSeleccionada = null;
        tblValvulas.clearSelection();
    }

    private void limpiarFormularioYCerrar() {
        limpiarFormulario();
        dispose();
    }

    private void cargarClientesYFluidos() {
        try {
            // Cargar Clientes
            List<Cliente> clientes = controller.obtenerTodosClientes();
            cboCliente.removeAllItems();
            for (Cliente cliente : clientes) {
                cboCliente.addItem(cliente);
            }

            // Cargar Fluidos de Servicio
            List<Fluido> fluidos = controller.obtenerTodosFluidosServicio();
            cboFluidoServicio.removeAllItems();
            for (Fluido fluido : fluidos) {
                cboFluidoServicio.addItem(fluido);
            }

            // Cargar opciones de búsqueda
            cboBusquedaCliente.removeAllItems();
            cboBusquedaCliente.addItem(new Cliente(0, "Todos los clientes", ""));
            for (Cliente cliente : clientes) {
                cboBusquedaCliente.addItem(cliente);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar datos iniciales:",
                    "Error de Carga",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar datos iniciales:", ex);
        }
    }

    private void cargarValvulas() {
        try {
            tableModel.setRowCount(0);
            List<Valvula> valvulas = controller.obtenerTodasValvulas();

            for (Valvula valvula : valvulas) {
                Object[] rowData = new Object[]{
                        valvula.getId(),
                        valvula.getCliente().getNombre(), // Suponiendo que Cliente tiene getNombre()
                        valvula.getTag(),
                        valvula.getNumeroSerie(),
                        valvula.getLugarConexion()
                };
                tableModel.addRow(rowData);
            }
            limpiarFormulario();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar las válvulas",
                    "Error de Carga",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar las válvulas", ex);
        }
    }

    private void cargarValvulasPorCliente(Cliente cliente) {
        try {
            tableModel.setRowCount(0);
            List<Valvula> valvulas = controller.obtenerValvulasPorCliente(cliente.getId());

            for (Valvula valvula : valvulas) {
                Object[] rowData = new Object[]{
                        valvula.getId(),
                        valvula.getCliente().getNombre(),
                        valvula.getTag(),
                        valvula.getNumeroSerie(),
                        valvula.getLugarConexion()
                };
                tableModel.addRow(rowData);
            }
            limpiarFormulario();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar las válvulas del cliente",
                    "Error de Carga",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar las válvulas del cliente", ex);
        }
    }

    private void btnGuardarActionPerformed(ActionEvent e) {
        Cliente cliente = (Cliente) cboCliente.getSelectedItem();
        Fluido fluido = (Fluido) cboFluidoServicio.getSelectedItem();
        String tag = txtTag.getText().trim();
        String nroSerie = txtNroSerie.getText().trim();
        String lugarConexion = txtLugarConexion.getText().trim();
        String marca = txtMarca.getText().trim();
        String materialCuerpo = txtMaterialCuerpo.getText().trim();
        String entradaRoscaTipo = txtEntradaRoscaTipo.getText().trim();
        String entradaBridaDiametro = txtEntradaBridaDiametro.getText().trim();
        String entradaBridaSerie = txtEntradaBridaSerie.getText().trim();
        String salidaRoscaTipo = txtSalidaRoscaTipo.getText().trim();
        String salidaBridaDiametro = txtSalidaBridaDiametro.getText().trim();
        String salidaBridaSerie = txtSalidaBridaSerie.getText().trim();

        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un Cliente.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (fluido == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un Fluido de servicio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (tag.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo TAG es obligatorio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (lugarConexion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo Lugar de conexión es obligatorio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Valvula valvulaAGuardar;
        String mensajeExito;

        if (valvulaSeleccionada == null) {
            valvulaAGuardar = new Valvula();
            valvulaAGuardar.setCliente(cliente);
            valvulaAGuardar.setFluido(fluido);
            valvulaAGuardar.setTag(tag);
            valvulaAGuardar.setNumeroSerie(nroSerie);
            valvulaAGuardar.setLugarConexion(lugarConexion);
            valvulaAGuardar.setMarca(marca);
            valvulaAGuardar.setMaterialCuerpo(materialCuerpo);
            valvulaAGuardar.setEntradaRoscaTipo(entradaRoscaTipo);
            valvulaAGuardar.setEntradaBridaDiametro(entradaBridaDiametro);
            valvulaAGuardar.setEntradaBridaSerie(entradaBridaSerie);
            valvulaAGuardar.setSalidaRoscaTipo(salidaRoscaTipo);
            valvulaAGuardar.setSalidaBridaDiametro(salidaBridaDiametro);
            valvulaAGuardar.setSalidaBridaSerie(salidaBridaSerie);

            mensajeExito = "Válvula guardada exitosamente.";
        } else {
            valvulaAGuardar = valvulaSeleccionada;
            valvulaAGuardar.setCliente(cliente);
            valvulaAGuardar.setFluido(fluido);
            valvulaAGuardar.setTag(tag);
            valvulaAGuardar.setNumeroSerie(nroSerie);
            valvulaAGuardar.setLugarConexion(lugarConexion);
            valvulaAGuardar.setMarca(marca);
            valvulaAGuardar.setMaterialCuerpo(materialCuerpo);
            valvulaAGuardar.setEntradaRoscaTipo(entradaRoscaTipo);
            valvulaAGuardar.setEntradaBridaDiametro(entradaBridaDiametro);
            valvulaAGuardar.setEntradaBridaSerie(entradaBridaSerie);
            valvulaAGuardar.setSalidaRoscaTipo(salidaRoscaTipo);
            valvulaAGuardar.setSalidaBridaDiametro(salidaBridaDiametro);
            valvulaAGuardar.setSalidaBridaSerie(salidaBridaSerie);

            mensajeExito = "Válvula actualizada exitosamente.";
        }

        try {
            // Validación de duplicado solo si es nuevo registro
            if (valvulaSeleccionada == null) {
                boolean existeDuplicado = controller.existeValvulaDuplicada(cliente.getId(), tag, lugarConexion);
                if (existeDuplicado) {
                    JOptionPane.showMessageDialog(this,
                            "Ya existe una válvula con el mismo Cliente, TAG y Lugar de conexión.",
                            "Error de Duplicado",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            boolean success = controller.guardarOActualizarValvula(valvulaAGuardar);

            if (success) {
                JOptionPane.showMessageDialog(this, mensajeExito, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarValvulas();
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar/actualizar la válvula (Controller retornó false).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de sistema al guardar/actualizar", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            logger.error("Error de sistema al guardar/actualizar", ex);
        }
    }

    private void btnEditarSeleccionadoActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblValvulas.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idValvula = (int) tblValvulas.getValueAt(filaSeleccionada, 0);
                String tag = (String) tblValvulas.getValueAt(filaSeleccionada, 2);
                String nroSerie = (String) tblValvulas.getValueAt(filaSeleccionada, 3);
                String lugarConexion = (String) tblValvulas.getValueAt(filaSeleccionada, 4);

                // Obtener la válvula completa desde el controlador
                Valvula valvula = controller.obtenerValvulaPorId(idValvula);

                if (valvula != null) {
                    valvulaSeleccionada = valvula;

                    // Cargar datos en los campos
                    cboCliente.setSelectedItem(valvula.getCliente());
                    cboFluidoServicio.setSelectedItem(valvula.getFluido());
                    txtTag.setText(tag);
                    txtNroSerie.setText(nroSerie);
                    txtLugarConexion.setText(lugarConexion);
                    txtMarca.setText(valvula.getMarca());
                    txtMaterialCuerpo.setText(valvula.getMaterialCuerpo());
                    txtEntradaRoscaTipo.setText(valvula.getEntradaRoscaTipo());
                    txtEntradaBridaDiametro.setText(valvula.getEntradaBridaDiametro());
                    txtEntradaBridaSerie.setText(valvula.getEntradaBridaSerie());
                    txtSalidaRoscaTipo.setText(valvula.getSalidaRoscaTipo());
                    txtSalidaBridaDiametro.setText(valvula.getSalidaBridaDiametro());
                    txtSalidaBridaSerie.setText(valvula.getSalidaBridaSerie());

                    btnGuardar.setText("Guardar Cambios (ID: " + idValvula + ")");
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar los datos para edición", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al cargar los datos para edición", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una válvula de la lista para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnEliminarSeleccionadoActionPerformed(ActionEvent e) {
        int filaSeleccionada = tblValvulas.getSelectedRow();

        if (filaSeleccionada >= 0) {
            try {
                int idValvula = (int) tblValvulas.getValueAt(filaSeleccionada, 0);
                String tagValvula = (String) tblValvulas.getValueAt(filaSeleccionada, 2);
                String clienteValvula = (String) tblValvulas.getValueAt(filaSeleccionada, 1);

                int confirmacion = JOptionPane.showConfirmDialog(
                        this,
                        "¿Está seguro de que desea eliminar la válvula: " + tagValvula +
                                " (Cliente: " + clienteValvula + ", ID: " + idValvula + ")?",
                        "Confirmar Eliminación",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirmacion == JOptionPane.YES_OPTION) {
                    if (controller.eliminarValvula(idValvula)) {
                        JOptionPane.showMessageDialog(this, "Válvula eliminada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        limpiarFormulario();
                        cargarValvulas();
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar la válvula (Verifique si hay registros relacionados).", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al obtener el ID de la válvula para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error al obtener el ID de la válvula para eliminar", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una válvula de la lista para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
}