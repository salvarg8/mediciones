package com.mediciones.dao;

import com.fazecast.jSerialComm.*;
import com.mediciones.controller.ClienteController;
import com.mediciones.controller.FluidoController;
import com.mediciones.controller.OperadorController;
import com.mediciones.controller.ValvulaController;
import com.mediciones.model.*;
import com.mediciones.repository.ArchivoNoEncontradoException;
import com.mediciones.repository.PortalRepository;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clase de ayuda para operaciones de interfaz de usuario relacionadas con gráficos en tiempo real.
 * NOTA: Aunque se llama "DAO", esta clase NO accede directamente a la base de datos.
 *       Actúa como intermediario entre la UI y los controladores.
 */
public class RealTimeGraphDAO {

    private final ValvulaController valvulaController;
    private final ClienteController clienteController;
    private final OperadorController operadorController;
    private final FluidoController fluidoController;
    private final ConfiguracionDAO configuracionDAO;
    private final  PortalRepository portalRepository;

    public RealTimeGraphDAO() {
        this.valvulaController = new ValvulaController();
        this.clienteController = new ClienteController();
        this.operadorController = new OperadorController();
        this.fluidoController = new FluidoController();
        this.configuracionDAO = new ConfiguracionDAO();
        this.portalRepository = new PortalRepository();
    }

    // --- Métodos de acceso a datos (delegan a los controladores) ---

    public List<Cliente> obtenerTodosClientes() throws ArchivoNoEncontradoException {

        Configuracion config =
                configuracionDAO.obtenerConfiguracion();

        if ("TXT".equalsIgnoreCase(config.getOrigenDatos())) {

            PortalRepository repository = new PortalRepository();

            repository.recargar(config.getRutaArchivo());

            return repository.getClientes();
        }

        return clienteController.obtenerTodosClientes();
    }

    public List<Valvula> obtenerValvulasPorCliente(int clienteId) throws ArchivoNoEncontradoException {

        Configuracion config =
                configuracionDAO.obtenerConfiguracion();

        if ("TXT".equalsIgnoreCase(config.getOrigenDatos())) {

            PortalRepository repository = new PortalRepository();

            repository.recargar(config.getRutaArchivo());

            return repository.getValvulasPorCliente(clienteId);
        }

        return valvulaController.obtenerValvulasPorCliente(clienteId);
    }

    public List<Operador> obtenerTodosOperadores() {
        return operadorController.obtenerTodosOperadores();
    }

    public Operador obtenerOperadorPorNombre(String nombre) {
        if (Objects.isNull(nombre) || nombre.trim().isEmpty()) {
            return null;
        }
        return operadorController.obtenerOperadorPorNombre(nombre);
    }

    public List<Fluido> obtenerTodosFluidos() {
        return fluidoController.obtenerTodosFluidos();
    }

    public Fluido obtenerFluidoPorNombre(String nombre) {
        if (Objects.isNull(nombre) || nombre.trim().isEmpty()) {
            return null;
        }
        return fluidoController.obtenerFluidoPorNombre(nombre);
    }

    // --- Métodos para cargar componentes UI ---

    public void cargarComboBoxClientes(JComboBox<Cliente> cmbCliente) throws ArchivoNoEncontradoException {
        DefaultComboBoxModel<Cliente> clienteModel = new DefaultComboBoxModel<>();
        clienteModel.addElement(null); // Placeholder para selección nula

        try {
            List<Cliente> clientes = obtenerTodosClientes();
            for (Cliente c : clientes) {
                clienteModel.addElement(c);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar clientes en ComboBox: " + e.getMessage());
            clienteModel.addElement(new Cliente(0, "Error de carga", "ERR"));
        }

        cmbCliente.setModel(clienteModel);
        cmbCliente.setSelectedIndex(0);

        // Configurar renderizador para mostrar nombres y manejar null
        cmbCliente.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == null) {
                    setText("Seleccione un cliente...");
                    setForeground(Color.GRAY);
                } else if (value instanceof Cliente) {
                    Cliente cliente = (Cliente) value;
                    setText(cliente.getNombre());
                    setForeground(Color.BLACK);
                } else {
                    setText("Elemento inválido");
                    setForeground(Color.RED);
                }
                return this;
            }
        });
    }

    public void cargarComboBoxValvulas(JComboBox<Valvula> cmbValvula, Integer clienteId) throws ArchivoNoEncontradoException {
        DefaultComboBoxModel<Valvula> valvulaModel = new DefaultComboBoxModel<>();
        valvulaModel.addElement(null); // Placeholder

        try {
            if (clienteId != null && clienteId > 0) {
                List<Valvula> valvulas = obtenerValvulasPorCliente(clienteId);
                for (Valvula v : valvulas) {
                    valvulaModel.addElement(v);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar válvulas: " + e.getMessage());

            // ✅ CORRECCIÓN: Crear objeto Valvula de forma correcta
            Valvula errorValvula = new Valvula();
            errorValvula.setId(0);
            errorValvula.setTag("Error de carga");
            errorValvula.setNumeroSerie(null);
            errorValvula.setLugarConexion(null);
            errorValvula.setMarca(null);
            errorValvula.setMaterialCuerpo(null);
            errorValvula.setEntradaRoscaTipo(null);
            errorValvula.setEntradaBridaDiametro(null);
            errorValvula.setEntradaBridaSerie(null);
            errorValvula.setSalidaRoscaTipo(null);
            errorValvula.setSalidaBridaDiametro(null);
            errorValvula.setSalidaBridaSerie(null);

            valvulaModel.addElement(errorValvula);
        }

        cmbValvula.setModel(valvulaModel);
        cmbValvula.setSelectedIndex(0);

        // Configurar renderizador para válvulas
        cmbValvula.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == null) {
                    setText("Seleccione una válvula...");
                    setForeground(Color.GRAY);
                } else if (value instanceof Valvula) {
                    Valvula valvula = (Valvula) value;
                    String tag = (valvula.getTag() != null && !valvula.getTag().isEmpty())
                            ? valvula.getTag()
                            : "Sin TAG";
                    String serie = (valvula.getEntradaBridaSerie() != null)
                            ? valvula.getEntradaBridaSerie()
                            : "Sin serie";
                    setText(String.format("%s (%s)", tag, serie));
                    setForeground(Color.BLACK);
                } else {
                    setText("Válvula inválida");
                    setForeground(Color.RED);
                }
                return this;
            }
        });
    }

    public void recargarPortal() throws ArchivoNoEncontradoException {

        Configuracion configuracion =
                configuracionDAO.obtenerConfiguracion();

        if (configuracion == null) {
            return;
        }

        if (!"TXT".equalsIgnoreCase(configuracion.getOrigenDatos())) {
            return;
        }

        portalRepository.recargar(
                configuracion.getRutaArchivo()
        );

    }
}
