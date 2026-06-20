package com.mediciones.gestor;

import com.mediciones.dao.TipoValvulaDAO;
import com.mediciones.model.TipoValvula;

import java.util.List;

public class TipoValvulaGestor {

    private final TipoValvulaDAO tipoValvulaDAO;

    public TipoValvulaGestor() {
        tipoValvulaDAO = new TipoValvulaDAO();
    }

    public List<TipoValvula> getAll() {
        return tipoValvulaDAO.getAll();
    }

    public boolean insertar(TipoValvula tipoValvula) {
        return  tipoValvulaDAO.insertar(tipoValvula);
    }

    public boolean actualizar(TipoValvula tipoValvula) {
        return  tipoValvulaDAO.actualizar(tipoValvula);
    }

    public boolean eliminar(Integer tipoValvulaId) {
        return  tipoValvulaDAO.eliminar(tipoValvulaId);
    }

    public boolean guardarOActualizar(TipoValvula tipoValvula) {
        return tipoValvulaDAO.guardarOActualizar(tipoValvula);
    }
}
