package com.mediciones.repository;

import com.mediciones.model.Cliente;
import com.mediciones.model.Fluido;
import com.mediciones.model.Valvula;
import com.mediciones.view.FrmOperadorCRUD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class PortalRepository {

    private static PortalRepository instancia;

    private List<Cliente> clientes;
    private List<Valvula> valvulas;
    private Map<Integer, Cliente> clientesPorId;
    private Map<Integer, Valvula> valvulasPorId;
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

    public void recargar(String rutaArchivo) throws ArchivoNoEncontradoException {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists() || !archivo.isFile()) {
            logger.error("error al recargar(), no existe archivo: " + rutaArchivo);
            throw new ArchivoNoEncontradoException("El archivo de texto no se encuentra en la ruta especificada.");
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
            logger.error("error al recargar()", ex);
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
            logger.error("error al parsear cliente", ex);
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

            // 2. EXTRAER EL BLOQUE DE DETALLE
            String detalleRaw = safeSubstring(linea, 9, 159).trim();
            String detalle = detalleRaw;

            // Variables con valores por defecto ("Salvavidas")
            String marca = "S/D";
            String material = "";
            String serie = "";
            String servicio = "Válvula";
            String conexionesSobrantes = "";

            // 3. EXTRAER SERIE/MODELO (Lo que está entre paréntesis)
            java.util.regex.Matcher mSerie = java.util.regex.Pattern.compile("\\((.*?)\\)").matcher(detalle);
            if (mSerie.find()) {
                serie = mSerie.group(1).trim();
                // Quitamos el paréntesis del texto y limpiamos guiones dobles sobrantes
                detalle = detalle.replace(mSerie.group(0), "").replace("--", "-").replace(" - - ", " - ").trim();
            }

            // 4. EXTRAER MATERIAL (Si menciona inox, bronce, etc. en cualquier lado)
            java.util.regex.Matcher mMat = java.util.regex.Pattern.compile("(?i)(acero\\s*inox\\w*|acero\\s*carbono|hierro|bronce|lat[oó]n|tefl[oó]n|wcb|cf8m|ss316|asiento)").matcher(detalle);
            if (mMat.find()) {
                material = mMat.group(1).trim();
                detalle = detalle.replace(mMat.group(0), "").replace("--", "-").trim();
            }

            // 5. SEPARAR SERVICIO, MARCA Y CONEXIONES (Usando los guiones restantes)
            String[] fragmentos = detalle.split("-");

            if (fragmentos.length >= 2) {
                // El primer bloque es el servicio (ej: "Válvula de Seguridad")
                servicio = fragmentos[0].trim();

                // El segundo bloque es la marca (ej: "Hansen")
                marca = fragmentos[1].trim();

                // Todo lo demás son las conexiones (ej: 1" y 1 1/4")
                for (int i = 2; i < fragmentos.length; i++) {
                    if (!fragmentos[i].trim().isEmpty()) {
                        conexionesSobrantes += fragmentos[i].trim() + " - ";
                    }
                }
                // Quitamos el último guion sobrante de las conexiones
                if (conexionesSobrantes.endsWith(" - ")) {
                    conexionesSobrantes = conexionesSobrantes.substring(0, conexionesSobrantes.length() - 3);
                }
            } else {
                // SALVAVIDAS: Si el texto no tiene el formato esperado, guardamos todo en "Marca"
                marca = detalleRaw;
                conexionesSobrantes = detalle;
            }

            // Asignar al objeto Válvula
            valvula.setMarca(marca);
            valvula.setMaterialCuerpo(material);
            valvula.setNumeroSerie(serie);

            Fluido fluido = new Fluido();
            fluido.setNombre(servicio);
            valvula.setFluido(fluido);

            // 6. PREPARAR CONEXIONES PARA EL PARSEO FINAL
            String despuesParentesis = conexionesSobrantes.trim();

            // 7. PARSEAR CONEXIONES DE ENTRADA Y SALIDA
            // (A partir de aquí, tu código original que divide entrada y salida funciona perfecto)
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

    public Valvula getValvulaPorId(int valvulaId) {
            return valvulasPorId.get(valvulaId);
    }

}
