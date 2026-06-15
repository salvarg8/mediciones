package com.mediciones.model;

/**
 * Entidad (Modelo) que representa a un Fluido.
 * **MODIFICADO: Solo ID y Nombre.**
 */
public class Fluido {
    private int id;
    private String nombre;
    // private String densidad; // ELIMINADO

    public Fluido() {}

    // Constructor completo
    public Fluido(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Constructor sin ID (útil para nuevos registros)
    public Fluido(String nombre) {
        this.nombre = nombre;
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

    @Override
    public String toString() {
        return nombre; // Esto hará que el JComboBox muestre el nombre del fluido
    }
}