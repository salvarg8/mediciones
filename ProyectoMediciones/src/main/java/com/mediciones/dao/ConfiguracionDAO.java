package com.mediciones.dao;

import com.mediciones.model.Configuracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracionDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionDAO.class);


    public Configuracion obtenerConfiguracion() {

        String sql =
                "SELECT id, origen_datos, ruta_archivo, puerto_com_default, ruta_plantilla_excel " +
                        "FROM configuracion WHERE id = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                return new Configuracion(
                        rs.getInt("id"),
                        rs.getString("origen_datos"),
                        rs.getString("ruta_archivo"),
                        rs.getString("puerto_com_default"),
                        rs.getString("ruta_plantilla_excel")
                );

            }

        } catch (Exception ex) {
            logger.error("Error al obtener configuracion", ex);
        }
        return null;
    }

    public boolean guardarConfiguracion(Configuracion configuracion) {

        String sql =
                "INSERT INTO configuracion " +
                        "(id, origen_datos, ruta_archivo, puerto_com_default, ruta_plantilla_excel) " +
                        "VALUES (1, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "origen_datos = VALUES(origen_datos), " +
                        "ruta_archivo = VALUES(ruta_archivo), " +
                        "puerto_com_default = VALUES(puerto_com_default), " +
                        "ruta_plantilla_excel = VALUES(ruta_plantilla_excel)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, configuracion.getOrigenDatos());
            ps.setString(2, configuracion.getRutaArchivo());
            ps.setString(3, configuracion.getPuertoComDefault());
            ps.setString(4,configuracion.getRutaPlantillaExcel());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            logger.error("Error al guardarConfiguracion", e);

            return false;
        }
    }

}
