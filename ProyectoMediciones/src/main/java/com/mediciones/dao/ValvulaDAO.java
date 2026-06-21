package com.mediciones.dao;

import com.mediciones.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de Acceso a Datos (DAO) para la entidad Valvula.
 * Actualizado para soportar relación con Planta y Borrado Lógico.
 */
public class ValvulaDAO {

    private static final String BASE_SELECT_SQL =
            "SELECT v.id, v.tag, v.numero_serie, v.lugar_conexion, v.marca, " +
                    "v.material_cuerpo, v.entrada_rosca_tipo, v.entrada_brida_diametro, v.entrada_brida_serie, " +
                    "v.salida_rosca_tipo, v.salida_brida_diametro, v.salida_brida_serie, " +
                    "p.id AS planta_id, p.nombre AS planta_nombre, " +
                    "c.id AS cliente_id, c.nombre AS cliente_nombre, " +
                    "f.id AS fluido_servicio_id, f.nombre AS fluido_servicio_nombre, " +
                    "tv.id AS tipo_valvula_id, tv.nombre AS tipo_valvula_nombre " + // <--- NUEVO
                    "FROM valvulas v " +
                    "LEFT JOIN plantas p ON v.planta_id = p.id " +
                    "LEFT JOIN clientes c ON v.cliente_id = c.id " +
                    "LEFT JOIN fluidos f ON v.fluido_servicio_id = f.id " +
                    "LEFT JOIN tipos_valvula tv ON v.tipo_valvula_id = tv.id " + // <--- NUEVO
                    "WHERE v.active = true ";

    public boolean insertar(Valvula valvula) {
        String sql = "INSERT INTO valvulas (cliente_id, planta_id, fluido_servicio_id, tipo_valvula_id, tag, numero_serie, " +
                "lugar_conexion, marca, material_cuerpo, entrada_rosca_tipo, entrada_brida_diametro, " +
                "entrada_brida_serie, salida_rosca_tipo, salida_brida_diametro, salida_brida_serie) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Protección de consistencia: Extraemos cliente_id directamente de la planta asignada
            int clienteId = valvula.getPlanta().getCliente().getId();
            pstmt.setInt(1, clienteId);
            pstmt.setInt(2, valvula.getPlanta().getId());

            setNullableInt(pstmt, 3, (valvula.getFluido() != null) ? valvula.getFluido().getId() : null);
            setNullableInt(pstmt, 4, (valvula.getTipoValvula() != null) ? valvula.getTipoValvula().getId() : null);

            pstmt.setString(5, valvula.getTag());
            pstmt.setString(6, valvula.getNumeroSerie());
            pstmt.setString(7, valvula.getLugarConexion());
            pstmt.setString(8, valvula.getMarca());
            pstmt.setString(9, valvula.getMaterialCuerpo());
            pstmt.setString(10, valvula.getEntradaRoscaTipo());
            pstmt.setString(11, valvula.getEntradaBridaDiametro());
            pstmt.setString(12, valvula.getEntradaBridaSerie());
            pstmt.setString(13, valvula.getSalidaRoscaTipo());
            pstmt.setString(14, valvula.getSalidaBridaDiametro());
            pstmt.setString(15, valvula.getSalidaBridaSerie());


            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        valvula.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error al insertar válvula: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean actualizar(Valvula valvula) {
        String sql = "UPDATE valvulas SET cliente_id = ?, planta_id = ?, fluido_servicio_id = ?, tipo_valvula_id = ?, tag = ?, " +
                "numero_serie = ?, lugar_conexion = ?, marca = ?, material_cuerpo = ?, entrada_rosca_tipo = ?, " +
                "entrada_brida_diametro = ?, entrada_brida_serie = ?, salida_rosca_tipo = ?, " +
                "salida_brida_diametro = ?, salida_brida_serie = ? WHERE id = ? AND active = true";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Mantenemos consistencia con la planta
            pstmt.setInt(1, valvula.getPlanta().getCliente().getId());
            pstmt.setInt(2, valvula.getPlanta().getId());

            setNullableInt(pstmt, 3, (valvula.getFluido() != null) ? valvula.getFluido().getId() : null);
            setNullableInt(pstmt, 4, (valvula.getTipoValvula() != null) ? valvula.getTipoValvula().getId() : null);


            pstmt.setString(5, valvula.getTag());
            pstmt.setString(6, valvula.getNumeroSerie());
            pstmt.setString(7, valvula.getLugarConexion());
            pstmt.setString(8, valvula.getMarca());
            pstmt.setString(9, valvula.getMaterialCuerpo());
            pstmt.setString(10, valvula.getEntradaRoscaTipo());
            pstmt.setString(11, valvula.getEntradaBridaDiametro());
            pstmt.setString(12, valvula.getEntradaBridaSerie());
            pstmt.setString(13, valvula.getSalidaRoscaTipo());
            pstmt.setString(14, valvula.getSalidaBridaDiametro());
            pstmt.setString(15, valvula.getSalidaBridaSerie());
            pstmt.setInt(16, valvula.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar válvula: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean eliminar(int id) {
        // En lugar de DELETE, cambiamos active a false
        String sql = "UPDATE valvulas SET active = false WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar (lógico) válvula: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Métodos de LECTURA ---

    public List<Valvula> obtenerTodas() {
        List<Valvula> valvulas = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(BASE_SELECT_SQL)) {

            while (rs.next()) {
                valvulas.add(crearValvulaDesdeResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todas las válvulas: " + e.getMessage());
            e.printStackTrace();
        }
        return valvulas;
    }

    public Valvula obtenerPorId(int id) {
        String sql = BASE_SELECT_SQL + " AND v.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return crearValvulaDesdeResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener válvula por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<Valvula> obtenerPorCliente(int idCliente) {
        List<Valvula> valvulas = new ArrayList<>();
        String sql = BASE_SELECT_SQL + " AND v.cliente_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    valvulas.add(crearValvulaDesdeResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener válvulas por cliente: " + e.getMessage());
            e.printStackTrace();
        }
        return valvulas;
    }

    public List<Valvula> getByPlantaId(int idPlanta) {
        List<Valvula> valvulas = new ArrayList<>();
        String sql = BASE_SELECT_SQL + " AND v.planta_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idPlanta);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    valvulas.add(crearValvulaDesdeResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener válvulas por Planta ID: " + e.getMessage());
            e.printStackTrace();
        }
        return valvulas;
    }

    public Valvula obtenerPorTag(String tag) {
        String sql = BASE_SELECT_SQL + " AND v.tag = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tag);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return crearValvulaDesdeResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener válvula por TAG: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean existeDuplicada(int clienteId, String tag, String lugarConexion) {
        String sql = "SELECT COUNT(*) AS count FROM valvulas WHERE cliente_id = ? AND tag = ? AND lugar_conexion = ? AND active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clienteId);
            pstmt.setString(2, tag);
            pstmt.setString(3, lugarConexion);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar duplicados: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // --- Métodos de armado y auxiliares ---

    private Valvula crearValvulaDesdeResultSet(ResultSet rs) throws SQLException {
        Valvula valvula = new Valvula();
        valvula.setId(rs.getInt("id"));

        // 1. Armamos el Cliente
        Cliente cliente = null;
        int clienteId = rs.getInt("cliente_id");
        if (!rs.wasNull()) {
            cliente = new Cliente(clienteId, rs.getString("cliente_nombre"), null);
            valvula.setCliente(cliente);
        }

        // 2. Armamos la Planta y le asociamos el Cliente
        int plantaId = rs.getInt("planta_id");
        if (!rs.wasNull()) {
            Planta planta = new Planta(plantaId, rs.getString("planta_nombre"), new ArrayList<>(), cliente);
            valvula.setPlanta(planta);
        }

        // 3. Armamos el Fluido
        int fluidoId = rs.getInt("fluido_servicio_id");
        if (!rs.wasNull()) {
            valvula.setFluido(new Fluido(fluidoId, rs.getString("fluido_servicio_nombre")));
        }

        // 4. Armamos el Tipo de Válvula
        int tipoId = rs.getInt("tipo_valvula_id");
        if (!rs.wasNull()) {
            TipoValvula tipo = new TipoValvula(tipoId, rs.getString("tipo_valvula_nombre"));
            valvula.setTipoValvula(tipo);
        }

        // 5. Mapeo de datos planos
        valvula.setTag(rs.getString("tag"));
        valvula.setNumeroSerie(rs.getString("numero_serie"));
        valvula.setLugarConexion(rs.getString("lugar_conexion"));
        valvula.setMarca(rs.getString("marca"));
        valvula.setMaterialCuerpo(rs.getString("material_cuerpo"));
        valvula.setEntradaRoscaTipo(rs.getString("entrada_rosca_tipo"));
        valvula.setEntradaBridaDiametro(rs.getString("entrada_brida_diametro"));
        valvula.setEntradaBridaSerie(rs.getString("entrada_brida_serie"));
        valvula.setSalidaRoscaTipo(rs.getString("salida_rosca_tipo"));
        valvula.setSalidaBridaDiametro(rs.getString("salida_brida_diametro"));
        valvula.setSalidaBridaSerie(rs.getString("salida_brida_serie"));

        return valvula;
    }

    /**
     * Elimina (borrado lógico) todas las válvulas asociadas a un cliente.
     * Utiliza una conexión externa para participar en una transacción.
     */
    public void eliminarPorCliente(int idCliente, Connection conn) throws SQLException {
        String sql = "UPDATE valvulas SET active = false WHERE cliente_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idCliente);
            pstmt.executeUpdate();
        }
    }
    /**
     * Elimina (borrado lógico) todas las válvulas asociadas a una planta específica.
     * Utiliza una conexión externa compartida para mantener la integridad de la transacción.
     */
    public void eliminarPorPlanta(int idPlanta, Connection conn) throws SQLException {
        String sql = "UPDATE valvulas SET active = false WHERE planta_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idPlanta);
            pstmt.executeUpdate();
        }
    }

    // Cuenta cuántas válvulas activas tiene una planta
    public int contarPorPlanta(int idPlanta) {
        String sql = "SELECT COUNT(*) FROM valvulas WHERE planta_id = ? AND active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlanta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar válvulas por planta: " + e.getMessage());
        }
        return 0;
    }

    // Cuenta cuántas válvulas activas tiene un cliente en total
    public int contarPorCliente(int idCliente) {
        String sql = "SELECT COUNT(*) FROM valvulas WHERE cliente_id = ? AND active = true";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar válvulas por cliente: " + e.getMessage());
        }
        return 0;
    }

    private void setNullableInt(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.INTEGER);
        } else {
            pstmt.setInt(index, value);
        }
    }

}