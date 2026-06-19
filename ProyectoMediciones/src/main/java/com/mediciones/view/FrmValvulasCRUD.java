package com.mediciones.view;

import com.mediciones.gestor.ClienteGestor;
import com.mediciones.gestor.FluidoGestor;
import com.mediciones.gestor.PlantaGestor;
import com.mediciones.gestor.ValvulaGestor;
import com.mediciones.model.Planta;
import com.mediciones.model.Valvula;
import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.view.components.Button3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
    private JLabel lblPlanta; // NUEVO
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

    // Componentes de entrada
    private JComboBox<Cliente> cboCliente;
    private JComboBox<Planta> cboPlanta; // NUEVO
    private JComboBox<Fluido> cboFluidoServicio;
    private JComboBox<Cliente> cboBusquedaCliente;
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

    // Tabla y modelo
    private JTable tblValvulas;
    private DefaultTableModel tableModel;

    // Controladores y entidad seleccionada
    private final ValvulaGestor gestor;
    private final ClienteGestor clienteGestor;
    private final FluidoGestor fluidoGestor;
    private final PlantaGestor plantaGestor; // NUEVO
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

        this.gestor = new ValvulaGestor();
        this.clienteGestor = new ClienteGestor();
        this.fluidoGestor = new FluidoGestor();
        this.plantaGestor = new PlantaGestor(); // Inicializamos el gestor de plantas

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
        topPanel.setPreferredSize(new Dimension(1200, 300));

        // Fila 1: Cliente, Planta y Fluido de servicio
        JPanel clienteFluidoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        lblCliente = new JLabel("Cliente:");
        cboCliente = new JComboBox<>();
        clienteFluidoPanel.add(lblCliente);
        clienteFluidoPanel.add(cboCliente);

        lblPlanta = new JLabel("Planta:");
        cboPlanta = new JComboBox<>();
        clienteFluidoPanel.add(lblPlanta);
        clienteFluidoPanel.add(cboPlanta);

        lblFluidoServicio = new JLabel("Fluido de servicio:");
        cboFluidoServicio = new JComboBox<>();
        clienteFluidoPanel.add(lblFluidoServicio);
        clienteFluidoPanel.add(cboFluidoServicio);

        topPanel.add(clienteFluidoPanel);

        // Listener para cargar las plantas cuando se elige un cliente
        cboCliente.addActionListener(e -> {
            cboPlanta.removeAllItems(); // Limpiamos el combo de plantas
            Cliente clienteSeleccionado = (Cliente) cboCliente.getSelectedItem();

            if (clienteSeleccionado != null && clienteSeleccionado.getId() != 0) {
                try {
                    List<Planta> plantasDelCliente = plantaGestor.obtenerTodasPlantasDeCliente(clienteSeleccionado);
                    for (Planta planta : plantasDelCliente) {
                        cboPlanta.addItem(planta);
                    }
                } catch (Exception ex) {
                    logger.error("Error al cargar las plantas del cliente", ex);
                }
            }
        });

        // Paneles de entrada de texto
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

        JPanel detailsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        detailsPanel.add(camposGeneralesPanel);
        detailsPanel.add(entradaPanel);
        detailsPanel.add(salidaPanel);
        topPanel.add(detailsPanel);

        add(topPanel, BorderLayout.NORTH);

        // --- Panel Central: Búsqueda y Tabla ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblBusquedaCliente = new JLabel("Buscar por Cliente:");
        cboBusquedaCliente = new JComboBox<>();
        cboBusquedaCliente.addItem(new Cliente(0, "Todos los clientes", ""));
        searchPanel.add(lblBusquedaCliente);
        searchPanel.add(cboBusquedaCliente);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Cliente", "Planta", "TAG", "N° Serie", "L. de Conex"};
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
        btnGuardar = new Button3D("Guardar nueva", new Color(200, 255, 200));
        btnGuardar.setPreferredSize(new Dimension(200, 40));
        btnEditarSeleccionado = new Button3D("Editar Seleccionado", new Color(255, 255, 200));
        btnEditarSeleccionado.setPreferredSize(new Dimension(200, 40));
        btnEliminarSeleccionado = new Button3D("Eliminar Seleccionado", new Color(255, 200, 200));
        btnEliminarSeleccionado.setPreferredSize(new Dimension(200, 40));
        btnSalir = new Button3D("Salir", new Color(255, 200, 200));
        btnSalir.setPreferredSize(new Dimension(200, 40));

        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnEditarSeleccionado);
        buttonPanel.add(btnEliminarSeleccionado);
        buttonPanel.add(btnSalir);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Listeners de botones ---
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

        cboBusquedaCliente.addActionListener(e -> {
            Cliente clienteSeleccionado = (Cliente) cboBusquedaCliente.getSelectedItem();
            if (clienteSeleccionado != null) {
                if (clienteSeleccionado.getId() == 0) {
                    cargarValvulas();
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
        cboPlanta.removeAllItems(); // Limpia la lista dependiente
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
            List<Cliente> clientes = clienteGestor.obtenerTodosClientes();
            cboCliente.removeAllItems();
            for (Cliente cliente : clientes) {
                cboCliente.addItem(cliente);
            }

            List<Fluido> fluidos = fluidoGestor.obtenerTodosFluidos();
            cboFluidoServicio.removeAllItems();
            for (Fluido fluido : fluidos) {
                cboFluidoServicio.addItem(fluido);
            }

            cboBusquedaCliente.removeAllItems();
            cboBusquedaCliente.addItem(new Cliente(0, "Todos los clientes", ""));
            for (Cliente cliente : clientes) {
                cboBusquedaCliente.addItem(cliente);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos iniciales.", "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Error al cargar datos iniciales:", ex);
        }
    }

    private void cargarValvulas() {
        try {
            tableModel.setRowCount(0);
            List<Valvula> valvulas = gestor.obtenerTodasValvulas();
            poblarTabla(valvulas);
            limpiarFormulario();
        } catch (Exception ex) {
            logger.error("Error al cargar las válvulas", ex);
        }
    }

    private void cargarValvulasPorCliente(Cliente cliente) {
        try {
            tableModel.setRowCount(0);
            List<Valvula> valvulas = gestor.obtenerValvulasPorCliente(cliente.getId());
            poblarTabla(valvulas);
            limpiarFormulario();
        } catch (Exception ex) {
            logger.error("Error al cargar las válvulas del cliente", ex);
        }
    }

    private void poblarTabla(List<Valvula> valvulas) {
        for (Valvula valvula : valvulas) {
            String nombreCliente = (valvula.getCliente() != null) ? valvula.getCliente().getNombre() : "N/A";
            String nombrePlanta = (valvula.getPlanta() != null) ? valvula.getPlanta().getNombre() : "N/A";

            Object[] rowData = new Object[]{
                    valvula.getId(),
                    nombreCliente,
                    nombrePlanta,
                    valvula.getTag(),
                    valvula.getNumeroSerie(),
                    valvula.getLugarConexion()
            };
            tableModel.addRow(rowData);
        }
    }

    private void btnGuardarActionPerformed(ActionEvent e) {
        Cliente cliente = (Cliente) cboCliente.getSelectedItem();
        Planta planta = (Planta) cboPlanta.getSelectedItem();
        Fluido fluido = (Fluido) cboFluidoServicio.getSelectedItem();
        String tag = txtTag.getText().trim();
        String nroSerie = txtNroSerie.getText().trim();
        String lugarConexion = txtLugarConexion.getText().trim();

        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un Cliente.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (planta == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una Planta. Si no hay opciones, registre una primero.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (fluido == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un Fluido de servicio.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (tag.isEmpty() || lugarConexion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos TAG y Lugar de conexión son obligatorios.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Valvula valvulaAGuardar = (valvulaSeleccionada == null) ? new Valvula() : valvulaSeleccionada;

        valvulaAGuardar.setCliente(cliente);
        valvulaAGuardar.setPlanta(planta);
        valvulaAGuardar.setFluido(fluido);
        valvulaAGuardar.setTag(tag);
        valvulaAGuardar.setNumeroSerie(nroSerie);
        valvulaAGuardar.setLugarConexion(lugarConexion);
        valvulaAGuardar.setMarca(txtMarca.getText().trim());
        valvulaAGuardar.setMaterialCuerpo(txtMaterialCuerpo.getText().trim());
        valvulaAGuardar.setEntradaRoscaTipo(txtEntradaRoscaTipo.getText().trim());
        valvulaAGuardar.setEntradaBridaDiametro(txtEntradaBridaDiametro.getText().trim());
        valvulaAGuardar.setEntradaBridaSerie(txtEntradaBridaSerie.getText().trim());
        valvulaAGuardar.setSalidaRoscaTipo(txtSalidaRoscaTipo.getText().trim());
        valvulaAGuardar.setSalidaBridaDiametro(txtSalidaBridaDiametro.getText().trim());
        valvulaAGuardar.setSalidaBridaSerie(txtSalidaBridaSerie.getText().trim());

        try {
            if (valvulaSeleccionada == null && gestor.existeValvulaDuplicada(cliente.getId(), tag, lugarConexion)) {
                JOptionPane.showMessageDialog(this, "Ya existe una válvula con este Cliente, TAG y Lugar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (gestor.guardarOActualizarValvula(valvulaAGuardar)) {
                JOptionPane.showMessageDialog(this, "Operación exitosa.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarValvulas();
            } else {
                JOptionPane.showMessageDialog(this, "Error al procesar la válvula.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            logger.error("Error de sistema", ex);
        }
    }

    private void btnEditarSeleccionadoActionPerformed(ActionEvent e) {
        int fila = tblValvulas.getSelectedRow();
        if (fila >= 0) {
            try {
                int idValvula = (int) tblValvulas.getValueAt(fila, 0);
                Valvula valvula = gestor.obtenerValvulaPorId(idValvula);

                if (valvula != null) {
                    valvulaSeleccionada = valvula;

                    if (valvula.getCliente() != null) {
                        seleccionarClienteEnCombo(valvula.getCliente().getId());
                    }
                    if (valvula.getPlanta() != null) {
                        seleccionarPlantaEnCombo(valvula.getPlanta().getId());
                    }
                    if (valvula.getFluido() != null) {
                        seleccionarFluidoEnCombo(valvula.getFluido().getId());
                    }

                    txtTag.setText(valvula.getTag());
                    txtNroSerie.setText(valvula.getNumeroSerie());
                    txtLugarConexion.setText(valvula.getLugarConexion());
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
                logger.error("Error al cargar datos para edición", ex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una válvula para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnEliminarSeleccionadoActionPerformed(ActionEvent e) {
        int fila = tblValvulas.getSelectedRow();
        if (fila >= 0) {
            int idValvula = (int) tblValvulas.getValueAt(fila, 0);
            String tagValvula = (String) tblValvulas.getValueAt(fila, 3);

            int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar la válvula " + tagValvula + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (gestor.eliminarValvula(idValvula)) {
                    JOptionPane.showMessageDialog(this, "Válvula eliminada.");
                    cargarValvulas();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar la válvula.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una válvula para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- Métodos de Selección Segura ---

    private void seleccionarClienteEnCombo(int idCliente) {
        for (int i = 0; i < cboCliente.getItemCount(); i++) {
            if (cboCliente.getItemAt(i).getId() == idCliente) {
                cboCliente.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarPlantaEnCombo(int idPlanta) {
        for (int i = 0; i < cboPlanta.getItemCount(); i++) {
            if (cboPlanta.getItemAt(i).getId() == idPlanta) {
                cboPlanta.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarFluidoEnCombo(int idFluido) {
        for (int i = 0; i < cboFluidoServicio.getItemCount(); i++) {
            if (cboFluidoServicio.getItemAt(i).getId() == idFluido) {
                cboFluidoServicio.setSelectedIndex(i);
                return;
            }
        }
    }
}