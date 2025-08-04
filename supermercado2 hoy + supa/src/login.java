import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class login {
    public JPanel panel1;
    private JButton iniciarSesionButton;
    private JTextField usuariotextField1;
    private JPasswordField contraseñaPasswordField;
    private JButton salir_button1;

    public login(JFrame framePrincipal) {
        iniciarSesionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String usuario = usuariotextField1.getText();
                String contraseña = new String(contraseñaPasswordField.getPassword());

                if (usuario.isEmpty() || contraseña.isEmpty()) {
                    JOptionPane.showMessageDialog(panel1, "Por favor, completa todos los campos.");
                    return;
                }

                try (Connection conn = Conexion.conectar()) {
                    if (conn == null) {
                        JOptionPane.showMessageDialog(panel1, "No se pudo conectar a la base de datos.");
                        return;
                    }

                    String sql = "SELECT usuario, rol, id_empleado FROM usuarios WHERE usuario = ? AND contraseña = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, usuario);
                    ps.setString(2, contraseña);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String usuarioDB = rs.getString("usuario");
                        String rol = rs.getString("rol");
                        int idEmpleado = rs.getInt("id_empleado");

                        User usuarioLogeado = new User(usuarioDB, rol, idEmpleado);

                        JOptionPane.showMessageDialog(panel1, "Login exitoso como: " + rol);

                        framePrincipal.dispose();

                        if (rol.equalsIgnoreCase("administrador")) {
                            JFrame adminFrame = new JFrame("Administrador");
                            adminFrame.setContentPane(new administrador(usuarioLogeado).panel1);
                            adminFrame.setSize(600, 400);
                            adminFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            adminFrame.setLocationRelativeTo(null);
                            adminFrame.setVisible(true);
                        } else if (rol.equalsIgnoreCase("empleado")) {
                            JFrame empleadoFrame = new JFrame("Empleado");
                            empleado empleadoPanel = new empleado(usuarioLogeado); // PASA EL OBJETO USER
                            empleadoFrame.setContentPane(empleadoPanel.panel1);
                            empleadoFrame.setSize(600, 400);
                            empleadoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            empleadoFrame.setLocationRelativeTo(null);
                            empleadoFrame.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(panel1, "Rol no reconocido.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(panel1, "Usuario o contraseña incorrectos.");
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(panel1, "Error: " + ex.getMessage());
                }
            }
        });

        salir_button1.addActionListener(e -> System.exit(0));
    }
}
