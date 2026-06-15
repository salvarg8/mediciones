package com.mediciones.controller;

import com.mediciones.dao.UbicacionDAO;
import com.mediciones.model.Ubicacion;

/**
 * Controlador para la lógica de negocio relacionada con la ubicación
 * del archivo Excel utilizado por ExcelGenerator.
 * Actúa como intermediario entre la Vista y el DAO (UbicacionDAO).
 */
public class UbicacionController {

    private UbicacionDAO dao = new UbicacionDAO();

    public boolean guardarUbicacion(String ruta) {
        Ubicacion u = new Ubicacion();
        u.setUbicacion(ruta);
        return dao.guardar(u);
    }

    public Ubicacion obtenerUbicacion() {
        return dao.obtener();
    }
}
