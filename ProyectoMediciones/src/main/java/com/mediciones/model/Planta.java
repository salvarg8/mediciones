package com.mediciones.model;

import java.util.ArrayList;
import java.util.List;

public class Planta {

    private Integer id;
    private String nombre;
    private List<Valvula> valvulas;
    private Cliente cliente;

    public Planta() {
        valvulas = new ArrayList<>();
    }

    public Planta(Integer id, String nombre, List<Valvula> valvulas, Cliente cliente) {
        this.id = id;
        this.nombre = nombre;
        this.valvulas = valvulas;
        this.cliente = cliente;
    }

    // ======= Getters & Setters =======

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

    public List<Valvula> getValvulas() {
        return valvulas;
    }

    public void setValvulas(List<Valvula> valvulas) {
        this.valvulas = valvulas;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
