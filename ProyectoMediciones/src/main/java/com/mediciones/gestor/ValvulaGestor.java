package com.mediciones.gestor;

import com.mediciones.dao.ValvulaDAO;
import com.mediciones.model.Valvula;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador para la lógica de negocio relacionada con la entidad Válvula.
 * Actúa como intermediario entre la Vista (FrmValvulasCRUD) y el DAO (ValvulaDAO).
 */
public class ValvulaGestor {

    private final ValvulaDAO valvulaDAO;

    public ValvulaGestor() {
        this.valvulaDAO = new ValvulaDAO(); // Usa "valvulaDAO" en lugar de "ValvulaDAO"
    }

    /**
     * Obtiene todas las válvulas registradas.
     * @return Lista de objetos Valvula.
     */
    public List<Valvula> obtenerTodasValvulas() {
        return valvulaDAO.obtenerTodas(); // Usa la instancia "valvulaDAO"
    }

    /**
     * Obtiene todas las válvulas asociadas a un cliente específico.
     * @param idCliente El ID del cliente.
     * @return Lista de objetos Valvula filtrados por cliente.
     */
    public List<Valvula> obtenerValvulasPorCliente(int idCliente) {
        return valvulaDAO.obtenerPorCliente(idCliente); // Usa la instancia "valvulaDAO"
    }

    /**
     * Guarda una nueva válvula o actualiza una existente.
     * La lógica se basa en si el objeto Valvula ya tiene un ID asignado.
     * @param valvula El objeto Valvula a guardar o actualizar.
     * @return true si la operación fue exitosa.
     * @throws SQLException Si ocurre un error en la base de datos.
     */
    public boolean guardarOActualizarValvula(Valvula valvula) throws SQLException {
        if (valvula.getId() > 0) {
            // Si tiene ID, se actualiza
            return valvulaDAO.actualizar(valvula); // Usa la instancia "valvulaDAO"
        } else {
            // Si no tiene ID, se inserta como nuevo
            return valvulaDAO.insertar(valvula); // Usa la instancia "valvulaDAO"
        }
    }

    /**
     * Elimina una válvula por su ID.
     * @param id El ID de la válvula a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminarValvula(int id) {
        return valvulaDAO.eliminar(id); // Usa la instancia "valvulaDAO"
    }

    /**
     * Verifica si existe una válvula duplicada basada en Cliente, TAG y Lugar de Conexión.
     * @param idCliente El ID del cliente.
     * @param tag El TAG de la válvula.
     * @param lugarConexion El lugar de conexión de la válvula.
     * @return true si existe una válvula duplicada, false en caso contrario.
     */
    public boolean existeValvulaDuplicada(int idCliente, String tag, String lugarConexion) {
        return valvulaDAO.existeDuplicada(idCliente, tag, lugarConexion); // Usa la instancia "valvulaDAO"
    }

    /**
     * Obtiene una válvula específica por su ID.
     * @param id El ID de la válvula.
     * @return El objeto Valvula correspondiente al ID.
     */
    public Valvula obtenerValvulaPorId(int id) {
        return valvulaDAO.obtenerPorId(id); // Usa la instancia "valvulaDAO"
    }


    /**
     * Obtiene una válvula específica por su TAG.
     * @param tag El TAG de la válvula.
     * @return El objeto Valvula correspondiente al TAG, o null si no existe.
     */
    public Valvula obtenerValvulaPorTag(String tag) {
        return valvulaDAO.obtenerPorTag(tag); // Delega la lógica al DAO
    }

}