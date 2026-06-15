package com.mediciones.controller;

import com.mediciones.model.Fluido;
import com.mediciones.dao.FluidoDAO;
import java.util.List;

/**
 * Controlador para la lógica de negocio relacionada con la entidad Fluido.
 * Actúa como intermediario entre la Vista y el DAO (FluidoDAO).
 */
public class FluidoController {

    private final FluidoDAO fluidoDAO;

    public FluidoController() {
        this.fluidoDAO = new FluidoDAO();
    }

    /**
     * Obtiene todos los fluidos registrados.
     * @return Lista de objetos Fluido.
     */
    public List<Fluido> obtenerTodosFluidos() {
        return fluidoDAO.obtenerTodos();
    }

    /**
     * Guarda un nuevo fluido o actualiza uno existente.
     * La lógica se basa en si el objeto Fluido ya tiene un ID asignado.
     * @param fluido El objeto Fluido a guardar o actualizar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    public boolean guardarOActualizarFluido(Fluido fluido) {
        if (fluido.getId() > 0) {
            // Si tiene ID, se actualiza
            return fluidoDAO.actualizar(fluido);
        } else {
            // Si no tiene ID, se inserta como nuevo
            return fluidoDAO.insertar(fluido);
        }
    }

    /**
     * Busca un fluido por su nombre.
     * @param nombre El nombre del fluido a buscar.
     * @return El fluido encontrado, o null si no se encuentra.
     */
    public Fluido obtenerFluidoPorNombre(String nombre) {
        // Obtiene todos los fluidos desde el DAO
        List<Fluido> fluidos = fluidoDAO.obtenerTodos();

        // Filtra la lista para encontrar el fluido con el nombre especificado
        return fluidos.stream()
                .filter(f -> f.getNombre().equalsIgnoreCase(nombre)) // Ignora mayúsculas/minúsculas
                .findFirst() // Encuentra el primer resultado
                .orElse(null); // Retorna null si no se encuentra
    }

    /**
     * Elimina un fluido por su ID.
     * @param id El ID del fluido a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminarFluido(int id) {
        return fluidoDAO.eliminar(id);
    }
}