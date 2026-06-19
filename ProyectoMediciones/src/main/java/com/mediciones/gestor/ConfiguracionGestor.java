package com.mediciones.gestor;

import com.mediciones.dao.ConfiguracionDAO;
import com.mediciones.model.Configuracion;

public class ConfiguracionGestor {

    private final ConfiguracionDAO dao =
            new ConfiguracionDAO();

    public Configuracion obtenerConfiguracion() {

        return dao.obtenerConfiguracion();

    }

    public boolean guardarConfiguracion(
            Configuracion configuracion) {

        return dao.guardarConfiguracion(
                configuracion);

    }

    public boolean existeConfiguracion() {
        return dao.obtenerConfiguracion() != null;
    }
}