package com.mediciones.dao;

import com.mediciones.model.Cliente;
import com.mediciones.dao.DatabaseManager;
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
 * Objeto de Acceso a Datos (DAO) para la entidad Cliente.
 * Versión corregida para funcionar con MySQL.
 */
public class ClienteDAO {

    private static final Logger logger = LoggerFactory.getLogger(ClienteDAO.class);

    public boolean insertar(Cliente cliente) {
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
        String sql = "UPDATE clientes SET nombre = ?, nit = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cliente.getNombre());
            pstmt.setString(2, cliente.getNit());
            pstmt.setInt(3, cliente.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar cliente: " + e.getMessage(),e);
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al eliminar cliente: " + e.getMessage(),e);
            return false;
        }
    }

    public List<Cliente> obtenerTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT id, nombre, nit FROM clientes";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("nit")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los clientes: " + e.getMessage(),e);
        }
        return clientes;
    }

    public Cliente obtenerPorId(int id) {
        String sql = "SELECT id, nombre, nit FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("nit")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener cliente: " + e.getMessage(),e);
        }
        return null;
    }
}