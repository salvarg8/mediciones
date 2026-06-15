package com.mediciones.model;

/**
 * Entidad de dominio para la tabla Operador.
 */
public class Operador {

    private int id;
    private String nombre;
    private String identificacion; 

    // Constructor para crear un nuevo operador (sin ID)
    public Operador(String nombre, String identificacion) {
        this.nombre = nombre;
        this.identificacion = identificacion;
    }

    // Constructor para recuperar un operador existente (con ID)
    public Operador(int id, String nombre, String identificacion) {
        this.id = id;
        this.nombre = nombre;
        this.identificacion = identificacion;
    }

    // Getters y Setters (Corrigen los errores de 'cannot find symbol')
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

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    @Override
    public String toString() {
        return nombre; // Esto hará que el JComboBox muestre el nombre del operador
    }
}