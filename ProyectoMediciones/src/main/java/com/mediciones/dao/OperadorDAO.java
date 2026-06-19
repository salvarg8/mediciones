package com.mediciones.dao;

import com.mediciones.model.Operador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Operador.
 * Actualizado con borrado lógico y manejo de logs SLF4J.
 */
public class OperadorDAO {

    private static final Logger logger = LoggerFactory.getLogger(OperadorDAO.class);

    // Ajustamos las consultas SQL para soportar el borrado lógico
    private static final String INSERT_SQL = "INSERT INTO operador (nombre, identificacion) VALUES (?, ?)";
    private static final String SELECT_ALL_SQL = "SELECT id, nombre, identificacion, active FROM operador WHERE active = true";
    private static final String UPDATE_SQL = "UPDATE operador SET nombre = ?, identificacion = ? WHERE id = ? AND active = true";
    private static final String DELETE_SQL = "UPDATE operador SET active = false WHERE id = ?";

    /**
     * Guarda un nuevo operador o actualiza uno existente.
     * @param operador El objeto Operador a guardar o actualizar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    public boolean guardarOActualizar(Operador operador) {
        if (operador.getId() == 0) {
            return insertar(operador);
        } else {
            return actualizar(operador);
        }
    }

    /**
     * Inserta un nuevo operador en la base de datos.
     * @param operador El objeto Operador a insertar.
     * @return true si la inserción fue exitosa, false en caso contrario.
     */
    private boolean insertar(Operador operador) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, operador.getNombre());
            pstmt.setString(2, operador.getIdentificacion());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        operador.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            logger.error("Error al insertar operador: " + operador.getNombre(), e);
            return false;
        }
    }

    /**
     * Actualiza un operador existente en la base de datos.
     * @param operador El objeto Operador a actualizar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    private boolean actualizar(Operador operador) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, operador.getNombre());
            stmt.setString(2, operador.getIdentificacion());
            stmt.setInt(3, operador.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("Error al actualizar operador con ID: " + operador.getId(), e);
            return false;
        }
    }

    // ========================================================================
    // READ (Leer todos)
    // ========================================================================

    /**
     * Obtiene todos los operadores activos registrados en la base de datos.
     * @return Una lista de objetos Operador.
     */
    public List<Operador> obtenerTodos() {
        List<Operador> operadores = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String identificacion = rs.getString("identificacion");

                Operador operador = new Operador(id, nombre, identificacion);

                operadores.add(operador);
            }

        } catch (SQLException e) {
            logger.error("Error al obtener la lista de operadores.", e);
        }
        return operadores;
    }

    /**
     * Realiza un borrado lógico de un operador por su ID.
     * @param id El ID del operador a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminar(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("Error al realizar el borrado lógico del operador con ID: " + id, e);
            return false;
        }
    }
}