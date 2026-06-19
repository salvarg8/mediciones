package com.mediciones.dao;

import com.mediciones.model.Cliente;
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
 * Objeto de Acceso a Datos (DAO) para la entidad Cliente.
 * Actualizado con borrado lógico (active = true/false).
 */
public class ClienteDAO {

    private static final Logger logger = LoggerFactory.getLogger(ClienteDAO.class);

    public boolean insertar(Cliente cliente) {
        // No necesitamos insertar 'active' porque en la BD le pusimos DEFAULT TRUE
        String insertSql = "INSERT INTO clientes(nombre, nit) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, cliente.getNombre());
            pstmt.setString(2, cliente.getNit());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        cliente.setId(generatedKeys.getInt(1));
                        cliente.setActivo(true); // Sincronizamos el modelo
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error al insertar cliente: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean actualizar(Cliente cliente) {
        // Solo permitimos actualizar si el cliente está activo
        String sql = "UPDATE clientes SET nombre = ?, nit = ? WHERE id = ? AND active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNombre());
            pstmt.setString(2, cliente.getNit());
            pstmt.setInt(3, cliente.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar cliente: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean eliminar(int id) {
        // Cambiamos el DELETE físico por el borrado lógico
        String sql = "UPDATE clientes SET active = false WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Error al realizar borrado lógico de cliente: " + e.getMessage(), e);
            return false;
        }
    }

    // --- Métodos de LECTURA ---

    public List<Cliente> obtenerTodos() {
        List<Cliente> clientes = new ArrayList<>();
        // Filtramos solo los clientes que no han sido borrados
        String sql = "SELECT id, nombre, nit, active FROM clientes WHERE active = true";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Cliente c = new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("nit")
                );
                c.setActivo(rs.getBoolean("active"));
                clientes.add(c);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los clientes: " + e.getMessage(), e);
        }
        return clientes;
    }

    public Cliente obtenerPorId(int id) {
        // Nos aseguramos de traerlo solo si está activo
        String sql = "SELECT id, nombre, nit, active FROM clientes WHERE id = ? AND active = true";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("nit")
                    );
                    c.setActivo(rs.getBoolean("active"));
                    return c;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener cliente por ID: " + e.getMessage(), e);
        }
        return null;
    }
}