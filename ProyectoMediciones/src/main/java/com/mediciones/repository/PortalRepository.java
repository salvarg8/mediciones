package com.mediciones.repository;

import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.model.Valvula;

import java.io.*;
import java.util.*;

public class PortalRepository {

    private List<Cliente> clientes;
    private List<Valvula> valvulas;
    private Map<Integer, Cliente> clientesPorId;
    private Map<Integer, Valvula> valvulasPorId;
    private Map<Integer, List<Valvula>> valvulasPorCliente;

    public PortalRepository() {
        this.clientes = new ArrayList<>();
        this.valvulas = new ArrayList<>();
        this.clientesPorId = new HashMap<>();
        this.valvulasPorId = new HashMap<>();
        this.valvulasPorCliente = new HashMap<>();
    }

    public List<Cliente> getClientes() {
        List<Cliente> resultado = new ArrayList<>(clientes);
        resultado.sort(Comparator.comparing(
                Cliente::getNombre,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)
        ));
        return resultado;
    }

    public List<Valvula> getValvulasPorCliente(int idCliente) {
        List<Valvula> lista = valvulasPorCliente.getOrDefault(idCliente, new ArrayList<Valvula>());
        List<Valvula> resultado = new ArrayList<>(lista);

        Collections.sort(resultado, new Comparator<Valvula>() {
            @Override
            public int compare(Valvula v1, Valvula v2) {
                String tag1 = v1.getTag() == null ? "" : v1.getTag();
                String tag2 = v2.getTag() == null ? "" : v2.getTag();
                return tag1.compareToIgnoreCase(tag2);
            }
        });

        return resultado;
    }

    public void recargar(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists() || !archivo.isFile()) {
            return;
        }

        List<Cliente> nuevosClientes = new ArrayList<>();
        List<Valvula> nuevasValvulas = new ArrayList<>();
        Map<Integer, Cliente> nuevosClientesPorId = new HashMap<>();
        Map<Integer, Valvula> nuevasValvulasPorId = new HashMap<>();
        Map<Integer, List<Valvula>> nuevasValvulasPorCliente = new HashMap<>();

        Cliente clienteActual = null;
        String plantaActual = "N/A"; // Variable persistente para el sector físico de las válvulas
        int idValvula = 1;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), "Windows-1252"))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                char tipoRegistro = linea.charAt(0);

                switch (tipoRegistro) {
                    case '1':
                        clienteActual = parseCliente(linea);
                        if (clienteActual != null) {
                            nuevosClientes.add(clienteActual);
                            nuevosClientesPorId.put(clienteActual.getId(), clienteActual);
                        }
                        break;

                    case '2':
                        String detallePlanta = safeSubstring(linea, 9, 159);
                        if (!detallePlanta.isEmpty()) {
                            plantaActual = detallePlanta;
                        }
                        break;

                    case '3':
                        if (clienteActual == null) {
                            continue;
                        }
                        Valvula valvula = parseValvula(linea, clienteActual, idValvula++, plantaActual);
                        if (valvula != null) {
                            nuevasValvulas.add(valvula);
                            nuevasValvulasPorId.put(valvula.getId(), valvula);
                            nuevasValvulasPorCliente
                                    .computeIfAbsent(clienteActual.getId(), k -> new ArrayList<>())
                                    .add(valvula);
                        }
                        break;

                    default:
                        // Registro desconocido o no soportado. Se ignora.
                        break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        // Si todo el proceso de lectura fue exitoso, actualizamos el estado atómico del repositorio
        this.clientes = nuevosClientes;
        this.valvulas = nuevasValvulas;
        this.clientesPorId = nuevosClientesPorId;
        this.valvulasPorId = nuevasValvulasPorId;
        this.valvulasPorCliente = nuevasValvulasPorCliente;
    }

    private int buscarIndiceDocumento(String linea) {
        String[] patrones = {"C.U.I.T.", "C.I.", "CUIT", "CUIL"};
        int menor = -1;

        for (String patron : patrones) {
            int indice = linea.indexOf(patron);
            if (indice >= 0 && (menor == -1 || indice < menor)) {
                menor = indice;
            }
        }
        return menor;
    }

    private Cliente parseCliente(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }
        try {
            int codigo = Integer.parseInt(linea.substring(1, 8).trim());
            int indiceDocumento = buscarIndiceDocumento(linea);
            String nombre;
            String nit = "";
            if (indiceDocumento >= 0) {
                nombre = linea.substring(8, indiceDocumento).trim();
                String resto;

                if (linea.startsWith("C.U.I.T.", indiceDocumento)) {
                    resto = linea.substring(indiceDocumento + "C.U.I.T.".length());
                } else if (linea.startsWith("C.I.", indiceDocumento)) {
                    resto = linea.substring(indiceDocumento + "C.I.".length());
                } else {
                    resto = linea.substring(indiceDocumento);
                }

                StringBuilder numero = new StringBuilder();
                for (char c : resto.toCharArray()) {
                    if (Character.isDigit(c)) {
                        numero.append(c);
                    } else if (numero.length() > 0) {
                        break;
                    }
                }
                nit = numero.toString();
            } else {
                nombre = linea.substring(8).trim();
            }

            return new Cliente(codigo, nombre, nit);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Valvula parseValvula(String linea, Cliente cliente, int idValvula, String lugarConexion) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }
        try {
            Valvula valvula = new Valvula();
            valvula.setId(idValvula);
            valvula.setCliente(cliente);
            valvula.setLugarConexion(lugarConexion);

            // 1. EXTRAER EL TAG OFICIAL
            String tag = safeSubstring(linea, 286, 306);
            valvula.setTag(tag);

            // 2. EXTRAER EL BLOQUE DE DETALLE (CH(150) - Posición exacta de 9 a 159)
            String detalle = safeSubstring(linea, 9, 159);

            // 3. AISLAR LOS PARÉNTESIS DEL NÚMERO DE SERIE O MATERIAL
            String contenidoParentesis = "";
            String antesParentesis = detalle;
            String despuesParentesis = "";

            int idxOpen = detalle.indexOf('(');
            int idxClose = detalle.indexOf(')', idxOpen + 1);

            if (idxOpen >= 0 && idxClose > idxOpen) {
                contenidoParentesis = detalle.substring(idxOpen + 1, idxClose).trim();
                antesParentesis = detalle.substring(0, idxOpen).trim();
                despuesParentesis = detalle.substring(idxClose + 1).trim();
            }

            // 4. ASIGNAR NÚMERO DE SERIE O MATERIAL DEL CUERPO (Si viene dentro del paréntesis)
            if (!contenidoParentesis.isEmpty()) {
                String limpio = contenidoParentesis.toLowerCase();
                if (limpio.contains("oxidable") || limpio.contains("vidrio") || limpio.contains("teflón") || limpio.contains("asiento")) {
                    valvula.setMaterialCuerpo(contenidoParentesis);
                    valvula.setNumeroSerie("");
                } else {
                    valvula.setNumeroSerie(contenidoParentesis);
                    valvula.setMaterialCuerpo("");
                }
            } else {
                valvula.setNumeroSerie("");
                valvula.setMaterialCuerpo("");
            }

            // 5. PARSEAR SERVICIO/FLUIDO Y MARCA (Bloque antes del paréntesis)
            if (antesParentesis.endsWith("-")) {
                antesParentesis = antesParentesis.substring(0, antesParentesis.length() - 1).trim();
            }

            int ultimoGuion = antesParentesis.lastIndexOf(" - ");
            String servicioTexto = "";
            String marca = "";

            if (ultimoGuion >= 0) {
                servicioTexto = antesParentesis.substring(0, ultimoGuion).trim();
                marca = antesParentesis.substring(ultimoGuion + 3).trim();
            } else {
                ultimoGuion = antesParentesis.lastIndexOf("-");
                if (ultimoGuion >= 0) {
                    servicioTexto = antesParentesis.substring(0, ultimoGuion).trim();
                    marca = antesParentesis.substring(ultimoGuion + 1).trim();
                } else {
                    servicioTexto = antesParentesis;
                    marca = "S/M";
                }
            }

            Fluido fluido = new Fluido();
            fluido.setNombre(servicioTexto);
            valvula.setFluido(fluido);
            valvula.setMarca(marca);

            // 6. LIMPIEZA Y BÚSQUEDA DE MATERIALES "FLOTANTES" (Bloque después del paréntesis)
            despuesParentesis = despuesParentesis.trim();
            while (despuesParentesis.startsWith("-")) despuesParentesis = despuesParentesis.substring(1).trim();
            while (despuesParentesis.endsWith("-"))
                despuesParentesis = despuesParentesis.substring(0, despuesParentesis.length() - 1).trim();

            // Dividimos lo que quedó usando los guiones como separador para no confundir medidas con materiales
            String[] fragmentos = despuesParentesis.split("-");
            String conexionesSobrantes = "";

            for (String fragmento : fragmentos) {
                String fragLimpio = fragmento.trim().toLowerCase();

                // Si el fragmento habla de un material, lo "pescamos"
                if (fragLimpio.contains("oxidable") || fragLimpio.contains("vidrio") ||
                        fragLimpio.contains("teflón") || fragLimpio.contains("asiento")) {

                    valvula.setMaterialCuerpo(fragmento.trim());

                } else {
                    // Si no es un material, son puras medidas. Las volvemos a unir.
                    if (!conexionesSobrantes.isEmpty()) {
                        conexionesSobrantes += " - ";
                    }
                    conexionesSobrantes += fragmento.trim();
                }
            }

            // Lo que sobrevivió al filtro de arriba son exclusivamente las conexiones
            despuesParentesis = conexionesSobrantes.trim();

            // 7. PARSEAR CONEXIONES DE ENTRADA Y SALIDA
            String entradaRaw = "";
            String salidaRaw = "";
            int divisorConexiones = despuesParentesis.indexOf("-");

            if (divisorConexiones >= 0) {
                entradaRaw = despuesParentesis.substring(0, divisorConexiones).trim();
                salidaRaw = despuesParentesis.substring(divisorConexiones + 1).trim();
            } else {
                entradaRaw = despuesParentesis;
            }

            if (!entradaRaw.isEmpty()) {
                procesarConexion(entradaRaw, true, valvula);
            }

            if (!salidaRaw.isEmpty()) {
                procesarConexion(salidaRaw, false, valvula);
            } else {
                valvula.setSalidaRoscaTipo(valvula.getEntradaRoscaTipo());
                valvula.setSalidaBridaDiametro(valvula.getEntradaBridaDiametro());
                valvula.setSalidaBridaSerie(valvula.getEntradaBridaSerie());
            }

            return valvula;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    /**
     * Desglosa y clasifica diámetros, series y tipos de conexión (Brida, Rosca, Clamp)
     */
    private void procesarConexion(String cadenaRaw, boolean esEntrada, Valvula valvula) {
        if (cadenaRaw == null || cadenaRaw.isEmpty()) return;

        String diametro = "";
        String serie = "";
        String tipoConexion = "";

        if (cadenaRaw.contains("s/")) {
            int idxSerie = cadenaRaw.indexOf("s/");
            diametro = cadenaRaw.substring(0, idxSerie).trim();
            serie = "s/" + cadenaRaw.substring(idxSerie + 2).trim();
            tipoConexion = "BRIDA";
        } else if (cadenaRaw.contains("PN")) {
            int idxSerie = cadenaRaw.indexOf("PN");
            diametro = cadenaRaw.substring(0, idxSerie).trim();
            serie = "PN " + cadenaRaw.substring(idxSerie + 2).trim();
            tipoConexion = "BRIDA";
        } else {
            diametro = cadenaRaw.trim();
            if (diametro.toLowerCase().contains("clamp")) {
                tipoConexion = "CLAMP";
                serie = "N/A";
            } else {
                tipoConexion = "ROSCA / NPT";
                serie = "N/A";
            }
        }

        if (esEntrada) {
            valvula.setEntradaRoscaTipo(tipoConexion);
            valvula.setEntradaBridaDiametro(diametro);
            valvula.setEntradaBridaSerie(serie);
        } else {
            valvula.setSalidaRoscaTipo(tipoConexion);
            valvula.setSalidaBridaDiametro(diametro);
            valvula.setSalidaBridaSerie(serie);
        }
    }

    /**
     * Método de extracción segura indexada
     */
    private String safeSubstring(String texto, int inicio, int fin) {
        if (texto == null || texto.length() < inicio) {
            return "";
        }
        if (texto.length() < fin) {
            return texto.substring(inicio).trim();
        }
        return texto.substring(inicio, fin).trim();
    }
}