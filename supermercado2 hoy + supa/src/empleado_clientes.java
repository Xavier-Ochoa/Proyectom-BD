import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class empleado_clientes {
    public JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JTextField ingreso_del_nombretextField1;
    private JTextField ingreso_de_cedulatextField2;
    private JTextField ingreso_de_correotextField3;
    private JTextField ingreso_de_telefonotextField4;
    private JTextField ingreso_de_direcciontextField5;
    private JButton registarNuevoClienteButton;
    private JButton eliminar_cliuenteButton;
    private JButton actualizarButton;
    private JTable tabla_de_clientestable1;
    private JButton modificarButton;
    private JButton volverButton;
    private JButton ordenarTablaButton;

    private Connection conn;
    private Object[][] datosOriginales; // Datos originales para detectar cambios
    private User usuarioLogeado;

    public empleado_clientes(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado; // ✅ Guardamos el usuario que se pasa desde la clase anterior
        conn = Conexion.conectar();

        if (conn == null) {
            JOptionPane.showMessageDialog(panel1, "No se pudo establecer la conexión a la base de datos.");
            return;
        }

        cargarClientesEnTabla();

        registarNuevoClienteButton.addActionListener(e -> registrarCliente());
        eliminar_cliuenteButton.addActionListener(e -> eliminarClienteSeleccionado());
        actualizarButton.addActionListener(e -> cargarClientesEnTabla());
        modificarButton.addActionListener(e -> modificarClientesEditados());

        ordenarTablaButton.addActionListener(e -> ordenarTabla());

        volverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            }
        });
    }

    private void ordenarTabla() {
        String[] columnas = {"ID", "Cédula", "Nombre", "Email", "Teléfono", "Dirección"};
        String[] ordenes = {"Ascendente", "Descendente"};

        String columnaSeleccionada = (String) JOptionPane.showInputDialog(
                panel1,
                "Selecciona la columna por la que deseas ordenar:",
                "Ordenar por columna",
                JOptionPane.QUESTION_MESSAGE,
                null,
                columnas,
                columnas[0]
        );

        if (columnaSeleccionada == null) return;

        String ordenSeleccionado = (String) JOptionPane.showInputDialog(
                panel1,
                "Selecciona el tipo de orden:",
                "Tipo de orden",
                JOptionPane.QUESTION_MESSAGE,
                null,
                ordenes,
                ordenes[0]
        );

        if (ordenSeleccionado == null) return;

        int columnaIndex = -1;
        for (int i = 0; i < columnas.length; i++) {
            if (columnas[i].equals(columnaSeleccionada)) {
                columnaIndex = i;
                break;
            }
        }

        if (columnaIndex >= 0) {
            SortOrder orden = ordenSeleccionado.equals("Ascendente") ? SortOrder.ASCENDING : SortOrder.DESCENDING;
            DefaultTableModel modelo = (DefaultTableModel) tabla_de_clientestable1.getModel();
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

            // Configurar comparador numérico solo para columna ID (col 0)
            sorter.setComparator(0, (o1, o2) -> {
                Integer i1 = Integer.parseInt(o1.toString());
                Integer i2 = Integer.parseInt(o2.toString());
                return i1.compareTo(i2);
            });

            tabla_de_clientestable1.setRowSorter(sorter);
            sorter.setSortKeys(java.util.Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
            sorter.sort();
        }
    }

    private void registrarCliente() {
        String nombre = ingreso_del_nombretextField1.getText().trim();
        String cedula = ingreso_de_cedulatextField2.getText().trim();
        String email = ingreso_de_correotextField3.getText().trim();
        String telefono = ingreso_de_telefonotextField4.getText().trim();
        String direccion = ingreso_de_direcciontextField5.getText().trim();

        if (nombre.isEmpty() || cedula.isEmpty()) {
            JOptionPane.showMessageDialog(panel1, "El nombre y la cédula son obligatorios.");
            return;
        }

        try {
            String sql = "INSERT INTO clientes (cedula, nombre, email, telefono, direccion) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            stmt.setString(2, nombre);
            stmt.setString(3, email.isEmpty() ? null : email);
            stmt.setString(4, telefono.isEmpty() ? null : telefono);
            stmt.setString(5, direccion.isEmpty() ? null : direccion);
            stmt.executeUpdate();
            stmt.close();

            JOptionPane.showMessageDialog(panel1, "Cliente registrado correctamente.");
            limpiarCampos();
            cargarClientesEnTabla();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al registrar cliente: " + e.getMessage());
        }
    }

    private void eliminarClienteSeleccionado() {
        int fila = tabla_de_clientestable1.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(panel1, "Selecciona un cliente para eliminar.");
            return;
        }

        int idCliente = (int) tabla_de_clientestable1.getValueAt(fila, 0);
        int confirmacion = JOptionPane.showConfirmDialog(panel1, "¿Estás seguro de eliminar este cliente?", "Confirmación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM clientes WHERE id_cliente = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, idCliente);
                stmt.executeUpdate();
                stmt.close();

                JOptionPane.showMessageDialog(panel1, "Cliente eliminado correctamente.");
                cargarClientesEnTabla();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(panel1, "Error al eliminar cliente: " + e.getMessage());
            }
        }
    }

    private void cargarClientesEnTabla() {
        if (conn == null) {
            JOptionPane.showMessageDialog(panel1, "No hay conexión con la base de datos.");
            return;
        }

        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Cédula", "Nombre", "Email", "Teléfono", "Dirección"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // ID no editable
            }
        };

        try {
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery("SELECT * FROM clientes ORDER BY id_cliente");

            rs.last();
            int totalFilas = rs.getRow();
            rs.beforeFirst();

            datosOriginales = new Object[totalFilas][6];
            int i = 0;

            while (rs.next()) {
                Object[] fila = new Object[]{
                        rs.getInt("id_cliente"),
                        rs.getString("cedula"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("direccion")
                };
                modelo.addRow(fila);
                datosOriginales[i] = fila;
                i++;
            }

            tabla_de_clientestable1.setModel(modelo);
            stmt.close();
            rs.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar clientes: " + e.getMessage());
        }
    }

    private void modificarClientesEditados() {
        if (tabla_de_clientestable1.isEditing()) {
            tabla_de_clientestable1.getCellEditor().stopCellEditing();
        }

        DefaultTableModel modelo = (DefaultTableModel) tabla_de_clientestable1.getModel();
        int filas = modelo.getRowCount();
        int modificados = 0;

        try {
            for (int i = 0; i < filas; i++) {
                boolean filaModificada = false;
                Object[] filaActual = new Object[6];

                for (int j = 0; j < 6; j++) {
                    filaActual[j] = modelo.getValueAt(i, j);
                }

                for (int j = 0; j < 6; j++) {
                    Object original = datosOriginales[i][j];
                    Object actual = filaActual[j];

                    if ((original == null && actual != null) ||
                            (original != null && !original.equals(actual))) {
                        filaModificada = true;
                        break;
                    }
                }

                if (filaModificada) {
                    int id = (int) filaActual[0];

                    String cedula = filaActual[1] != null ? filaActual[1].toString().trim() : "";
                    String nombre = filaActual[2] != null ? filaActual[2].toString().trim() : "";

                    if (cedula.isEmpty() || nombre.isEmpty()) {
                        JOptionPane.showMessageDialog(panel1, "Cédula y nombre no pueden estar vacíos en la fila " + (i + 1));
                        continue;
                    }

                    String email = filaActual[3] != null ?
                            filaActual[3].toString().trim() :
                            datosOriginales[i][3] != null ? datosOriginales[i][3].toString() : null;
                    if (email != null && email.isEmpty()) email = null;

                    String telefono = filaActual[4] != null ?
                            filaActual[4].toString().trim() :
                            datosOriginales[i][4] != null ? datosOriginales[i][4].toString() : null;
                    if (telefono != null && telefono.isEmpty()) telefono = null;

                    String direccion = filaActual[5] != null ?
                            filaActual[5].toString().trim() :
                            datosOriginales[i][5] != null ? datosOriginales[i][5].toString() : null;
                    if (direccion != null && direccion.isEmpty()) direccion = null;

                    String sql = "UPDATE clientes SET cedula = ?, nombre = ?, email = ?, telefono = ?, direccion = ? WHERE id_cliente = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, cedula);
                        stmt.setString(2, nombre);
                        stmt.setString(3, email);
                        stmt.setString(4, telefono);
                        stmt.setString(5, direccion);
                        stmt.setInt(6, id);

                        stmt.executeUpdate();
                        modificados++;
                    }
                }
            }

            if (modificados == 0) {
                JOptionPane.showMessageDialog(panel1, "No se detectaron cambios.");
            } else {
                JOptionPane.showMessageDialog(panel1, modificados + " cliente(s) modificados.");
                datosOriginales = new Object[filas][6];
                for (int i = 0; i < filas; i++) {
                    for (int j = 0; j < 6; j++) {
                        datosOriginales[i][j] = modelo.getValueAt(i, j);
                    }
                }
            }

            cargarClientesEnTabla();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al modificar clientes: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        ingreso_del_nombretextField1.setText("");
        ingreso_de_cedulatextField2.setText("");
        ingreso_de_correotextField3.setText("");
        ingreso_de_telefonotextField4.setText("");
        ingreso_de_direcciontextField5.setText("");
    }
}
