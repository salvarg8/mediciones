package com.mediciones.dao;

import com.mediciones.model.Configuracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracionDAO {

    public Configuracion obtenerConfiguracion() {

        String sql =
                "SELECT id, origen_datos, ruta_archivo " +
                        "FROM configuracion WHERE id = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                return new Configuracion(
                        rs.getInt("id"),
                        rs.getString("origen_datos"),
                        rs.getString("ruta_archivo")
                );

            }

        } catch (Exception ex) {

            ex.printStackTrace();

        }

        return null;
    }

    public boolean guardarConfiguracion(Configuracion configuracion) {

        String sql =
                "INSERT INTO configuracion " +
                        "(id, origen_datos, ruta_archivo) " +
                        "VALUES (1, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "origen_datos = VALUES(origen_datos), " +
                        "ruta_archivo = VALUES(ruta_archivo)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, configuracion.getOrigenDatos());
            ps.setString(2, configuracion.getRutaArchivo());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("Error al guardar configuración: " + e.getMessage());
            e.printStackTrace();

            return false;
        }
    }

}