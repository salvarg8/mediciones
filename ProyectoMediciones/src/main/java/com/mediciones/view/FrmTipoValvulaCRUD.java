package com.mediciones.view;

import com.mediciones.gestor.TipoValvulaGestor;
import com.mediciones.model.TipoValvula;
import com.mediciones.view.components.Button3D;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class FrmTipoValvulaCRUD extends JFrame {

    private JTextField txtNombre;
    private JTable tblTipos;
    private DefaultTableModel tableModel;

    private Button3D btnGuardar;
    private Button3D btnEditarSeleccionado;
    private Button3D btnEliminarSeleccionado;
    private Button3D btnSalir;

    private final TipoValvulaGestor gestor;
    private TipoValvula tipoSeleccionado;

    public FrmTipoValvulaCRUD() {
        super("Gestión de Tipos de Válvula");
        this.gestor = new TipoValvulaGestor();

        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        cargarTipos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // --- Panel Superior: Entrada de datos ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Datos del Tipo de Válvula"));

        topPanel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField(20);
        topPanel.add(txtNombre);

        add(topPanel, BorderLayout.NORTH);

        // --- Panel Central: Tabla ---
        String[] columnNames = {"ID", "Nombre"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblTipos = new JTable(tableModel);
        tblTipos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tblTipos);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- Panel Inferior: Botones ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnGuardar = new Button3D("Guardar", new Color(200, 255, 200));
        btnGuardar.setPreferredSize(new Dimension(120, 35));

        btnEditarSeleccionado = new Button3D("Editar", new Color(255, 255, 200));
        btnEditarSeleccionado.setPreferredSize(new Dimension(120, 35));

        btnEliminarSeleccionado = new Button3D("Eliminar", new Color(255, 200, 200));
        btnEliminarSeleccionado.setPreferredSize(new Dimension(120, 35));

        btnSalir = new Button3D("Salir", new Color(220, 220, 220));
        btnSalir.setPreferredSize(new Dimension(120, 35));

        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnEditarSeleccionado);
        buttonPanel.add(btnEliminarSeleccionado);
        buttonPanel.add(btnSalir);

        add(buttonPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);
        btnEditarSeleccionado.addActionListener(this::btnEditarSeleccionadoActionPerformed);
        btnEliminarSeleccionado.addActionListener(this::btnEliminarSeleccionadoActionPerformed);
        btnSalir.addActionListener(e -> dispose());

        tblTipos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnEditarSeleccionadoActionPerformed(null);
                }
            }
        });
    }

    private void cargarTipos() {
        tableModel.setRowCount(0);
        List<TipoValvula> tipos = gestor.getAll();
        for (TipoValvula tipo : tipos) {
            tableModel.addRow(new Object[]{tipo.getId(), tipo.getNombre()});
        }
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        tipoSeleccionado = null;
        btnGuardar.setText("Guardar");
        tblTipos.clearSelection();
    }

    private void btnGuardarActionPerformed(ActionEvent e) {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TipoValvula tipo = (tipoSeleccionado == null) ? new TipoValvula() : tipoSeleccionado;
        tipo.setNombre(nombre);

        if (gestor.guardarOActualizar(tipo)) {
            JOptionPane.showMessageDialog(this, "Tipo de Válvula guardado exitosamente.");
            cargarTipos();
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnEditarSeleccionadoActionPerformed(ActionEvent e) {
        int fila = tblTipos.getSelectedRow();
        if (fila >= 0) {
            Integer id = (Integer) tblTipos.getValueAt(fila, 0);
            String nombre = (String) tblTipos.getValueAt(fila, 1);

            tipoSeleccionado = new TipoValvula(id, nombre);
            txtNombre.setText(nombre);
            btnGuardar.setText("Actualizar");
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para editar.");
        }
    }

    private void btnEliminarSeleccionadoActionPerformed(ActionEvent e) {
        int fila = tblTipos.getSelectedRow();
        if (fila >= 0) {
            int id = (int) tblTipos.getValueAt(fila, 0);
            String nombre = (String) tblTipos.getValueAt(fila, 1);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de eliminar el tipo: " + nombre + "?",
                    "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (gestor.eliminar(id)) {
                    JOptionPane.showMessageDialog(this, "Registro eliminado correctamente.");
                    cargarTipos();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al eliminar el registro.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para eliminar.");
        }
    }
}