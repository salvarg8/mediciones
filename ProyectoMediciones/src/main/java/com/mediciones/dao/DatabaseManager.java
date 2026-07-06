package com.mediciones.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Administra la configuración y las conexiones a MySQL.
 *
 * <p>getConnection() solo abre una conexión nueva. La creación/actualización
 * del esquema debe ejecutarse explícitamente con initializeDatabase().</p>
 */
public final class DatabaseManager {

    private static final String CONFIG_FILE = "Database.properties";

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DATABASE = "mediciones_db";
    private static final String DEFAULT_USER = "appuser";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_USE_SSL = "false";
    private static final String DEFAULT_ALLOW_PUBLIC_KEY_RETRIEVAL = "true";
    private static final String DEFAULT_SERVER_TIMEZONE = "UTC";
    private static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    private static String host;
    private static String port;
    private static String database;
    private static String user;
    private static String password;
    private static String useSSL;
    private static String allowPublicKeyRetrieval;
    private static String serverTimezone;
    private static String characterEncoding;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    static {
        loadConfiguration();
        loadDriver();
    }

    private DatabaseManager() {
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        boolean loaded = loadExternalProperties(props);

        if (!loaded) {
            loaded = loadClasspathProperties(props);
        }

        if (!loaded) {
            System.out.println("No se encontró " + CONFIG_FILE + ". Usando valores por defecto/entorno.");
        }

        host = getConfigValue(props, "db.host", "DB_HOST", DEFAULT_HOST);
        port = getConfigValue(props, "db.port", "DB_PORT", DEFAULT_PORT);
        database = getConfigValue(props, "db.database", "DB_DATABASE", DEFAULT_DATABASE);
        user = getConfigValue(props, "db.user", "DB_USER", DEFAULT_USER);
        password = getConfigValue(props, "db.password", "DB_PASSWORD", DEFAULT_PASSWORD);
        useSSL = getConfigValue(props, "db.useSSL", "DB_USE_SSL", DEFAULT_USE_SSL);
        allowPublicKeyRetrieval = getConfigValue(
                props,
                "db.allowPublicKeyRetrieval",
                "DB_ALLOW_PUBLIC_KEY_RETRIEVAL",
                DEFAULT_ALLOW_PUBLIC_KEY_RETRIEVAL
        );
        serverTimezone = getConfigValue(props, "db.serverTimezone", "DB_SERVER_TIMEZONE", DEFAULT_SERVER_TIMEZONE);
        characterEncoding = getConfigValue(
                props,
                "db.characterEncoding",
                "DB_CHARACTER_ENCODING",
                DEFAULT_CHARACTER_ENCODING
        );
    }

    private static boolean loadExternalProperties(Properties props) {
        File externalFile = new File(CONFIG_FILE);
        if (!externalFile.isFile()) {
            return false;
        }

        try (InputStream input = new FileInputStream(externalFile)) {
            props.load(input);
            System.out.println("✅ Configuración cargada desde archivo externo: " + externalFile.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            logger.error("Error al cargar " + CONFIG_FILE + " externo: " + ex.getMessage(), ex);
            return false;
        }
    }

    private static boolean loadClasspathProperties(Properties props) {
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                return false;
            }
            props.load(input);
            System.out.println("✅ Configuración cargada desde " + CONFIG_FILE + " interno.");
            return true;
        } catch (IOException ex) {
            logger.error("Error al cargar " + CONFIG_FILE + " interno: " + ex.getMessage(), ex);
            return false;
        }
    }

    private static String getConfigValue(Properties props, String propertyKey, String environmentKey, String defaultValue) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }

        String propertyValue = props.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            logger.error("Driver JDBC de MySQL no encontrado.", ex);
            throw new IllegalStateException("Driver JDBC de MySQL no encontrado.", ex);
        }
    }

    private static String buildConnectionUrl() {
        return String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=%s&serverTimezone=%s&characterEncoding=%s",
                host,
                port,
                database,
                useSSL,
                allowPublicKeyRetrieval,
                serverTimezone,
                characterEncoding
        );
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(buildConnectionUrl(), user, password);
        } catch (SQLException ex) {
            logger.error("Fallo al conectar a MySQL", ex);
            throw new IllegalStateException("Fallo al conectar a MySQL: " + ex.getMessage(), ex);
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTablesIfNotExist(conn);
            updateSchema(conn);
            System.out.println("✅ Base de datos inicializada correctamente.");
        } catch (SQLException ex) {
            logger.error("Error al inicializar la base de datos.", ex);
            throw new IllegalStateException("Error al inicializar la base de datos.", ex);
        }
    }

    private static void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL," +
                    "nit VARCHAR(50) UNIQUE NOT NULL," +
                    "active BOOLEAN DEFAULT TRUE" + // Columna para borrado lógico
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS operador (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL," +
                    "identificacion VARCHAR(50) UNIQUE NOT NULL," +
                    "active BOOLEAN DEFAULT TRUE" + // Columna para borrado lógico
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS fluidos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(100) NOT NULL UNIQUE," +
                    "densidad REAL," +
                    "viscosidad REAL," +
                    "active BOOLEAN DEFAULT TRUE" +
                    ") ENGINE=InnoDB;");

            // NUEVA TABLA: plantas (Debe ir antes de valvulas por la clave foránea)
            stmt.execute("CREATE TABLE IF NOT EXISTS plantas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL," +
                    "cliente_id INT," +
                    "active BOOLEAN DEFAULT TRUE," + // Columna para borrado lógico
                    "CONSTRAINT fk_plantas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)" +
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS valvulas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "cliente_id INT," +
                    "planta_id INT," + // Relación con la tabla plantas
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
                    "active BOOLEAN DEFAULT TRUE," + // Columna para borrado lógico
                    "CONSTRAINT fk_valvulas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)," +
                    "CONSTRAINT fk_valvulas_planta FOREIGN KEY (planta_id) REFERENCES plantas(id)," +
                    "CONSTRAINT fk_valvulas_fluido FOREIGN KEY (fluido_servicio_id) REFERENCES fluidos(id)" +
                    ") ENGINE=InnoDB;");

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

            stmt.execute("CREATE TABLE IF NOT EXISTS ubicacion (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "ubicacion TEXT NOT NULL" +
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS configuracion (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "origen_datos TEXT NOT NULL," +
                    "ruta_archivo TEXT, " +
                    "puerto_com_default TEXT, " +
                    "ruta_plantilla_excel TEXT "+
                    ") ENGINE=InnoDB;");

            stmt.execute("CREATE TABLE IF NOT EXISTS tipos_valvula (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "nombre VARCHAR(255) NOT NULL UNIQUE," +
                    "active BOOLEAN DEFAULT TRUE" +
                    ") ENGINE=InnoDB;");
        }
    }

    private static void updateSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Actualizaciones anteriores
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN fluido_servicio_id INT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN entrada_rosca_tipo TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN entrada_brida_diametro TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN entrada_brida_serie TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN salida_rosca_tipo TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN salida_brida_diametro TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN salida_brida_serie TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE calibracion_sensores ADD COLUMN a3 REAL");
            executeSchemaUpdate(stmt, "ALTER TABLE calibracion_sensores ADD COLUMN c3 REAL");

            // Actualizaciones NUEVAS para soportar Plantas y Borrado Lógico
            executeSchemaUpdate(stmt, "ALTER TABLE clientes ADD COLUMN active BOOLEAN DEFAULT TRUE");
            executeSchemaUpdate(stmt, "ALTER TABLE operador ADD COLUMN active BOOLEAN DEFAULT TRUE");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN active BOOLEAN DEFAULT TRUE");


            // Relación de válvula con planta (por si la tabla válvulas ya existía)
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN planta_id INT");

            executeSchemaUpdate(stmt, "ALTER TABLE fluidos ADD COLUMN active BOOLEAN DEFAULT TRUE");

            // Claves foráneas faltantes
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD CONSTRAINT fk_valvulas_fluido " +
                    "FOREIGN KEY (fluido_servicio_id) REFERENCES fluidos(id)");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD CONSTRAINT fk_valvulas_planta " +
                    "FOREIGN KEY (planta_id) REFERENCES plantas(id)");

            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD COLUMN tipo_valvula_id INT");
            executeSchemaUpdate(stmt, "ALTER TABLE valvulas ADD CONSTRAINT fk_valvulas_tipo FOREIGN KEY (tipo_valvula_id) REFERENCES tipos_valvula(id)");
            executeSchemaUpdate(stmt, "ALTER TABLE configuracion ADD COLUMN puerto_com_default TEXT");
            executeSchemaUpdate(stmt, "ALTER TABLE configuracion ADD COLUMN ruta_plantilla_excel TEXT");
        }
    }

    private static void executeSchemaUpdate(Statement stmt, String sql) throws SQLException {
        try {
            stmt.execute(sql);
        } catch (SQLException ex) {
            if (isExpectedAlreadyAppliedError(ex)) {
                return;
            }
            throw ex;
        }
    }

    private static boolean isExpectedAlreadyAppliedError(SQLException ex) {
        int errorCode = ex.getErrorCode();
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return errorCode == 1060
                || errorCode == 1061
                || errorCode == 1826
                || message.contains("duplicate column")
                || message.contains("duplicate key")
                || message.contains("duplicate foreign key")
                || message.contains("already exists");
    }

    /**
     * Compatibilidad con llamadas existentes. Las conexiones se cierran por DAO.
     */
    public static void closeConnection() {
        // No hay conexión global que cerrar.
    }
}
