package com.mediciones.App;

import com.mediciones.dao.DatabaseManager;
import com.mediciones.view.FrmInicio;
import javax.swing.SwingUtilities;
import java.io.File;

public class App {
    public static void main(String[] args) {

        // --- CONFIGURACIÓN PARA JSERIALCOMM ---
        // Se define una carpeta local para extraer las librerías nativas (.dll)
        // y así evitar problemas de "Acceso Denegado" enen "señal del sensor a< la carpeta Temp del sistema.
        File nativeDir = new File("target/native-libs");
        if (!nativeDir.exists()) {
            if (!nativeDir.mkdirs()) {
                System.err.println("Failed to create native-libs directory");
                return;
            }
        }
        System.setProperty("jSerialComm.tmpdir", nativeDir.getAbsolutePath());


        // 1. Inicializar la conexión a la base de datos (ya no necesita try/catch)
        DatabaseManager.getConnection();

        // 2. Ejecutar la interfaz gráfica en el hilo de Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            FrmInicio inicio = new FrmInicio();
            inicio.setVisible(true);
        });

        // 3. Cierre limpio de la conexión de DB al apagar (el error ya está corregido)
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::closeConnection));
    }
}