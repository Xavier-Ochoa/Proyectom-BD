import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Collections;

public class administrador_proveedores {
    public JPanel panel1;
    private JTextField ingreso_del_nombretextField1;
    private JTextField ingreso_de_contactotextField2;
    private JTextField ingreso_de_telefonotextField4;
    private JTextField direccion_textField1;
    private JButton registarNuevoEmpleadoButton;
    private JTable tabla_de_proveedortable1;
    private JButton eliminar_empleadoButton;
    private JButton actualizarButton;
    private JButton modificarButton;
    private JButton volverButton;
    private JButton ordenar_tablabutton1;

    private User usuarioLogeado;

    public administrador_proveedores(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        actualizarTablaProveedores();

        registarNuevoEmpleadoButton.addActionListener(e -> registrarProveedor());

        actualizarButton.addActionListener(e -> actualizarTablaProveedores());

        modificarButton.addActionListener(e -> modificarProveedores());

        eliminar_empleadoButton.addActionListener(e -> eliminarProveedor());

        ordenar_tablabutton1.addActionListener(e -> ordenarTabla());

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

    private void registrarProveedor() {
        String nombre = ingreso_del_nombretextField1.getText().trim();
        String contacto = ingreso_de_contactotextField2.getText().trim();
        String telefono = ingreso_de_telefonotextField4.getText().trim();
        String direccion = direccion_textField1.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(panel1, "El nombre del proveedor es obligatorio.");
            return;
        }

        try (Connection conn = Conexion.conectar();
             CallableStatement stmt = conn.prepareCall("{ ? = call ingresar_proveedor(?, ?, ?, ?) }")) {

            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setString(2, nombre);
            stmt.setString(3, contacto.isEmpty() ? null : contacto);
            stmt.setString(4, telefono.isEmpty() ? null : telefono);
            stmt.setString(5, direccion.isEmpty() ? null : direccion);

            stmt.execute();

            int nuevoId = stmt.getInt(1);
            JOptionPane.showMessageDialog(panel1, "Proveedor registrado con ID: " + nuevoId);
            actualizarTablaProveedores();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al registrar proveedor: " + ex.getMessage());
        }
    }

    private void actualizarTablaProveedores() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Contacto", "Teléfono", "Dirección"}, 0);

        try (Connection conn = Conexion.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM proveedores ORDER BY id_proveedor")) {

            while (rs.next()) {
                Object[] fila = new Object[5];
                fila[0] = rs.getInt("id_proveedor");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("contacto");
                fila[3] = rs.getString("telefono");
                fila[4] = rs.getString("direccion");
                modelo.addRow(fila);
            }

            tabla_de_proveedortable1.setModel(modelo);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar proveedores: " + ex.getMessage());
        }
    }

    private void modificarProveedores() {
        if (tabla_de_proveedortable1.isEditing()) {
            tabla_de_proveedortable1.getCellEditor().stopCellEditing();
        }

        DefaultTableModel modelo = (DefaultTableModel) tabla_de_proveedortable1.getModel();
        int filas = modelo.getRowCount();
        int modificados = 0;

        try (Connection conn = Conexion.conectar()) {
            for (int i = 0; i < filas; i++) {
                int id = Integer.parseInt(modelo.getValueAt(i, 0).toString());
                String nombre = modelo.getValueAt(i, 1).toString().trim();
                String contacto = modelo.getValueAt(i, 2).toString().trim();
                String telefono = modelo.getValueAt(i, 3).toString().trim();
                String direccion = modelo.getValueAt(i, 4).toString().trim();

                String sql = "UPDATE proveedores SET nombre = ?, contacto = ?, telefono = ?, direccion = ? WHERE id_proveedor = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, contacto);
                    stmt.setString(3, telefono);
                    stmt.setString(4, direccion);
                    stmt.setInt(5, id);
                    stmt.executeUpdate();
                    modificados++;
                }
            }

            JOptionPane.showMessageDialog(panel1, modificados + " proveedor(es) modificados correctamente.");
            actualizarTablaProveedores();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al modificar proveedores: " + ex.getMessage());
        }
    }

    private void eliminarProveedor() {
        int fila = tabla_de_proveedortable1.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(panel1, "Selecciona un proveedor para eliminar.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(panel1, "¿Estás seguro de eliminar este proveedor?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        int idProveedor = Integer.parseInt(tabla_de_proveedortable1.getValueAt(fila, 0).toString());

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM proveedores WHERE id_proveedor = ?")) {

            stmt.setInt(1, idProveedor);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(panel1, "Proveedor eliminado correctamente.");
            actualizarTablaProveedores();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al eliminar proveedor: " + ex.getMessage());
        }
    }

    private void ordenarTabla() {
        String[] columnas = {"ID", "Nombre", "Contacto", "Teléfono", "Dirección"};
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
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tabla_de_proveedortable1.getModel());
            sorter.setComparator(0, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));
            tabla_de_proveedortable1.setRowSorter(sorter);
            sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
            sorter.sort();
        }
    }
}
