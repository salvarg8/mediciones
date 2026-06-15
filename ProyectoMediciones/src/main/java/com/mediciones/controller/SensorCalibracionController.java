package com.mediciones.controller;

import com.mediciones.dao.SensorCalibracionDAO;
import com.mediciones.model.SensorCalibracion;

public class SensorCalibracionController {

    private SensorCalibracionDAO sensorCalibracionDAO;

    public SensorCalibracionController() {
        // Inicializar el DAO. Ya no requiere try-catch porque la conexión 
        // se maneja centralizadamente en DatabaseManager.
        sensorCalibracionDAO = new SensorCalibracionDAO();
    }

    // Guardar nueva calibración
    public boolean guardarCalibracion(SensorCalibracion calibracion) {
        return sensorCalibracionDAO.guardarCalibracion(calibracion);
    }

    // Obtener la última calibración por tipo de sensor
    public SensorCalibracion obtenerUltimaCalibracionPorSensor(String sensorType) {
        return sensorCalibracionDAO.obtenerUltimaCalibracionPorSensor(sensorType);
    }

    // Cerrar conexión (opcional, el DatabaseManager lo hace al salir)
    public void cerrarConexion() {
        sensorCalibracionDAO.cerrarConexion();
    }
}