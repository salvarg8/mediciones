package com.mediciones.utils;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;

public class ValidadorUI {

    /**
     * Verifica si un campo de texto está vacío.
     */
    public static boolean esCampoVacio(JTextField campo, String nombreCampo, Component padre) {
        if (campo.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(padre,
                    "El campo '" + nombreCampo + "' es obligatorio y no puede estar vacío.",
                    "Validación de Entrada",
                    JOptionPane.WARNING_MESSAGE);
            campo.requestFocus();
            return true;
        }
        return false;
    }

    /**
     * Verifica si el texto ingresado es un número decimal válido.
     */
    public static boolean esNumeroValido(JTextField campo, String nombreCampo, Component padre) {
        if (esCampoVacio(campo, nombreCampo, padre)) return false;

        try {
            // Reemplazamos coma por punto por si el usuario usa teclado numérico latino
            String textoNumerico = campo.getText().trim().replace(",", ".");
            Double.parseDouble(textoNumerico);
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(padre,
                    "El valor ingresado en '" + nombreCampo + "' no es un número válido.\nPor favor, ingrese solo números y un punto decimal.",
                    "Error de Formato",
                    JOptionPane.ERROR_MESSAGE);
            campo.selectAll(); // Selecciona el texto erróneo para que lo borre rápido
            campo.requestFocus();
            return false;
        }
    }

    /**
     * Agrega un KeyListener a un JTextField para que solo acepte números y un punto decimal.
     * Se llama así: ValidadorUI.soloNumeros(txtPresionSolicitada);
     */
    public static void soloNumeros(JTextField campo) {
        campo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                // Permitir números, la tecla de borrar (backspace), y el punto/coma
                if (!Character.isDigit(c) && c != '\b' && c != '.' && c != ',') {
                    evt.consume();
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
                // Evitar que escriban más de un punto/coma
                if ((c == '.' || c == ',') && (campo.getText().contains(".") || campo.getText().contains(","))) {
                    evt.consume();
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }
}