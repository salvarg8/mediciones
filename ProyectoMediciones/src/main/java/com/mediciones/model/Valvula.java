package com.mediciones.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Entidad (Modelo) que representa una Válvula.
 */
public class Valvula implements Serializable {
    private static final long serialVersionUID = 1L;

    // Atributos principales
    private int id;
    private Cliente cliente;
    private Fluido fluidoServicio;
    private String tag;
    private String numeroSerie;
    private String lugarConexion;
    private String marca;
    private String materialCuerpo;
    private String entradaRoscaTipo;
    private String entradaBridaDiametro;
    private String entradaBridaSerie;
    private String salidaRoscaTipo;
    private String salidaBridaDiametro;
    private String salidaBridaSerie;
    private Planta planta;

    // Constructor completo
    public Valvula(int id, Cliente cliente, Fluido fluidoServicio, String tag, String numeroSerie,
                   String lugarConexion, String marca, String materialCuerpo, String entradaRoscaTipo,
                   String entradaBridaDiametro, String entradaBridaSerie, String salidaRoscaTipo,
                   String salidaBridaDiametro, String salidaBridaSerie, Planta planta) {
        this.id = id;
        this.cliente = cliente;
        this.fluidoServicio = fluidoServicio;
        this.tag = tag;
        this.numeroSerie = numeroSerie;
        this.lugarConexion = lugarConexion;
        this.marca = marca;
        this.materialCuerpo = materialCuerpo;
        this.entradaRoscaTipo = entradaRoscaTipo;
        this.entradaBridaDiametro = entradaBridaDiametro;
        this.entradaBridaSerie = entradaBridaSerie;
        this.salidaRoscaTipo = salidaRoscaTipo;
        this.salidaBridaDiametro = salidaBridaDiametro;
        this.salidaBridaSerie = salidaBridaSerie;
        this.planta =  planta;
    }

    // Constructor sin ID (útil para nuevos registros)
    public Valvula(Cliente cliente, Fluido fluidoServicio, String tag, String numeroSerie,
                   String lugarConexion, String marca, String materialCuerpo, String entradaRoscaTipo,
                   String entradaBridaDiametro, String entradaBridaSerie, String salidaRoscaTipo,
                   String salidaBridaDiametro, String salidaBridaSerie, Planta planta) {
        this.cliente = cliente;
        this.fluidoServicio = fluidoServicio;
        this.tag = tag;
        this.numeroSerie = numeroSerie;
        this.lugarConexion = lugarConexion;
        this.marca = marca;
        this.materialCuerpo = materialCuerpo;
        this.entradaRoscaTipo = entradaRoscaTipo;
        this.entradaBridaDiametro = entradaBridaDiametro;
        this.entradaBridaSerie = entradaBridaSerie;
        this.salidaRoscaTipo = salidaRoscaTipo;
        this.salidaBridaDiametro = salidaBridaDiametro;
        this.salidaBridaSerie = salidaBridaSerie;
        this.planta = planta;
    }

    // Constructor vacío (útil para inicialización manual o frameworks ORM como Hibernate)
    public Valvula() {}

    // --- Getters y Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Fluido getFluido() {
        return fluidoServicio;
    }

    public void setFluido(Fluido fluidoServicio) {
        this.fluidoServicio = fluidoServicio;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getLugarConexion() {
        return lugarConexion;
    }

    public void setLugarConexion(String lugarConexion) {
        this.lugarConexion = lugarConexion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getMaterialCuerpo() {
        return materialCuerpo;
    }

    public void setMaterialCuerpo(String materialCuerpo) {
        this.materialCuerpo = materialCuerpo;
    }

    public String getEntradaRoscaTipo() {
        return entradaRoscaTipo;
    }

    public void setEntradaRoscaTipo(String entradaRoscaTipo) {
        this.entradaRoscaTipo = entradaRoscaTipo;
    }

    public String getEntradaBridaDiametro() {
        return entradaBridaDiametro;
    }

    public void setEntradaBridaDiametro(String entradaBridaDiametro) {
        this.entradaBridaDiametro = entradaBridaDiametro;
    }

    public String getEntradaBridaSerie() {
        return entradaBridaSerie;
    }

    public void setEntradaBridaSerie(String entradaBridaSerie) {
        this.entradaBridaSerie = entradaBridaSerie;
    }

    public String getSalidaRoscaTipo() {
        return salidaRoscaTipo;
    }

    public void setSalidaRoscaTipo(String salidaRoscaTipo) {
        this.salidaRoscaTipo = salidaRoscaTipo;
    }

    public String getSalidaBridaDiametro() {
        return salidaBridaDiametro;
    }

    public void setSalidaBridaDiametro(String salidaBridaDiametro) {
        this.salidaBridaDiametro = salidaBridaDiametro;
    }

    public String getSalidaBridaSerie() {
        return salidaBridaSerie;
    }

    public void setSalidaBridaSerie(String salidaBridaSerie) {
        this.salidaBridaSerie = salidaBridaSerie;
    }

    public Planta getPlanta() { return planta; }

    public void setPlanta(Planta planta) { this.planta = planta; }


    /**
     * SOBREESCRITO: Representación textual de la válvula.
     */
    @Override
    public String toString() {
        return tag;
    }
}