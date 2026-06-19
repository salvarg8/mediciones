package com.mediciones.dao;

import com.mediciones.model.Cliente;
import com.mediciones.model.Planta;
import com.mediciones.model.Valvula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlantaDAO {

    private static final Logger logger = LoggerFactory.getLogger(PlantaDAO.class);


    public List<Planta> getAllPlantasByCliente(Cliente cliente) {
        Map<Integer, Planta> plantasMap = new LinkedHashMap<>();

        String sql = "SELECT p.id AS planta_id, p.nombre AS planta_nombre, " +
                "v.id AS valvula_id, v.tag AS valvula_tag " +
                "FROM plantas p " +
                "LEFT JOIN valvulas v ON v.planta_id = p.id AND v.active = true " +
                "WHERE p.cliente_id = ? AND p.active = true " +
                "ORDER BY p.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cliente.getId());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int plantaId = rs.getInt("planta_id");

                    String plantaNombre = rs.getString("planta_nombre");

                    Planta planta = plantasMap.computeIfAbsent(
                            plantaId,
                            id -> new Planta(
                                    id,
                                    plantaNombre,
                                    new ArrayList<>(),
                                    cliente
                            )
                    );

                    int valvulaId = rs.getInt("valvula_id");

                    if (!rs.wasNull()) {
                        Valvula valvula = new Valvula();
                        valvula.setId(valvulaId);
                        valvula.setTag(rs.getString("valvula_tag"));

                        planta.getValvulas().add(valvula);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Erro ao obter plantas de plantas de clientel Cliente ID : " + cliente.getId(), e);
        }

        return new ArrayList<>(plantasMap.values());
    }

    public boolean guardarPlanta(Planta plantaAGuardar) {
        String sql = "INSERT INTO Plantas (nombre,cliente_id) VALUES (?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plantaAGuardar.getNombre());
            ps.setInt(2, plantaAGuardar.getCliente().getId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        plantaAGuardar.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error al insertar Planta ", e);
             return false;
        }
    }

    public boolean deletePlantaById(int idPlanta) {
        // Borrado lógico: Actualizamos el estado 'active' a false en lugar de borrar el registro
        String sql = "UPDATE plantas SET active = false WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPlanta);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Retorna true si se actualizó al menos una fila exitosamente

        } catch (SQLException e) {
            logger.error("Error al realizar el borrado lógico de la planta con ID: " + idPlanta, e);
            return false;
        }
    }

    public Planta getPlantaById(int idPlanta) {
        // Buscamos la planta específica por ID, asegurándonos de que esté activa,
        // e incluimos sus válvulas activas usando un LEFT JOIN.
        String sql = "SELECT p.id AS planta_id, p.nombre AS planta_nombre, p.cliente_id, " +
                "v.id AS valvula_id, v.tag AS valvula_tag " +
                "FROM plantas p " +
                "LEFT JOIN valvulas v ON v.planta_id = p.id AND v.active = true " +
                "WHERE p.id = ? AND p.active = true";

        Planta planta = null;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPlanta);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    // Solo inicializamos el objeto Planta en la primera iteración
                    if (planta == null) {
                        // Creamos un Cliente "vacío" solo con su ID para mantener la relación del modelo
                        Cliente clienteAsociado = new Cliente();
                        clienteAsociado.setId(rs.getInt("cliente_id"));

                        planta = new Planta(
                                rs.getInt("planta_id"),
                                rs.getString("planta_nombre"),
                                new ArrayList<>(),
                                clienteAsociado
                        );
                    }

                    // Extraemos y agregamos las válvulas asociadas (si existen)
                    int valvulaId = rs.getInt("valvula_id");

                    if (!rs.wasNull()) {
                        Valvula valvula = new Valvula();
                        valvula.setId(valvulaId);
                        valvula.setTag(rs.getString("valvula_tag"));

                        planta.getValvulas().add(valvula);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error al consultar la planta con ID: " + idPlanta, e);
        }

        // Si la planta no existe o ya fue borrada lógicamente (active = false), retornará null.
        return planta;
    }

    public List<Planta> getAllPlantas() {
        Map<Integer, Planta> plantasMap = new LinkedHashMap<>();

        // Traemos todas las plantas activas, los datos básicos de su cliente,
        // y sus válvulas activas (si las tiene)
        String sql = "SELECT p.id AS planta_id, p.nombre AS planta_nombre, " +
                "c.id AS cliente_id, c.nombre AS cliente_nombre, " +
                "v.id AS valvula_id, v.tag AS valvula_tag " +
                "FROM plantas p " +
                "INNER JOIN clientes c ON p.cliente_id = c.id " +
                "LEFT JOIN valvulas v ON v.planta_id = p.id AND v.active = true " +
                "WHERE p.active = true " + // Filtro estricto: solo plantas activas
                "ORDER BY p.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int plantaId = rs.getInt("planta_id");

                // Extraemos las variables fuera de la función Lambda para evitar el error de SQLException
                String plantaNombre = rs.getString("planta_nombre");
                int clienteId = rs.getInt("cliente_id");
                String clienteNombre = rs.getString("cliente_nombre");

                Planta planta = plantasMap.computeIfAbsent(
                        plantaId,
                        id -> {
                            // Armamos un Cliente básico con los datos traídos del JOIN
                            Cliente clienteAsociado = new Cliente();
                            clienteAsociado.setId(clienteId);
                            clienteAsociado.setNombre(clienteNombre);

                            return new Planta(
                                    id,
                                    plantaNombre,
                                    new ArrayList<>(),
                                    clienteAsociado
                            );
                        }
                );

                int valvulaId = rs.getInt("valvula_id");

                // Si la planta tiene válvulas activas, las agregamos a su lista
                if (!rs.wasNull()) {
                    Valvula valvula = new Valvula();
                    valvula.setId(valvulaId);
                    valvula.setTag(rs.getString("valvula_tag"));

                    planta.getValvulas().add(valvula);
                }
            }

        } catch (SQLException e) {
            logger.error("Error al obtener todas las plantas activas", e);
        }

        return new ArrayList<>(plantasMap.values());
    }
}
