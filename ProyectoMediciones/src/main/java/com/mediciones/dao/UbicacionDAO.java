package com.mediciones.dao;

import com.mediciones.model.Ubicacion;
import java.sql.*;

/**
 * DAO para manejar la ubicación en MySQL.
 * Versión corregida para:
 * 1. Crear tabla compatible con MySQL (no SQLite)
 * 2. Eliminar lógica redundante (la tabla ya se crea en DatabaseManager)
 * 3. Mejorar manejo de transacciones
 */
public class UbicacionDAO {

    public boolean guardar(Ubicacion u) {
        String deleteSql = "DELETE FROM ubicacion";
        String insertSql = "INSERT INTO ubicacion (ubicacion) VALUES (?)";

        Connection conn = DatabaseManager.getConnection();

        try {
            // ✅ CORRECCIÓN: Usar transacción para atomicidad
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

                // 1. Borrar lo anterior (si existe)
                stmt.executeUpdate(deleteSql);

                // 2. Insertar la nueva ubicación
                pstmt.setString(1, u.getUbicacion());
                pstmt.executeUpdate();

                conn.commit();
                System.out.println("✅ Ubicación guardada en BD: " + u.getUbicacion());
                return true;
            }
        } catch (SQLException e) {
            // ✅ CORRECCIÓN: Rollback en caso de error
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }

            System.err.println("❌ Error al guardar ubicación: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restablecer autocommit: " + e.getMessage());
            }
        }
    }

    public Ubicacion obtener() {
        String sql = "SELECT ubicacion FROM ubicacion LIMIT 1";
        Connection conn = DatabaseManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Ubicacion u = new Ubicacion();
                u.setUbicacion(rs.getString("ubicacion"));
                return u;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al obtener ubicación: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}