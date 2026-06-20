package com.mediciones.repository;

import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.model.Planta;
import com.mediciones.model.TipoValvula;
import com.mediciones.model.Valvula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class PortalRepository {

    private static PortalRepository instancia;

    private List<Cliente> clientes;
    private List<Valvula> valvulas;
    private List<Planta> plantas;
    private List<TipoValvula> tiposValvula; // <-- NUEVO: Lista permanente de tipos

    private Map<Integer, Cliente> clientesPorId;
    private Map<Integer, Planta> plantasPorId;
    private Map<Integer, Valvula> valvulasPorId;
    private Map<Integer, List<Planta>> plantasPorCliente;
    private Map<Integer, List<Valvula>> valvulasPorPlanta;
    private Map<Integer, List<Valvula>> valvulasPorCliente;

    private static final Logger logger = LoggerFactory.getLogger(PortalRepository.class);

    public static PortalRepository getInstancia() {
        if (instancia == null) {
            instancia = new PortalRepository();
        }
        return instancia;
    }

    private PortalRepository() {
        this.clientes = new ArrayList<>();
        this.valvulas = new ArrayList<>();
        this.plantas = new ArrayList<>();
        this.tiposValvula = new ArrayList<>(); // <-- Inicialización

        this.clientesPorId = new HashMap<>();
        this.valvulasPorId = new HashMap<>();
        this.plantasPorId = new HashMap<>();

        this.valvulasPorCliente = new HashMap<>();
        this.plantasPorCliente = new HashMap<>();
        this.valvulasPorPlanta = new HashMap<>();
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

    public List<Planta> getPlantasPorCliente(int idCliente) {
        return plantasPorCliente.getOrDefault(idCliente, new ArrayList<>());
    }

    // <-- NUEVO: Simplemente retorna la lista ya calculada
    public List<TipoValvula> getTiposValvula() {
        return new ArrayList<>(this.tiposValvula);
    }

    public void recargar(String rutaArchivo) throws ArchivoNoEncontradoException {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists() || !archivo.isFile()) {
            logger.error("error al recargar(), no existe archivo: " + rutaArchivo);
            throw new ArchivoNoEncontradoException("El archivo de texto no se encuentra en la ruta especificada.");
        }

        // --- MAPAS Y LISTAS TEMPORALES (ESTADO ATÓMICO) ---
        List<Cliente> nuevosClientes = new ArrayList<>();
        List<Valvula> nuevasValvulas = new ArrayList<>();
        List<Planta> nuevasPlantas = new ArrayList<>();

        Map<Integer, Cliente> nuevosClientesPorId = new HashMap<>();
        Map<Integer, Valvula> nuevasValvulasPorId = new HashMap<>();
        Map<Integer, Planta> nuevasPlantasPorId = new HashMap<>();

        Map<Integer, List<Valvula>> nuevasValvulasPorCliente = new HashMap<>();
        Map<Integer, List<Planta>> nuevasPlantasPorCliente = new HashMap<>();
        Map<Integer, List<Valvula>> nuevasValvulasPorPlanta = new HashMap<>();

        // <-- NUEVO: Mapa temporal para guardar los Tipos de Válvula que vayamos encontrando
        Map<String, TipoValvula> nuevosTiposMap = new HashMap<>();

        // --- VARIABLES DE CONTROL DE FLUJO ---
        Cliente clienteActual = null;
        Planta plantaActualObj = null;
        String plantaActualStr = "N/A";
        int idValvula = 1;
        int idPlanta = 1;
        int[] idFicticioTipo = {-1}; // <-- NUEVO: Usamos un array para poder pasarlo por referencia al parseValvula

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
                            plantaActualObj = null;
                            plantaActualStr = "N/A";
                        }
                        break;

                    case '2':
                        String detallePlanta = safeSubstring(linea, 9, 159);
                        if (!detallePlanta.isEmpty() && clienteActual != null) {
                            plantaActualStr = detallePlanta;

                            plantaActualObj = new Planta();
                            plantaActualObj.setId(idPlanta++);
                            plantaActualObj.setNombre(plantaActualStr);
                            plantaActualObj.setCliente(clienteActual);
                            plantaActualObj.setValvulas(new ArrayList<>());

                            nuevasPlantas.add(plantaActualObj);
                            nuevasPlantasPorId.put(plantaActualObj.getId(), plantaActualObj);

                            nuevasPlantasPorCliente
                                    .computeIfAbsent(clienteActual.getId(), k -> new ArrayList<>())
                                    .add(plantaActualObj);
                            clienteActual.getPlantas().add(plantaActualObj);
                        }
                        break;

                    case '3':
                        if (clienteActual == null) {
                            continue;
                        }

                        // <-- CAMBIO: Le pasamos el mapa y el contador ficticio al parser
                        Valvula valvula = parseValvula(linea, clienteActual, idValvula++, plantaActualStr, nuevosTiposMap, idFicticioTipo);
                        if (valvula != null) {

                            nuevasValvulas.add(valvula);
                            nuevasValvulasPorId.put(valvula.getId(), valvula);
                            nuevasValvulasPorCliente
                                    .computeIfAbsent(clienteActual.getId(), k -> new ArrayList<>())
                                    .add(valvula);

                            if (plantaActualObj != null) {
                                plantaActualObj.getValvulas().add(valvula);
                                nuevasValvulasPorPlanta
                                        .computeIfAbsent(plantaActualObj.getId(), k -> new ArrayList<>())
                                        .add(valvula);
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException ex) {
            logger.error("error al recargar()", ex);
            return;
        }

        // <-- NUEVO: Convertimos los Tipos de Válvula encontrados a una lista y los ordenamos alfabéticamente
        List<TipoValvula> nuevosTiposValvula = new ArrayList<>(nuevosTiposMap.values());
        nuevosTiposValvula.sort(Comparator.comparing(TipoValvula::getNombre, String.CASE_INSENSITIVE_ORDER));

        // Reemplazo atómico de los estados del repositorio
        this.clientes = nuevosClientes;
        this.valvulas = nuevasValvulas;
        this.plantas = nuevasPlantas;
        this.tiposValvula = nuevosTiposValvula; // <-- NUEVO: Asignación final atómica

        this.clientesPorId = nuevosClientesPorId;
        this.valvulasPorId = nuevasValvulasPorId;
        this.plantasPorId = nuevasPlantasPorId;

        this.valvulasPorCliente = nuevasValvulasPorCliente;
        this.plantasPorCliente = nuevasPlantasPorCliente;
        this.valvulasPorPlanta = nuevasValvulasPorPlanta;
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
            logger.error("error al parsear cliente", ex);
            return null;
        }
    }

    // <-- CAMBIO: Se añadió `mapaTipos` y `idFicticio` a la firma del método
    private Valvula parseValvula(String linea, Cliente cliente, int idValvula, String lugarConexion, Map<String, TipoValvula> mapaTipos, int[] idFicticio) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }
        try {
            Valvula valvula = new Valvula();
            valvula.setId(idValvula);
            valvula.setCliente(cliente);
            valvula.setLugarConexion(lugarConexion);

            String tag = safeSubstring(linea, 286, 305);
            valvula.setTag(tag);

            String detalleRaw = safeSubstring(linea, 9, 159).trim();
            String detalle = detalleRaw;

            String marca = "S/D";
            String material = "";
            String serie = "";
            String tipoValvulaStr = "Válvula";
            String conexionesSobrantes = "";

            java.util.regex.Matcher mSerie = java.util.regex.Pattern.compile("\\((.*?)\\)").matcher(detalle);
            if (mSerie.find()) {
                serie = mSerie.group(1).trim();
                detalle = detalle.replace(mSerie.group(0), "").replace("--", "-").replace(" - - ", " - ").trim();
            }

            java.util.regex.Matcher mMat = java.util.regex.Pattern.compile("(?i)(acero\\s*inox\\w*|acero\\s*carbono|hierro|bronce|lat[oó]n|tefl[oó]n|wcb|cf8m|ss316|asiento)").matcher(detalle);
            if (mMat.find()) {
                material = mMat.group(1).trim();
                detalle = detalle.replace(mMat.group(0), "").replace("--", "-").trim();
            }

            String[] fragmentos = detalle.split("-");

            if (fragmentos.length >= 2) {
                tipoValvulaStr = fragmentos[0].trim();
                marca = fragmentos[1].trim();

                for (int i = 2; i < fragmentos.length; i++) {
                    if (!fragmentos[i].trim().isEmpty()) {
                        conexionesSobrantes += fragmentos[i].trim() + " - ";
                    }
                }
                if (conexionesSobrantes.endsWith(" - ")) {
                    conexionesSobrantes = conexionesSobrantes.substring(0, conexionesSobrantes.length() - 3);
                }
            } else {
                marca = detalleRaw;
                conexionesSobrantes = detalle;
            }

            valvula.setMarca(marca);
            valvula.setMaterialCuerpo(material);
            valvula.setNumeroSerie(serie);

            // --- NUEVA LÓGICA DE MAPEO PARA EL TIPO DE VÁLVULA ---
            // Usamos mayúsculas como clave única para no tener duplicados por diferencias de tipeo.
            String keyTipo = tipoValvulaStr.toUpperCase();
            TipoValvula tipoV = mapaTipos.get(keyTipo);

            if (tipoV == null) {
                // Si el Tipo de Válvula no existe aún en el mapa, lo creamos.
                tipoV = new TipoValvula();
                // Usamos el idFicticio actual (ej. -1), y luego lo decrementamos (ej. pasa a -2)
                tipoV.setId(idFicticio[0]--);
                tipoV.setNombre(tipoValvulaStr);
                mapaTipos.put(keyTipo, tipoV);
            }

            // Le asignamos a la Válvula el TipoValvula recién creado o el existente del mapa
            valvula.setTipoValvula(tipoV);
            // -----------------------------------------------------

            // Fluido por defecto
            Fluido fluido = new Fluido();
            fluido.setNombre("No especificado");
            valvula.setFluido(fluido);

            String despuesParentesis = conexionesSobrantes.trim();

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
            logger.error("error al parsear valvula", ex);
            return null;
        }
    }

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

    private String safeSubstring(String texto, int inicio, int fin) {
        if (texto == null || texto.length() < inicio) {
            return "";
        }
        if (texto.length() < fin) {
            return texto.substring(inicio).trim();
        }
        return texto.substring(inicio, fin).trim();
    }

    public Valvula getValvulaPorId(int valvulaId) {
        return valvulasPorId.get(valvulaId);
    }
}