import javax.swing.*;
import java.awt.event.*;

public class Main {
    public static void main(String[] args) {
        // Crear el JFrame
        JFrame frame = new JFrame("Pantalla de Productos");

        // Quitar decoración (sin barra de título ni botones)
        frame.setUndecorated(true);

        // Pasar el JFrame al constructor de login
        login pantalla = new login(frame);

        // Establecer el contenido
        frame.setContentPane(pantalla.panel1);

        // 🚫 Prevenir cierre con Alt+F4 u otros medios
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Agregar un WindowListener que ignora el cierre
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // No hacer nada cuando se intente cerrar
                System.out.println("Intento de cierre bloqueado (Alt+F4 desactivado)");
            }
        });

        // Tamaño estilo login
        frame.setSize(400, 200);

        // Centrar ventana
        frame.setLocationRelativeTo(null);

        // Mostrar ventana
        frame.setVisible(true);
    }
}
