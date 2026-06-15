package com.mediciones.dao;

import com.mediciones.model.Valvula;
import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de Acceso a Datos (DAO) para la entidad Valvula.
 * Versión corregida para Java 19 + MySQL.
 */
public class ValvulaDAO {

    // --- Método INSERTAR (CORREGIDO PARA MYSQL) ---

    /**
     * Inserta una nueva válvula en la base de datos.
     * @param valvula La válvula a insertar.
     * @return true si la inserción fue exitosa.
     */
    public boolean insertar(Valvula valvula) {
        String insertSql = "INSERT INTO valvulas(cliente_id, fluido_servicio_id, tag, numero_serie, lugar_conexion, " +
                "marca, material_cuerpo, entrada_rosca_tipo, entrada_brida_diametro, entrada_brida_serie, " +
                "salida_rosca_tipo, salida_brida_diametro, salida_brida_serie) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            // Asignar parámetros con manejo seguro de NULLs
            setNullableInt(pstmt, 1, (valvula.getCliente() != null) ? valvula.getCliente().getId() : null);
            setNullableInt(pstmt, 2, (valvula.getFluido() != null) ? valvula.getFluido().getId() : null);
            pstmt.setString(3, valvula.getTag());
            pstmt.setString(4, valvula.getNumeroSerie());
            pstmt.setString(5, valvula.getLugarConexion());
            pstmt.setString(6, valvula.getMarca());
            pstmt.setString(7, valvula.getMaterialCuerpo());
            pstmt.setString(8, valvula.getEntradaRoscaTipo());
            pstmt.setString(9, valvula.getEntradaBridaDiametro());
            pstmt.setString(10, valvula.getEntradaBridaSerie());
            pstmt.setString(11, valvula.getSalidaRoscaTipo());
            pstmt.setString(12, valvula.getSalidaBridaDiametro());
            pstmt.setString(13, valvula.getSalidaBridaSerie());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // ✅ CORRECCIÓN: Usar getGeneratedKeys() (compatible con MySQL)
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

    // Método auxiliar para manejar integers nulos
    private void setNullableInt(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.INTEGER);
        } else {
            pstmt.setInt(index, value);
        }
    }

    // --- Método ACTUALIZAR (CORREGIDO) ---

    /**
     * Actualiza una válvula existente en la base de datos.
     * @param valvula La válvula a actualizar.
     * @return true si la actualización fue exitosa.
     */
    public boolean actualizar(Valvula valvula) {
        String sql = "UPDATE valvulas SET cliente_id = ?, fluido_servicio_id = ?, tag = ?, numero_serie = ?, lugar_conexion = ?, " +
                "marca = ?, material_cuerpo = ?, entrada_rosca_tipo = ?, entrada_brida_diametro = ?, entrada_brida_serie = ?, " +
                "salida_rosca_tipo = ?, salida_brida_diametro = ?, salida_brida_serie = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Asignar parámetros con manejo seguro de NULLs
            setNullableInt(pstmt, 1, (valvula.getCliente() != null) ? valvula.getCliente().getId() : null);
            setNullableInt(pstmt, 2, (valvula.getFluido() != null) ? valvula.getFluido().getId() : null);
            pstmt.setString(3, valvula.getTag());
            pstmt.setString(4, valvula.getNumeroSerie());
            pstmt.setString(5, valvula.getLugarConexion());
            pstmt.setString(6, valvula.getMarca());
            pstmt.setString(7, valvula.getMaterialCuerpo());
            pstmt.setString(8, valvula.getEntradaRoscaTipo());
            pstmt.setString(9, valvula.getEntradaBridaDiametro());
            pstmt.setString(10, valvula.getEntradaBridaSerie());
            pstmt.setString(11, valvula.getSalidaRoscaTipo());
            pstmt.setString(12, valvula.getSalidaBridaDiametro());
            pstmt.setString(13, valvula.getSalidaBridaSerie());
            pstmt.setInt(14, valvula.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar válvula: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Método ELIMINAR (CORREGIDO) ---

    /**
     * Elimina una válvula de la base de datos.
     * @param id El ID de la válvula a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM valvulas WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar válvula: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Método OBTENER TODAS LAS VÁLVULAS (CORREGIDO) ---

    /**
     * Obtiene todas las válvulas registradas en la base de datos.
     * @return Una lista de objetos Valvula.
     */
    public List<Valvula> obtenerTodas() {
        List<Valvula> valvulas = new ArrayList<>();
        String sql = "SELECT v.id, c.id AS cliente_id, c.nombre AS cliente_nombre, f.id AS fluido_servicio_id, " +
                "f.nombre AS fluido_servicio_nombre, v.tag, v.numero_serie, v.lugar_conexion, v.marca, " +
                "v.material_cuerpo, v.entrada_rosca_tipo, v.entrada_brida_diametro, v.entrada_brida_serie, " +
                "v.salida_rosca_tipo, v.salida_brida_diametro, v.salida_brida_serie " +
                "FROM valvulas v " +
                "LEFT JOIN clientes c ON v.cliente_id = c.id " +
                "LEFT JOIN fluidos f ON v.fluido_servicio_id = f.id";

        Connection conn = DatabaseManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                valvulas.add(crearValvulaDesdeResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener todas las válvulas: " + e.getMessage());
            e.printStackTrace();
        }

        return valvulas;
    }

    // --- Método OBTENER VÁLVULA POR ID (CORREGIDO) ---

    /**
     * Obtiene una válvula específica por su ID.
     * @param id El ID de la válvula a buscar.
     * @return La válvula encontrada, o null si no existe.
     */
    public Valvula obtenerPorId(int id) {
        String sql = "SELECT v.id, c.id AS cliente_id, c.nombre AS cliente_nombre, f.id AS fluido_servicio_id, " +
                "f.nombre AS fluido_servicio_nombre, v.tag, v.numero_serie, v.lugar_conexion, v.marca, " +
                "v.material_cuerpo, v.entrada_rosca_tipo, v.entrada_brida_diametro, v.entrada_brida_serie, " +
                "v.salida_rosca_tipo, v.salida_brida_diametro, v.salida_brida_serie " +
                "FROM valvulas v " +
                "LEFT JOIN clientes c ON v.cliente_id = c.id " +
                "LEFT JOIN fluidos f ON v.fluido_servicio_id = f.id " +
                "WHERE v.id = ?";
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // --- Método PARA VERIFICAR DUPLICADOS (CORREGIDO) ---

    /**
     * Verifica si ya existe una válvula con los mismos valores de cliente, TAG y lugar de conexión.
     * @param clienteId El ID del cliente.
     * @param tag El TAG de la válvula.
     * @param lugarConexion El lugar de conexión.
     * @return true si existe una duplicada, false en caso contrario.
     */
    public boolean existeDuplicada(int clienteId, String tag, String lugarConexion) {
        String sql = "SELECT COUNT(*) AS count FROM valvulas WHERE cliente_id = ? AND tag = ? AND lugar_conexion = ?";
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // --- Métodos de ayuda para consultas específicas ---

    /**
     * Obtiene todas las válvulas asociadas a un cliente específico.
     * @param idCliente El ID del cliente.
     * @return Una lista de objetos Valvula filtrados por cliente.
     */
    public List<Valvula> obtenerPorCliente(int idCliente) {
        List<Valvula> valvulas = new ArrayList<>();
        String sql = "SELECT v.id, c.id AS cliente_id, c.nombre AS cliente_nombre, f.id AS fluido_servicio_id, " +
                "f.nombre AS fluido_servicio_nombre, v.tag, v.numero_serie, v.lugar_conexion, v.marca, " +
                "v.material_cuerpo, v.entrada_rosca_tipo, v.entrada_brida_diametro, v.entrada_brida_serie, " +
                "v.salida_rosca_tipo, v.salida_brida_diametro, v.salida_brida_serie " +
                "FROM valvulas v " +
                "LEFT JOIN clientes c ON v.cliente_id = c.id " +
                "LEFT JOIN fluidos f ON v.fluido_servicio_id = f.id " +
                "WHERE v.cliente_id = ?";

        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    /**
     * Obtiene una válvula específica por su TAG.
     * @param tag El TAG de la válvula a buscar.
     * @return La válvula encontrada, o null si no existe.
     */
    public Valvula obtenerPorTag(String tag) {
        String sql = "SELECT v.id, c.id AS cliente_id, c.nombre AS cliente_nombre, f.id AS fluido_servicio_id, " +
                "f.nombre AS fluido_servicio_nombre, v.tag, v.numero_serie, v.lugar_conexion, v.marca, " +
                "v.material_cuerpo, v.entrada_rosca_tipo, v.entrada_brida_diametro, v.entrada_brida_serie, " +
                "v.salida_rosca_tipo, v.salida_brida_diametro, v.salida_brida_serie " +
                "FROM valvulas v " +
                "LEFT JOIN clientes c ON v.cliente_id = c.id " +
                "LEFT JOIN fluidos f ON v.fluido_servicio_id = f.id " +
                "WHERE v.tag = ?";
        Connection conn = DatabaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    // --- Métodos auxiliares para obtener datos relacionales ---

    /**
     * Obtiene todos los clientes registrados.
     * @return Una lista de objetos Cliente.
     */
    public List<Cliente> obtenerTodosClientes() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT id, nombre FROM clientes";

        Connection conn = DatabaseManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        null
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener todos los clientes: " + e.getMessage());
            e.printStackTrace();
        }

        return clientes;
    }

    /**
     * Obtiene todos los fluidos de servicio registrados.
     * @return Una lista de objetos Fluido.
     */
    public List<Fluido> obtenerTodosFluidos() {
        List<Fluido> fluidos = new ArrayList<>();
        String sql = "SELECT id, nombre FROM fluidos";

        Connection conn = DatabaseManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                fluidos.add(new Fluido(
                        rs.getInt("id"),
                        rs.getString("nombre")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener todos los fluidos: " + e.getMessage());
            e.printStackTrace();
        }

        return fluidos;
    }

    // --- Método auxiliar para crear objetos Valvula desde ResultSet ---

    /**
     * Crea un objeto Valvula desde un ResultSet.
     * @param rs El ResultSet con los datos de la válvula.
     * @return Un objeto Valvula completo.
     * @throws SQLException Si ocurre un error al procesar el ResultSet.
     */
    private Valvula crearValvulaDesdeResultSet(ResultSet rs) throws SQLException {
        Valvula valvula = new Valvula();
        valvula.setId(rs.getInt("id"));

        // ✅ CORRECCIÓN: Manejo correcto de wasNull()
        int clienteId = rs.getInt("cliente_id");
        if (!rs.wasNull()) {
            valvula.setCliente(new Cliente(clienteId, rs.getString("cliente_nombre"), null));
        }

        int fluidoId = rs.getInt("fluido_servicio_id");
        if (!rs.wasNull()) {
            valvula.setFluido(new Fluido(fluidoId, rs.getString("fluido_servicio_nombre")));
        }

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
}