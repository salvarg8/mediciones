// Contenido de com.mediciones.model.Cliente
package com.mediciones.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad (Modelo) que representa a un Cliente.
 */
public class Cliente {
    private int id;
    private String nombre;
    private String nit; // Número de Identificación Tributaria o Cédula
    private List<Planta> plantas;
    private List<Valvula> valvulas;
    private Boolean activo;

    public Cliente() {
    }

    public Cliente(int id, String nombre, String nit, List<Planta> plantas, List<Valvula> valvulas) {
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
        this.plantas = plantas;
        this.valvulas = valvulas;
    }

    // Constructor completo
    public Cliente(int id, String nombre, String nit) {
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
        plantas = new ArrayList<>();
        valvulas = new ArrayList<>();
    }

    // Constructor sin ID (útil para nuevos registros)
    public Cliente(String nombre, String nit) {
        this.nombre = nombre;
        this.nit = nit;
        plantas = new ArrayList<>();
        valvulas = new ArrayList<>();
    }

    // --- Getters y Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public List<Planta> getPlantas() { return plantas; }

    public void setPlantas(List<Planta> plantas) { this.plantas = plantas; }

    public List<Valvula> getValvulas() { return valvulas; }

    public void setValvulas(List<Valvula> valvulas) { this.valvulas = valvulas; }

    public Boolean getActivo() { return activo; }

    public void setActivo(Boolean activo) { this.activo = activo; }

    /**
     * SOBREESCRITO: Muestra el nombre del cliente en el JComboBox.
     */
    @Override
    public String toString() {
        return this.nombre;
    }
}