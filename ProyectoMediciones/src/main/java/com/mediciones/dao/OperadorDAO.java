package com.mediciones.dao;

import com.mediciones.model.Operador;
import com.mediciones.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Operador.
 * Versión corregida para funcionar con MySQL (no SQLite).
 */
public class OperadorDAO {

    private static final String INSERT_SQL = "INSERT INTO operador (nombre, identificacion) VALUES (?, ?)";
    private static final String SELECT_ALL_SQL = "SELECT id, nombre, identificacion FROM operador";
    private static final String UPDATE_SQL = "UPDATE operador SET nombre = ?, identificacion = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM operador WHERE id = ?";

    // ========================================================================
    // CREATE / UPDATE (Guardar o Actualizar)
    // ========================================================================
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
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, operador.getNombre());
            pstmt.setString(2, operador.getIdentificacion());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // ✅ CORRECCIÓN: Usar getGeneratedKeys() (compatible con MySQL)
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        operador.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error al insertar operador: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error al actualizar operador: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ========================================================================
    // READ (Leer todos)
    // ========================================================================
    /**
     * Obtiene todos los operadores registrados en la base de datos.
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
                operadores.add(new Operador(id, nombre, identificacion));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener operadores: " + e.getMessage());
            e.printStackTrace();
        }
        return operadores;
    }

    // ========================================================================
    // DELETE (Eliminar)
    // ========================================================================
    /**
     * Elimina un operador por su ID.
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
            System.err.println("Error al eliminar operador: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}