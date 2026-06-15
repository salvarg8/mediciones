package com.mediciones.model;

public class ReporteValvula {
    // --- Campos existentes ---
    private String tag;
    private String cliente;
    private String marca;
    private String materialCuerpo;
    private String lugarConexion;
    private String fluidoServicio;
    private String conexionTipo;
    private String entrada;
    private String salida;
    private String presionDiseno;
    private String temperaturaDiseno;
    private String contrapresion;
    private String presionCalibracionVacio;
    private String trabajosRealizados;
    private double presionAperturaFinal; // Este es el valor medido
    private String serie;
    private String modelo;
    private String normaDiseno;

    // --- CAMPOS NUEVOS PARA SOLUCIONAR EL ERROR ---
    private String oc;
    private String presionAperturaSolicitada;
    private String presionAperturaPreDesarme;
    private String presionRecierrePreDesarme; // <-- CAMPO AÑADIDO
    private String observacionesPreDesarme;
    private String instrumento;
    private String fluidoPrueba;
    private String temperaturaPrueba;
    private String presionRecierre;
    private String resultadoFinal;
    private String tecnicoRealiza;

    // Constructor
    public ReporteValvula() {}

    // --- GETTERS Y SETTERS ---

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getMaterialCuerpo() { return materialCuerpo; }
    public void setMaterialCuerpo(String materialCuerpo) { this.materialCuerpo = materialCuerpo; }

    public String getLugarConexion() { return lugarConexion; }
    public void setLugarConexion(String lugarConexion) { this.lugarConexion = lugarConexion; }

    public String getFluidoServicio() { return fluidoServicio; }
    public void setFluido(String fluidoServicio) { this.fluidoServicio = fluidoServicio; }

    public String getConexionTipo() { return conexionTipo; }
    public void setConexionTipo(String conexionTipo) { this.conexionTipo = conexionTipo; }

    public String getEntrada() { return entrada; }
    public void setEntrada(String entrada) { this.entrada = entrada; }

    public String getSalida() { return salida; }
    public void setSalida(String salida) { this.salida = salida; }

    public String getPresionDiseno() { return presionDiseno; }
    public void setPresionDiseno(String presionDiseno) { this.presionDiseno = presionDiseno; }

    public String getTemperaturaDiseno() { return temperaturaDiseno; }
    public void setTemperaturaDiseno(String temperaturaDiseno) { this.temperaturaDiseno = temperaturaDiseno; }

    public String getContrapresion() { return contrapresion; }
    public void setContrapresion(String contrapresion) { this.contrapresion = contrapresion; }

    public String getPresionCalibracionVacio() { return presionCalibracionVacio; }
    public void setPresionCalibracionVacio(String presionCalibracionVacio) { this.presionCalibracionVacio = presionCalibracionVacio; }

    public String getTrabajosRealizados() { return trabajosRealizados; }
    public void setTrabajosRealizados(String trabajosRealizados) { this.trabajosRealizados = trabajosRealizados; }

    public double getPresionAperturaFinal() { return presionAperturaFinal; }
    public void setPresionAperturaFinal(double presionAperturaFinal) { this.presionAperturaFinal = presionAperturaFinal; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getNormaDiseno() { return normaDiseno; }
    public void setNormaDiseno(String normaDiseno) { this.normaDiseno = normaDiseno; }

    // --- GETTERS Y SETTERS PARA NUEVOS CAMPOS ---

    public String getOc() { return oc; }
    public void setOc(String oc) { this.oc = oc; }

    public String getPresionAperturaSolicitada() { return presionAperturaSolicitada; }
    public void setPresionAperturaSolicitada(String presionAperturaSolicitada) { this.presionAperturaSolicitada = presionAperturaSolicitada; }

    public String getPresionAperturaPreDesarme() { return presionAperturaPreDesarme; }
    public void setPresionAperturaPreDesarme(String presionAperturaPreDesarme) { this.presionAperturaPreDesarme = presionAperturaPreDesarme; }

    public String getPresionRecierrePreDesarme() { return presionRecierrePreDesarme; } // <-- GETTER Y SETTER AÑADIDOS
    public void setPresionRecierrePreDesarme(String presionRecierrePreDesarme) { this.presionRecierrePreDesarme = presionRecierrePreDesarme; }

    public String getObservacionesPreDesarme() { return observacionesPreDesarme; }
    public void setObservacionesPreDesarme(String observacionesPreDesarme) { this.observacionesPreDesarme = observacionesPreDesarme; }

    public String getInstrumento() { return instrumento; }
    public void setInstrumento(String instrumento) { this.instrumento = instrumento; }

    public String getFluidoPrueba() { return fluidoPrueba; }
    public void setFluidoPrueba(String fluidoPrueba) { this.fluidoPrueba = fluidoPrueba; }

    public String getTemperaturaPrueba() { return temperaturaPrueba; }
    public void setTemperaturaPrueba(String temperaturaPrueba) { this.temperaturaPrueba = temperaturaPrueba; }

    public String getPresionRecierre() { return presionRecierre; }
    public void setPresionRecierre(String presionRecierre) { this.presionRecierre = presionRecierre; }

    public String getResultadoFinal() { return resultadoFinal; }
    public void setResultadoFinal(String resultadoFinal) { this.resultadoFinal = resultadoFinal; }

    public String getTecnicoRealiza() { return tecnicoRealiza; }
    public void setTecnicoRealiza(String tecnicoRealiza) { this.tecnicoRealiza = tecnicoRealiza; }
}
