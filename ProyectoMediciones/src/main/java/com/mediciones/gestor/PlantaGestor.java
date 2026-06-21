package com.mediciones.gestor;

import com.mediciones.dao.ClienteDAO;
import com.mediciones.dao.PlantaDAO;
import com.mediciones.model.Cliente;
import com.mediciones.model.Planta;

import java.util.List;

public class PlantaGestor {

    private final PlantaDAO plantaDAO;
    private final ClienteDAO clienteDAO;
    private final ValvulaGestor valvulaGestor;

    public PlantaGestor() {
        plantaDAO = new PlantaDAO();
        clienteDAO = new ClienteDAO();
        valvulaGestor = new ValvulaGestor();
    }

    public Planta obtenerPlantaPorId(int idPlanta) {
        return plantaDAO.getPlantaById(idPlanta);
    }

    public boolean eliminarPlanta(int idPlanta) {
        return plantaDAO.deletePlantaById(idPlanta);
    }

    public boolean guardarOActualizarPlanta(Planta plantaAGuardar) {
        return plantaDAO.guardarPlanta(plantaAGuardar);
    }

    public List<Planta> obtenerTodasPlantasDeCliente(Cliente cliente) {
        return plantaDAO.getAllPlantasByCliente(cliente);
    }

    /**
     * Obtiene TODAS las plantas registradas y activas en el sistema,
     * sin importar a qué cliente pertenecen.
     * @return Lista de todas las plantas activas.
     */
    public List<Planta> obtenerTodasPlantas() {
        return plantaDAO.getAllPlantas();
    }

    public int contarValvulasAsociadas(int idPlanta) {
        return valvulaGestor.contarValvulasPorPlanta(idPlanta);
    }

    public int contarPorCliente(int idCliente) {
        return plantaDAO.contarPorCliente(idCliente);
    }
}
