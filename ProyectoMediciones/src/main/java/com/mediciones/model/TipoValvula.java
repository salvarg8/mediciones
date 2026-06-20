package com.mediciones.model;

public class TipoValvula {

    private Integer id;
    private String nombre;
    private Boolean active;

    public TipoValvula(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public TipoValvula() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
