package com.mediciones.dao;

import com.mediciones.model.TipoValvula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipoValvulaDAO {

    private static final Logger logger = LoggerFactory.getLogger(TipoValvulaDAO.class);

    public List<TipoValvula> getAll() {
        String sql = "SELECT id, nombre, active FROM tipos_valvula WHERE active = true";
        List<TipoValvula> lista = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TipoValvula tipoValvula = new TipoValvula();
                tipoValvula.setId(rs.getInt("id"));
                tipoValvula.setNombre(rs.getString("nombre"));
                tipoValvula.setActive(rs.getBoolean("active"));
                lista.add(tipoValvula);
            }

        } catch (SQLException e) {
            logger.error("Error al obtener los tipos de válvulas" ,e);
        }
        return lista;
    }

    public boolean insertar(TipoValvula tipoValvula) {
        String sql = "INSERT INTO tipos_valvula (nombre) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tipoValvula.getNombre());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tipoValvula.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException ex) {
            logger.error("error al insertar un tipo de válvula: ", ex);
            return false;
        }
    }

    public boolean actualizar(TipoValvula tipoValvula) {
        String sql = "UPDATE tipos_valvula SET nombre = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipoValvula.getNombre());
            ps.setInt(2, tipoValvula.getId());
            return ps.executeUpdate() > 0;
        }  catch (SQLException ex) {
            logger.error("error al intentar actualizar un tipo de válvula: ", ex);
        }
        return false;
    }

    public boolean eliminar(Integer tipoValvulaId) {
        String sql = "UPDATE tipos_valvula SET active = false WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setInt(1, tipoValvulaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            logger.error("Error al realizar borado lógico con id: " +  tipoValvulaId, ex);
        }
        return false;
    }

    public boolean guardarOActualizar(TipoValvula tipoValvula) {
        if (tipoValvula.getId() == null) {
            return insertar(tipoValvula);
        } else {
            return actualizar(tipoValvula);
        }
    }
}
