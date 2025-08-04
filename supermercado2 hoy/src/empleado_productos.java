import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Comparator;

public class empleado_productos {
    public JPanel panel1;
    private JTextField ingreso_del_nombre_productotextField1;
    private JTextField ingreso_de_preciotextField3;
    private JTextField ingreso_de_stockactualtextField5;
    private JButton registarNuevoproducClienteButton;
    private JTable tabla_de_productostable1;
    private JButton eliminar_productButton;
    private JButton actualizar_tablaButton;
    private JButton modificarButton;
    private JButton volverButton;
    private JTextField stock_minimotextField1;
    private JComboBox categorias_comboBox1;
    private JTextArea descripcion_produc_textArea1;
    private JButton ordenarTablaButton;
    private User usuarioLogeado;

    public empleado_productos(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        cargarCategoriasEnComboBox();
        actualizarTablaProductos();

        registarNuevoproducClienteButton.addActionListener(e -> {
            String nombre = ingreso_del_nombre_productotextField1.getText().trim();
            String descripcion = descripcion_produc_textArea1.getText().trim();
            String precioStr = ingreso_de_preciotextField3.getText().trim();
            String stockActualStr = ingreso_de_stockactualtextField5.getText().trim();
            String stockMinimoStr = stock_minimotextField1.getText().trim();
            String categoriaNombre = (String) categorias_comboBox1.getSelectedItem();

            if (nombre.isEmpty() || precioStr.isEmpty() || stockActualStr.isEmpty() || stockMinimoStr.isEmpty() || categoriaNombre == null) {
                JOptionPane.showMessageDialog(panel1, "Todos los campos obligatorios deben estar completos.");
                return;
            }

            try {
                BigDecimal precio = new BigDecimal(precioStr);
                int stockActual = Integer.parseInt(stockActualStr);
                int stockMinimo = Integer.parseInt(stockMinimoStr);

                int idCategoria = -1;
                try (Connection conn = Conexion.conectar();
                     PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria FROM categorias WHERE nombre_categoria = ?")) {
                    stmt.setString(1, categoriaNombre);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        idCategoria = rs.getInt("id_categoria");
                    } else {
                        JOptionPane.showMessageDialog(panel1, "Categoría seleccionada no válida.");
                        return;
                    }
                }

                try (Connection conn = Conexion.conectar();
                     CallableStatement call = conn.prepareCall("SELECT ingresar_producto_con_stock(?, ?, ?, ?, ?, ?)")) {
                    call.setString(1, nombre);
                    call.setString(2, descripcion.isEmpty() ? null : descripcion);
                    call.setBigDecimal(3, precio);
                    call.setInt(4, idCategoria);
                    call.setInt(5, stockActual);
                    call.setInt(6, stockMinimo);

                    call.execute();

                    JOptionPane.showMessageDialog(panel1, "Producto registrado exitosamente.");
                    actualizarTablaProductos();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel1, "Precio, stock actual y mínimo deben ser numéricos.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al registrar el producto: " + ex.getMessage());
            }
        });

        actualizar_tablaButton.addActionListener(e -> actualizarTablaProductos());

        modificarButton.addActionListener(e -> {
            if (tabla_de_productostable1.isEditing()) {
                tabla_de_productostable1.getCellEditor().stopCellEditing();
            }

            DefaultTableModel modelo = (DefaultTableModel) tabla_de_productostable1.getModel();
            int filas = modelo.getRowCount();
            int modificados = 0;

            try (Connection conn = Conexion.conectar()) {
                for (int i = 0; i < filas; i++) {
                    int idProducto = (int) modelo.getValueAt(i, 0);
                    String nombre = modelo.getValueAt(i, 1).toString().trim();
                    String descripcion = modelo.getValueAt(i, 2) != null ? modelo.getValueAt(i, 2).toString().trim() : null;
                    BigDecimal precio = new BigDecimal(modelo.getValueAt(i, 3).toString());
                    String categoriaNombre = modelo.getValueAt(i, 4).toString().trim();
                    int stockActual = Integer.parseInt(modelo.getValueAt(i, 5).toString());
                    int stockMinimo = Integer.parseInt(modelo.getValueAt(i, 6).toString());

                    int idCategoria = -1;
                    try (PreparedStatement catStmt = conn.prepareStatement("SELECT id_categoria FROM categorias WHERE nombre_categoria = ?")) {
                        catStmt.setString(1, categoriaNombre);
                        ResultSet rs = catStmt.executeQuery();
                        if (rs.next()) {
                            idCategoria = rs.getInt("id_categoria");
                        } else {
                            JOptionPane.showMessageDialog(panel1, "Categoría no válida en fila " + (i + 1));
                            continue;
                        }
                    }

                    String sqlProd = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, id_categoria = ? WHERE id_producto = ?";
                    try (PreparedStatement prodStmt = conn.prepareStatement(sqlProd)) {
                        prodStmt.setString(1, nombre);
                        prodStmt.setString(2, descripcion);
                        prodStmt.setBigDecimal(3, precio);
                        prodStmt.setInt(4, idCategoria);
                        prodStmt.setInt(5, idProducto);
                        prodStmt.executeUpdate();
                    }

                    String sqlInv = "UPDATE inventario SET stock_actual = ?, stock_minimo = ? WHERE id_producto = ?";
                    try (PreparedStatement invStmt = conn.prepareStatement(sqlInv)) {
                        invStmt.setInt(1, stockActual);
                        invStmt.setInt(2, stockMinimo);
                        invStmt.setInt(3, idProducto);
                        invStmt.executeUpdate();
                    }

                    modificados++;
                }

                JOptionPane.showMessageDialog(panel1, modificados + " producto(s) modificados correctamente.");
                actualizarTablaProductos();

            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al modificar productos: " + ex.getMessage());
            }
        });

        ordenarTablaButton.addActionListener(e -> {
            String[] columnas = {"ID", "Nombre", "Descripción", "Precio", "Categoría", "Stock Actual", "Stock Mínimo"};
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

            String ordenSeleccionada = (String) JOptionPane.showInputDialog(
                    panel1,
                    "Selecciona el tipo de orden:",
                    "Tipo de orden",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    ordenes,
                    ordenes[0]
            );

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
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tabla_de_productostable1.getModel());

// Forzar orden numérico en columnas específicas
                sorter.setComparator(0, (o1, o2) -> Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()))); // ID
                sorter.setComparator(3, (o1, o2) -> new BigDecimal(o1.toString()).compareTo(new BigDecimal(o2.toString()))); // Precio
                sorter.setComparator(5, (o1, o2) -> Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()))); // Stock Actual
                sorter.setComparator(6, (o1, o2) -> Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()))); // Stock Mínimo


                tabla_de_productostable1.setRowSorter(sorter);

                tabla_de_productostable1.setRowSorter(sorter);
                sorter.setSortKeys(java.util.Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
                sorter.sort();
            }
        });

        eliminar_productButton.addActionListener(e -> {
            int filaSeleccionada = tabla_de_productostable1.getSelectedRow();
            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(panel1, "Selecciona un producto para eliminar.");
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(panel1, "¿Estás seguro de que deseas eliminar este producto?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (confirmacion != JOptionPane.YES_OPTION) return;

            int idProducto = (int) tabla_de_productostable1.getValueAt(filaSeleccionada, 0);

            try (Connection conn = Conexion.conectar()) {
                String sqlInv = "DELETE FROM inventario WHERE id_producto = ?";
                try (PreparedStatement stmtInv = conn.prepareStatement(sqlInv)) {
                    stmtInv.setInt(1, idProducto);
                    stmtInv.executeUpdate();
                }

                String sqlProd = "DELETE FROM productos WHERE id_producto = ?";
                try (PreparedStatement stmtProd = conn.prepareStatement(sqlProd)) {
                    stmtProd.setInt(1, idProducto);
                    stmtProd.executeUpdate();
                }

                JOptionPane.showMessageDialog(panel1, "Producto eliminado correctamente.");
                actualizarTablaProductos();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel1, "Error al eliminar el producto: " + ex.getMessage());
            }
        });
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

    private void cargarCategoriasEnComboBox() {
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT nombre_categoria FROM categorias");
             ResultSet rs = stmt.executeQuery()) {

            categorias_comboBox1.removeAllItems();

            while (rs.next()) {
                categorias_comboBox1.addItem(rs.getString("nombre_categoria"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void actualizarTablaProductos() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Descripción", "Precio", "Categoría", "Stock Actual", "Stock Mínimo"}, 0);

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT p.id_producto, p.nombre, p.descripcion, p.precio, c.nombre_categoria,
                       i.stock_actual, i.stock_minimo
                FROM productos p
                JOIN categorias c ON p.id_categoria = c.id_categoria
                JOIN inventario i ON p.id_producto = i.id_producto
             """);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[7];
                fila[0] = rs.getInt("id_producto");
                fila[1] = rs.getString("nombre");
                fila[2] = rs.getString("descripcion");
                fila[3] = rs.getBigDecimal("precio");
                fila[4] = rs.getString("nombre_categoria");
                fila[5] = rs.getInt("stock_actual");
                fila[6] = rs.getInt("stock_minimo");
                modelo.addRow(fila);
            }

            tabla_de_productostable1.setModel(modelo);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar productos: " + e.getMessage());
        }
    }
}
