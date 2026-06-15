// Contenido de com.mediciones.model.Cliente
package com.mediciones.model;

/**
 * Entidad (Modelo) que representa a un Cliente.
 */
public class Cliente {
    private int id;
    private String nombre;
    private String nit; // Número de Identificación Tributaria o Cédula

    // Constructor completo
    public Cliente(int id, String nombre, String nit) {
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
    }

    // Constructor sin ID (útil para nuevos registros)
    public Cliente(String nombre, String nit) {
        this.nombre = nombre;
        this.nit = nit;
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

    /**
     * SOBREESCRITO: Muestra el nombre del cliente en el JComboBox.
     */
    @Override
    public String toString() {
        return this.nombre;
    }
}