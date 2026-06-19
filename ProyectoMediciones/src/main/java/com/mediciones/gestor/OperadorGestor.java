package com.mediciones.gestor;

import com.mediciones.dao.OperadorDAO; // Asegúrate de que esta clase exista
import com.mediciones.model.Operador; // Asegúrate de que esta clase exista

import java.util.List;

/**
 * Controlador para la gestión de Operadores.
 */
public class OperadorGestor {

    // Se asume que OperadorDAO es una clase existente en tu paquete dao
    private final OperadorDAO operadorDAO;

    public OperadorGestor() {
        // Inicializa el Data Access Object (DAO)
        this.operadorDAO = new OperadorDAO();
    }

    /**
     * Busca un operador por su nombre.
     *
     * @param nombre El nombre del operador a buscar.
     * @return El operador encontrado, o null si no se encuentra.
     */
    public Operador obtenerOperadorPorNombre(String nombre) {
        // Obtiene la lista de todos los operadores desde el DAO
        List<Operador> operadores = operadorDAO.obtenerTodos();

        // Filtra la lista para encontrar el operador con el nombre especificado
        return operadores.stream()
                .filter(o -> o.getNombre().equalsIgnoreCase(nombre)) // Ignora mayúsculas/minúsculas
                .findFirst() // Encuentra el primer resultado
                .orElse(null); // Retorna null si no se encuentra
    }

    /**
     * Guarda un nuevo Operador o actualiza uno existente.
     *
     * @param operador El objeto Operador a guardar o actualizar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    public boolean guardarOActualizarOperador(Operador operador) {
        // Llama al método del DAO que maneja la lógica de INSERT o UPDATE
        return operadorDAO.guardarOActualizar(operador);
    }

    /**
     * Obtiene todos los Operadores de la base de datos.
     *
     * @return Una lista de objetos Operador.
     */
    public List<Operador> obtenerTodosOperadores() {
        return operadorDAO.obtenerTodos();
    }

    /**
     * Elimina un Operador por su ID.
     *
     * @param id El ID del Operador a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminarOperador(int id) {
        return operadorDAO.eliminar(id);
    }
}