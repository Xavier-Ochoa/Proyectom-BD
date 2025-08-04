import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class empleado {
    public JPanel panel1;
    private JButton ir_a_clientes_Button;
    private JButton ir_a_ventas_Button;
    private JButton facturasButton;
    private JButton hProdutosButton;
    private JButton categorias_button1;
    private JButton promocionesButton;
    private JButton salirButton;
    private User usuarioLogeado;

    public empleado(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        ir_a_clientes_Button.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_clientes pantallaClientes = new empleado_clientes(usuarioLogeado);
            frame.setContentPane(pantallaClientes.panel1);
            frame.revalidate();
            frame.repaint();
        });

        ir_a_ventas_Button.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_ventas pantallaVentas = new empleado_ventas(usuarioLogeado);
            frame.setContentPane(pantallaVentas.panel1);
            frame.revalidate();
            frame.repaint();
        });

        facturasButton.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_facturas pantallaFacturas = new empleado_facturas(usuarioLogeado);
            frame.setContentPane(pantallaFacturas.panel1);
            frame.revalidate();
            frame.repaint();
        });

        hProdutosButton.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_productos pantallaProductos = new empleado_productos(usuarioLogeado);
            frame.setContentPane(pantallaProductos.panel1);
            frame.revalidate();
            frame.repaint();
        });

        categorias_button1.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_categ pantallaCategorias = new empleado_categ(usuarioLogeado);
            frame.setContentPane(pantallaCategorias.panel1);
            frame.revalidate();
            frame.repaint();
        });

        promocionesButton.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel1);
            empleado_promociones pantallaPromociones = new empleado_promociones(usuarioLogeado);
            frame.setContentPane(pantallaPromociones.panel1);
            frame.revalidate();
            frame.repaint();
        });
        salirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame ventanaActual = (JFrame) SwingUtilities.getWindowAncestor(panel1);
                ventanaActual.dispose(); // Cierra la ventana actual

                JFrame loginFrame = new JFrame("Login");
                login loginPanel = new login(loginFrame); // Crea el panel login
                loginFrame.setContentPane(loginPanel.panel1);
                loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                loginFrame.setSize(400, 300);
                loginFrame.setLocationRelativeTo(null);
                loginFrame.setVisible(true);
            }
        });
    }
}
