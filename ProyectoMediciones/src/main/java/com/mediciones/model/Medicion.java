package com.mediciones.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Medicion {

    // 1. DATOS INICIALES: Se conocen al arrancar (pueden ser final)
    private final Valvula valvula;
    private final Cliente cliente;
    private final Operador operador;
    private final Fluido fluido;
    private final Double presionSolicitada;
    private final String unidadPresion;

    // 2. DATOS DE RESULTADO: Se calculan durante o al final (NO pueden ser final)
    private Double maximo;
    private Double recuperacion;
    private Double temperaturaInicial;

    // 3. LISTAS DINÁMICAS
    private final List<Double> tiemposMedicion;
    private final List<Double> valoresMedicion;

    // El constructor solo pide lo que se sabe al presionar "Iniciar Medición"
    public Medicion(Valvula valvula, Cliente cliente, Operador operador, Fluido fluido, Double presionSolicitada, String unidadPresion) {
        this.valvula = valvula;
        this.cliente = cliente;
        this.operador = operador;
        this.fluido = fluido;
        this.presionSolicitada = presionSolicitada;
        this.unidadPresion = unidadPresion;

        // Inicializamos los resultados en valores neutros seguros
        this.maximo = 0.0;
        this.recuperacion = 0.0;
        this.temperaturaInicial = null;

        this.valoresMedicion = new ArrayList<>();
        this.tiemposMedicion = new ArrayList<>();
    }

    // Método seguro para sincronizar la adición de puntos en tiempo real
    public synchronized void agregarPunto(double tiempo, double valor) {
        this.tiemposMedicion.add(tiempo);
        this.valoresMedicion.add(valor);
    }

    // SETTERS para los datos que se consolidan al final de la prueba
    public void setMaximo(Double maximo) {
        this.maximo = maximo;
    }

    public void setRecuperacion(Double recuperacion) {
        this.recuperacion = recuperacion;
    }

    public void setTemperaturaInicial(Double temperaturaInicial) {
        if (this.temperaturaInicial == null) { // Protege para capturar solo la primera del ensayo
            this.temperaturaInicial = temperaturaInicial;
        }
    }

    // GETTERS de solo lectura
    public Valvula getValvula() { return valvula; }
    public Cliente getCliente() { return cliente; }
    public Operador getOperador() { return operador; }
    public Fluido getFluido() { return fluido; }
    public Double getPresionSolicitada() { return presionSolicitada; }
    public String getUnidadPresion() { return unidadPresion; }

    public Double getMaximo() { return maximo; }
    public Double getRecuperacion() { return recuperacion; }
    public Double getTemperaturaInicial() { return temperaturaInicial; }

    public synchronized List<Double> getTiemposMedicion() {
        return Collections.unmodifiableList(tiemposMedicion);
    }

    public synchronized List<Double> getValoresMedicion() {
        return Collections.unmodifiableList(valoresMedicion);
    }

    public synchronized void limpiarMediciones() {
        this.tiemposMedicion.clear();
        this.valoresMedicion.clear();
        this.maximo = 0.0;
        this.recuperacion = 0.0;
        this.temperaturaInicial = null;
    }
}