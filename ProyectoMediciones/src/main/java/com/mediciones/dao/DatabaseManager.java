package com.mediciones.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Clase para manejar la conexión a la base de datos MySQL.
 * Versión mejorada con sistema de configuración externa para conexión a servidores remotos.
 */
public class DatabaseManager {

    // Valores por defecto (se usarán si no hay archivo de configuración)
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DATABASE = "mediciones_db";
    private static final String DEFAULT_USER = "appuser";
    private static final String DEFAULT_PASSWORD = "Sierra";

    // Parámetros de configuración
    private static String HOST;
    private static String PORT;
    private static String DATABASE;
    private static String USER;
    private static String PASSWORD;
    private static String USE_SSL;
    private static String ALLOW_PUBLIC_KEY_RETRIEVAL;
    private static String SERVER_TIMEZONE;
    private static String CHARACTER_ENCODING;

    private static Connection connection = null;

    // Carga la configuración al iniciar la clase
    static {
        loadConfiguration();
    }

    /**
     * Carga la configuración desde database.properties
     */
    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = DatabaseManager.class
                .getClassLoader()
                .getResourceAsStream("Database.properties")) {

            if (input == null) {
                System.out.println("⚠️ Archivo Database.properties no encontrado. Usando valores por defecto.");
                useDefaultConfiguration();
                return;
            }

            // Cargar propiedades
            props.load(input);

            // Obtener valores
            HOST = props.getProperty("db.host", DEFAULT_HOST);
            PORT = props.getProperty("db.port", DEFAULT_PORT);
            DATABASE = props.getProperty("db.database", DEFAULT_DATABASE);
            USER = props.getProperty("db.user", DEFAULT_USER);
            PASSWORD = props.getProperty("db.password", DEFAULT_PASSWORD);
            USE_SSL = props.getProperty("db.useSSL", "false");
            ALLOW_PUBLIC_KEY_RETRIEVAL = props.getProperty("db.allowPublicKeyRetrieval", "true");
            SERVER_TIMEZONE = props.getProperty("db.serverTimezone", "UTC");
            CHARACTER_ENCODING = props.getProperty("db.characterEncoding", "UTF-8");

            System.out.println("✅ Configuración cargada desde database.properties");

        } catch (Exception ex) {
            System.err.println("❌ Error al cargar configuración: " + ex.getMessage());
            System.out.println("⚠️ Usando valores por defecto");
            useDefaultConfiguration();
        }
    }

    /**
     * Usa valores por defecto si no hay archivo de configuración
     */
    private static void useDefaultConfiguration() {
        HOST = DEFAULT_HOST;
        PORT = DEFAULT_PORT;
        DATABASE = DEFAULT_DATABASE;
        USER = DEFAULT_USER;
        PASSWORD = DEFAULT_PASSWORD;
        USE_SSL = "false";
        ALLOW_PUBLIC_KEY_RETRIEVAL = "true";
        SERVER_TIMEZONE = "UTC";
        CHARACTER_ENCODING = "UTF-8";
    }

    /**
     * Construye la URL de conexión a MySQL
     */
    private static String buildConnectionUrl() {
        return String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s&characterEncoding=%s",
                HOST, PORT, DATABASE, USE_SSL, ALLOW_PUBLIC_KEY_RETRIEVAL,
                SERVER_TIMEZONE, CHARACTER_ENCODING
        );
    }

    public static Connection getConnection() {
        try {
            // Cargar el driver JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");

            if (connection == null || connection.isClosed()) {
                // Construir URL de conexión
                String url = buildConnectionUrl();
                System.out.println("🔌 Conectando a: " + url.replace(PASSWORD, "******"));

                connection = DriverManager.getConnection(url, USER, PASSWORD);
                System.out.println("✅ Conexión a MySQL establecida.");

                createTablesIfNotExist(connection);
                updateSchema(connection);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: Driver de MySQL no encontrado.");
            e.printStackTrace();
            throw new RuntimeException("Driver JDBC no cargado.", e);
        } catch (SQLException e) {
            System.err.println("❌ ERROR al conectar a MySQL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fallo crítico al conectar a MySQL.", e);
        }
        return connection;
    }

    private static void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // ✅ TABLA CLIENTES - CORREGIDA (TEXT → VARCHAR)
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL," +
                    "nit VARCHAR(50) UNIQUE NOT NULL" +
                    ") ENGINE=InnoDB;");

            // ✅ TABLA OPERADOR - CORREGIDA (TEXT → VARCHAR)
            stmt.execute("CREATE TABLE IF NOT EXISTS operador (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL," +
                    "identificacion VARCHAR(50) UNIQUE NOT NULL" +
                    ") ENGINE=InnoDB;");

            // ✅ TABLA FLUIDOS - CORREGIDA (TEXT → VARCHAR)
            stmt.execute("CREATE TABLE IF NOT EXISTS fluidos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(100) NOT NULL UNIQUE," +
                    "densidad REAL," +
                    "viscosidad REAL" +
                    ") ENGINE=InnoDB;");

            // Tabla valvulas (sin cambios necesarios)
            stmt.execute("CREATE TABLE IF NOT EXISTS valvulas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "cliente_id INT," +
                    "fluido_servicio_id INT," +
                    "tag TEXT," +
                    "numero_serie TEXT," +
                    "lugar_conexion TEXT," +
                    "marca TEXT," +
                    "material_cuerpo TEXT," +
                    "entrada_rosca_tipo TEXT," +
                    "entrada_brida_diametro TEXT," +
                    "entrada_brida_serie TEXT," +
                    "salida_rosca_tipo TEXT," +
                    "salida_brida_diametro TEXT," +
                    "salida_brida_serie TEXT," +
                    "FOREIGN KEY (cliente_id) REFERENCES clientes(id)," +
                    "FOREIGN KEY (fluido_servicio_id) REFERENCES fluidos(id)" +
                    ") ENGINE=InnoDB;");

            // Tabla calibracion_sensores (sin cambios)
            stmt.execute("CREATE TABLE IF NOT EXISTS calibracion_sensores (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sensor_type TEXT NOT NULL," +
                    "a1 REAL," +
                    "c1 REAL," +
                    "a2 REAL," +
                    "c2 REAL," +
                    "a3 REAL," +
                    "c3 REAL," +
                    "presion_conocida REAL," +
                    "voltaje_conocido REAL," +
                    "fecha_calibracion TEXT" +
                    ") ENGINE=InnoDB;");

            // Tabla ubicacion (sin cambios)
            stmt.execute("CREATE TABLE IF NOT EXISTS ubicacion (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "ubicacion TEXT NOT NULL" +
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS configuracion (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    origen_datos TEXT NOT NULL," +
                    "    ruta_archivo TEXT" +
                    ") ENGINE=InnoDB;");

            System.out.println("✅ Tablas de MySQL creadas correctamente.");
        }
    }

    private static void updateSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Para valvulas: primero columnas, luego foreign keys
            String[] valvulasUpdates = {
                    "ALTER TABLE valvulas ADD COLUMN fluido_servicio_id INT",
                    "ALTER TABLE valvulas ADD FOREIGN KEY (fluido_servicio_id) REFERENCES fluidos(id)",
                    "ALTER TABLE valvulas ADD COLUMN entrada_rosca_tipo TEXT",
                    "ALTER TABLE valvulas ADD COLUMN entrada_brida_diametro TEXT",
                    "ALTER TABLE valvulas ADD COLUMN entrada_brida_serie TEXT",
                    "ALTER TABLE valvulas ADD COLUMN salida_rosca_tipo TEXT",
                    "ALTER TABLE valvulas ADD COLUMN salida_brida_diametro TEXT",
                    "ALTER TABLE valvulas ADD COLUMN salida_brida_serie TEXT"
            };

            for (String sql : valvulasUpdates) {
                try {
                    stmt.execute(sql);
                } catch (SQLException ignored) {}
            }

            // Para calibracion_sensores
            String[] calibUpdates = {
                    "ALTER TABLE calibracion_sensores ADD COLUMN a3 REAL",
                    "ALTER TABLE calibracion_sensores ADD COLUMN c3 REAL"
            };

            for (String sql : calibUpdates) {
                try {
                    stmt.execute(sql);
                } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error al actualizar esquema MySQL: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Conexión a MySQL cerrada.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}