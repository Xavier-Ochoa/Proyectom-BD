import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Collections;

public class administrador_empleados {
    public JPanel panel1;
    private JTextField ingreso_del_nombretextField1;
    private JTextField ingreso_de_cedulatextField2;
    private JTextField ingreso_de_cargotextField3;
    private JTextField ingreso_de_telefonotextField4;
    private JButton registarNuevoEmpleadoButton;
    private JTable tabla_de_clientestable1;
    private JButton eliminar_empleadoButton;
    private JButton actualizarButton;
    private JButton modificarButton;
    private JButton volverButton;
    private JButton ordenar_tablabutton1;

    private User usuarioLogeado;

    public administrador_empleados(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        actualizarTablaEmpleados();

        registarNuevoEmpleadoButton.addActionListener(e -> {
            String nombre = ingreso_del_nombretextField1.getText().trim();
            String cedula = ingreso_de_cedulatextField2.getText().trim();
            String cargo = ingreso_de_cargotextField3.getText().trim();
            String telefono = ingreso_de_telefonotextField4.getText().trim();

            if (nombre.isEmpty() || cedula.isEmpty() || cargo.isEmpty() || telefono.isEmpty()) {
                JOptionPane.showMessageDialog(panel1, "Todos los campos son obligatorios.");
                return;
            }

            try (Connection conn = Conexion.conectar();
                 CallableStatement stmt = conn.prepareCall("{ call insertar_empleado(?, ?, ?, ?) }")) {
                stmt.setString(1, nombre);
                stmt.setString(2, cedula);
                stmt.setString(3, cargo);
                stmt.setString(4, telefono);
                stmt.execute();

                JOptionPane.showMessageDialog(panel1, "Empleado registrado exitosamente.");
                actualizarTablaEmpleados();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al registrar empleado: " + ex.getMessage());
            }
        });

        actualizarButton.addActionListener(e -> actualizarTablaEmpleados());

        modificarButton.addActionListener(e -> {
            if (tabla_de_clientestable1.isEditing()) {
                tabla_de_clientestable1.getCellEditor().stopCellEditing();
            }

            DefaultTableModel modelo = (DefaultTableModel) tabla_de_clientestable1.getModel();
            int filas = modelo.getRowCount();
            int modificados = 0;

            try (Connection conn = Conexion.conectar()) {
                for (int i = 0; i < filas; i++) {
                    int idEmpleado = Integer.parseInt(modelo.getValueAt(i, 0).toString());
                    String nombre = modelo.getValueAt(i, 1).toString().trim();
                    String cedula = modelo.getValueAt(i, 2).toString().trim();
                    String cargo = modelo.getValueAt(i, 3).toString().trim();
                    String telefono = modelo.getValueAt(i, 4).toString().trim();

                    String sql = "UPDATE empleados SET nombre = ?, cedula = ?, cargo = ?, telefono = ? WHERE id_empleado = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, nombre);
                        stmt.setString(2, cedula);
                        stmt.setString(3, cargo);
                        stmt.setString(4, telefono);
                        stmt.setInt(5, idEmpleado);
                        stmt.executeUpdate();
                        modificados++;
                    }
                }

                JOptionPane.showMessageDialog(panel1, modificados + " empleado(s) modificados correctamente.");
                actualizarTablaEmpleados();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al modificar empleados: " + ex.getMessage());
            }
        });

        eliminar_empleadoButton.addActionListener(e -> {
            int fila = tabla_de_clientestable1.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(panel1, "Selecciona un empleado para eliminar.");
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(panel1, "¿Estás seguro de eliminar este empleado?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (confirmacion != JOptionPane.YES_OPTION) return;

            int idEmpleado = Integer.parseInt(tabla_de_clientestable1.getValueAt(fila, 0).toString());

            try (Connection conn = Conexion.conectar()) {
                String sql = "DELETE FROM empleados WHERE id_empleado = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, idEmpleado);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(panel1, "Empleado eliminado correctamente.");
                actualizarTablaEmpleados();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al eliminar empleado: " + ex.getMessage());
            }
        });

        ordenar_tablabutton1.addActionListener(e -> {
            String[] columnas = {"ID", "Nombre", "Cédula", "Cargo", "Teléfono", "Fecha Contratación"};
            String[] ordenes = {"Ascendente", "Descendente"};

            String columnaSeleccionada = (String) JOptionPane.showInputDialog(
                    panel1, "Selecciona la columna:", "Ordenar tabla",
                    JOptionPane.QUESTION_MESSAGE, null, columnas, columnas[0]);

            if (columnaSeleccionada == null) return;

            String ordenSeleccionada = (String) JOptionPane.showInputDialog(
                    panel1, "Tipo de orden:", "Ordenar tabla",
                    JOptionPane.QUESTION_MESSAGE, null, ordenes, ordenes[0]);

            if (ordenSeleccionada == null) return;

            int columnaIndex = -1;
            for (int i = 0; i < columnas.length; i++) {
                if (columnas[i].equals(columnaSeleccionada)) {
                    columnaIndex = i;
                    break;
                }
            }

            if (columnaIndex >= 0) {
                SortOrder orden = ordenSeleccionada.equals("Ascendente") ? SortOrder.ASCENDING : SortOrder.DESCENDING;
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tabla_de_clientestable1.getModel());

                sorter.setComparator(0, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));

                tabla_de_clientestable1.setRowSorter(sorter);
                sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
                sorter.sort();
            }
        });

        volverButton.addActionListener(e -> {
            Connection conn = null;
            PreparedStatement pst = null;
            ResultSet rs = null;

            try {
                conn = Conexion.conectar();
                if (conn == null) {
                    JOptionPane.showMessageDialog(null, "No se pudo conectar a la base de datos");
                    return;
                }

                String sql = "SELECT rol FROM usuarios WHERE usuario = ?";
                pst = conn.prepareStatement(sql);
                pst.setString(1, usuarioLogeado.getUsuario()); // Asegúrate que usuarioLogeado tiene getUsuario()

                rs = pst.executeQuery();

                if (rs.next()) {
                    String rol = rs.getString("rol");

                    JFrame ventanaActual = (JFrame) SwingUtilities.getWindowAncestor(panel1);
                    ventanaActual.dispose();

                    if ("administrador".equalsIgnoreCase(rol)) {
                        JFrame ventanaPrincipal = new JFrame("Pantalla Principal del Administrador");
                        ventanaPrincipal.setContentPane(new administrador(usuarioLogeado).panel1);
                        ventanaPrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        ventanaPrincipal.setSize(800, 600);
                        ventanaPrincipal.setLocationRelativeTo(null);
                        ventanaPrincipal.setVisible(true);
                    } else if ("empleado".equalsIgnoreCase(rol)) {
                        JFrame ventanaEmpleado = new JFrame("Pantalla Principal del Empleado");
                        ventanaEmpleado.setContentPane(new empleado(usuarioLogeado).panel1);
                        ventanaEmpleado.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        ventanaEmpleado.setSize(800, 600);
                        ventanaEmpleado.setLocationRelativeTo(null);
                        ventanaEmpleado.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(null, "Rol no reconocido: " + rol);
                    }

                } else {
                    JOptionPane.showMessageDialog(null, "Usuario no encontrado");
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al consultar el rol: " + ex.getMessage());
            } finally {
                try { if (rs != null) rs.close(); } catch (SQLException ex) {}
                try { if (pst != null) pst.close(); } catch (SQLException ex) {}
                try { if (conn != null) conn.close(); } catch (SQLException ex) {}
            }
        });
    }

    private void actualizarTablaEmpleados() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Cédula", "Cargo", "Teléfono", "Fecha Contratación"}, 0);

        try (Connection conn = Conexion.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM empleados")) {

            while (rs.next()) {
                Object[] fila = new Object[6];
                fila[0] = rs.getInt("id_empleado");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("cedula");
                fila[3] = rs.getString("cargo");
                fila[4] = rs.getString("telefono");
                fila[5] = rs.getDate("fecha_contratacion");
                modelo.addRow(fila);
            }

            tabla_de_clientestable1.setModel(modelo);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar empleados: " + ex.getMessage());
        }
    }
}
