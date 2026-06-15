package com.mediciones.controller;

import com.mediciones.model.Cliente;
import com.mediciones.dao.ClienteDAO;
import java.util.List;

/**
 * Controlador para la lógica de negocio relacionada con la entidad Cliente.
 * Actúa como intermediario entre la Vista (FrmClienteCRUD) y el DAO (ClienteDAO).
 */
public class ClienteController {

    private final ClienteDAO clienteDAO;

    public ClienteController() {
        this.clienteDAO = new ClienteDAO();
    }

    /**
     * Busca un cliente por su nombre.
     * @param nombre El nombre del cliente a buscar.
     * @return El cliente encontrado, o null si no se encuentra.
     */
    public Cliente obtenerClientePorNombre(String nombre) {
        // Obtiene todos los clientes desde el DAO
        List<Cliente> clientes = clienteDAO.obtenerTodos();

        // Filtra la lista para encontrar el cliente con el nombre especificado
        return clientes.stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombre)) // Ignora mayúsculas/minúsculas
                .findFirst() // Encuentra el primer resultado
                .orElse(null); // Retorna null si no se encuentra
    }

    /**
     * Obtiene todos los clientes registrados.
     * @return Lista de objetos Cliente.
     */
    public List<Cliente> obtenerTodosClientes() {
        return clienteDAO.obtenerTodos();
    }

    /**
     * Guarda un nuevo cliente o actualiza uno existente.
     * La lógica se basa en si el objeto Cliente ya tiene un ID asignado.
     * @param cliente El objeto Cliente a guardar o actualizar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    public boolean guardarOActualizarCliente(Cliente cliente) {
        if (cliente.getId() > 0) {
            // Si tiene ID, se actualiza
            return clienteDAO.actualizar(cliente);
        } else {
            // Si no tiene ID, se inserta como nuevo
            return clienteDAO.insertar(cliente);
        }
    }

    /**
     * Elimina un cliente por su ID.
     * @param id El ID del cliente a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminarCliente(int id) {
        return clienteDAO.eliminar(id);
    }

    /**
     * Obtiene un cliente por su ID.
     * @param id El ID del cliente a buscar.
     * @return El cliente encontrado, o null si no se encuentra.
     */
    public Cliente obtenerClientePorId(int id) {
        return clienteDAO.obtenerPorId(id);
    }
}