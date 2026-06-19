package com.mediciones.dao;

import com.mediciones.model.Ubicacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO para manejar la ubicación en MySQL.
 * Actualizado con la función UPSERT (ON DUPLICATE KEY UPDATE) de MySQL
 * y manejo de logs mediante SLF4J.
 */
public class UbicacionDAO {

    private static final Logger logger = LoggerFactory.getLogger(UbicacionDAO.class);

    public boolean guardar(Ubicacion u) {
        String sql = "INSERT INTO ubicacion (id, ubicacion) VALUES (1, ?) " +
                "ON DUPLICATE KEY UPDATE ubicacion = VALUES(ubicacion)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, u.getUbicacion());

            pstmt.executeUpdate();

            logger.info("Ubicación guardada/actualizada con éxito: {}", u.getUbicacion());
            return true;

        } catch (SQLException e) {
            logger.error("Error al guardar la ubicación: " + e.getMessage(), e);
            return false;
        }
    }

    public Ubicacion obtener() {
        String sql = "SELECT ubicacion FROM ubicacion WHERE id = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                Ubicacion u = new Ubicacion();
                u.setUbicacion(rs.getString("ubicacion"));
                return u;
            }

        } catch (SQLException e) {
            logger.error("Error al obtener la ubicación: " + e.getMessage(), e);
        }
        return null;
    }
}