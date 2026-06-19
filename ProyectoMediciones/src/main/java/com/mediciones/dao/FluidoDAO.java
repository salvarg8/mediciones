package com.mediciones.dao;

import com.mediciones.model.Fluido;
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
 * Actualizado con borrado lógico y manejo de logs SLF4J.
 */
public class FluidoDAO {

    private static final Logger logger = LoggerFactory.getLogger(FluidoDAO.class);

    /**
     * Obtiene todos los fluidos activos desde la base de datos.
     * @return Una lista de objetos Fluido.
     */
    public List<Fluido> obtenerTodos() {
        List<Fluido> fluidos = new ArrayList<>();
        // Filtramos para traer solo los fluidos que no han sido borrados lógicamente
        String sql = "SELECT id, nombre FROM fluidos WHERE active = true";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                Fluido fluido = new Fluido(id, nombre);
                // Si tienes un campo 'activo' en tu modelo Fluido, puedes mapearlo aquí:
                // fluido.setActivo(true);

                fluidos.add(fluido);
            }

        } catch (SQLException e) {
            logger.error("Error al obtener la lista de fluidos.", e);
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
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        fluido.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error al insertar el fluido: " + fluido.getNombre(), e);
            return false;
        }
    }

    /**
     * Actualiza un fluido existente en la base de datos.
     * @param fluido El objeto Fluido a actualizar.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    public boolean actualizar(Fluido fluido) {
        String sql = "UPDATE fluidos SET nombre = ? WHERE id = ? AND active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fluido.getNombre());
            pstmt.setInt(2, fluido.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar el fluido con ID: " + fluido.getId(), e);
            return false;
        }
    }

    /**
     * Elimina un fluido lógicamente por su ID.
     * @param id El ID del fluido a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminar(int id) {
        String sql = "UPDATE fluidos SET active = false WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Error al realizar el borrado lógico del fluido con ID: " + id, e);
            return false;
        }
    }
}