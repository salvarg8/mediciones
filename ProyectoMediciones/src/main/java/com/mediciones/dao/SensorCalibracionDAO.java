package com.mediciones.dao;

import com.mediciones.model.SensorCalibracion;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DAO para manejar la calibración de sensores en MySQL.
 * Versión corregida para:
 * 1. Obtener ID generado correctamente (sin last_insert_rowid)
 * 2. Manejar fechas compatible con MySQL
 * 3. Mejorar manejo de valores nulos
 */
public class SensorCalibracionDAO {

    public SensorCalibracionDAO() {
        // La tabla se crea automáticamente en DatabaseManager
    }

    // Guardar nueva calibración
    public boolean guardarCalibracion(SensorCalibracion calibracion) {
        String sql = "INSERT INTO calibracion_sensores " +
                "(sensor_type, a1, c1, a2, c2, a3, c3, presion_conocida, voltaje_conocido, fecha_calibracion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conexion = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Establecer parámetros
            pstmt.setString(1, calibracion.getSensorType());
            setNullableDouble(pstmt, 2, calibracion.getA1());
            setNullableDouble(pstmt, 3, calibracion.getC1());
            setNullableDouble(pstmt, 4, calibracion.getA2());
            setNullableDouble(pstmt, 5, calibracion.getC2());
            setNullableDouble(pstmt, 6, calibracion.getA3());
            setNullableDouble(pstmt, 7, calibracion.getC3());
            setNullableDouble(pstmt, 8, calibracion.getPresionConocida());
            setNullableDouble(pstmt, 9, calibracion.getVoltajeConocido());

            // Manejar fecha compatible con MySQL
            if (calibracion.getFechaCalibracion() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                pstmt.setString(10, sdf.format(calibracion.getFechaCalibracion()));
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }

            // Ejecutar inserción
            int affectedRows = pstmt.executeUpdate();

            // ✅ CORRECCIÓN: Obtener ID generado para MySQL
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        calibracion.setId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error al guardar calibración: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Método auxiliar para manejar doubles nulos
    private void setNullableDouble(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.DOUBLE);
        } else {
            pstmt.setDouble(index, value);
        }
    }

    // Obtener la última calibración por tipo de sensor
    public SensorCalibracion obtenerUltimaCalibracionPorSensor(String sensorType) {
        String sql = "SELECT * FROM calibracion_sensores WHERE sensor_type = ? ORDER BY id DESC LIMIT 1";

        Connection conexion = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, sensorType);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                SensorCalibracion calibracion = new SensorCalibracion();
                calibracion.setId(rs.getLong("id"));
                calibracion.setSensorType(rs.getString("sensor_type"));

                // ✅ CORRECCIÓN: Manejo seguro de valores nulos
                calibracion.setA1(getNullableDouble(rs, "a1"));
                calibracion.setC1(getNullableDouble(rs, "c1"));
                calibracion.setA2(getNullableDouble(rs, "a2"));
                calibracion.setC2(getNullableDouble(rs, "c2"));
                calibracion.setA3(getNullableDouble(rs, "a3"));
                calibracion.setC3(getNullableDouble(rs, "c3"));
                calibracion.setPresionConocida(getNullableDouble(rs, "presion_conocida"));
                calibracion.setVoltajeConocido(getNullableDouble(rs, "voltaje_conocido"));

                // ✅ CORRECCIÓN: Manejo robusto de fechas
                String fechaStr = rs.getString("fecha_calibracion");
                if (fechaStr != null && !fechaStr.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        calibracion.setFechaCalibracion(sdf.parse(fechaStr));
                    } catch (Exception ex) {
                        System.err.println("Error al parsear fecha: " + fechaStr);
                        calibracion.setFechaCalibracion(new Date());
                    }
                }
                return calibracion;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener calibración: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Método auxiliar para obtener doubles nulos
    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    // Cerrar conexión (Ya lo maneja DatabaseManager)
    public void cerrarConexion() {
        // No necesario
    }
}