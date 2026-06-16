package com.mediciones.dao;

import com.mediciones.model.Fluido;
import com.mediciones.view.FrmOperadorCRUD;
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
 * Objeto de Acceso a Datos (DAO) para la entidad Fluido.
 * Versión corregida para funcionar con MySQL.
 */
public class FluidoDAO {

    private static final Logger logger = LoggerFactory.getLogger(FluidoDAO.class);

    /**
     * Obtiene todos los fluidos desde la base de datos.
     * @return Una lista de objetos Fluido.
     */
    public List<Fluido> obtenerTodos() {
        List<Fluido> fluidos = new ArrayList<>();
        String sql = "SELECT id, nombre FROM fluidos";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                fluidos.add(new Fluido(id, nombre));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener fluidos: " + e.getMessage());
            e.printStackTrace();
        }
        return fluidos;
    }

    /**
     * Inserta un nuevo fluido en la base de datos.
     * @param fluido El objeto Fluido a insertar.
     * @return true si la inserción fue exitosa, false en caso contrario.
     */
    public boolean insertar(Fluido fluido) {
        String sql = "INSERT INTO fluidos(nombre) VALUES(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fluido.getNombre());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // ✅ CORRECCIÓN: Usar getGeneratedKeys() (compatible con MySQL)
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        fluido.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error al insertar fluido: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza un fluido existente en la base de datos.
     * @param fluido El objeto Fluido a actualizar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    public boolean actualizar(Fluido fluido) {
        String sql = "UPDATE fluidos SET nombre = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fluido.getNombre());
            pstmt.setInt(2, fluido.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar fluido: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un fluido por su ID.
     * @param id El ID del fluido a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM fluidos WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar fluido: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}