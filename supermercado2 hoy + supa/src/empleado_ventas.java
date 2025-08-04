import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import javax.swing.table.DefaultTableModel;


public class empleado_ventas {
    public JPanel panel1;
    private JTextField id_cliente_que_compratextField1;
    private JButton nueva_ventaButton_de_venta;
    private JTable lista_productos_venderantable1;
    private JButton guardarButton_de_venta;
    private JLabel id_empleado_que_logeo_que_vende;
    private JButton buscarProductoYCantidadButton;
    private JButton buscar_id_clienteButton;
    private JLabel muestra_el_TOTAL;
    private JButton volverButton;
    private JLabel ivaa;
    private JLabel total_finall;

    private User usuarioLogeado;

    public empleado_ventas(User usuario) {
        this.usuarioLogeado = usuario;
        lista_productos_venderantable1.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID Producto", "Nombre", "Cantidad", "Precio Unitario", "Subtotal"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        // Mostrar id_empleado en la etiqueta
        id_empleado_que_logeo_que_vende.setText(String.valueOf(usuarioLogeado.getIdEmpleado()));

        buscar_id_clienteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cedula = JOptionPane.showInputDialog(null, "Ingrese la cédula del cliente:", "Buscar Cliente", JOptionPane.QUESTION_MESSAGE);
                if (cedula != null && !cedula.trim().isEmpty()) {
                    buscarClientePorCedula(cedula.trim());
                }
            }
        });
        buscarProductoYCantidadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarVentanaSeleccionProducto();
            }
        });
        guardarButton_de_venta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarVenta();
            }
        });
        nueva_ventaButton_de_venta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Limpiar el campo del cliente
                id_cliente_que_compratextField1.setText("");

                // Limpiar la tabla de productos seleccionados
                DefaultTableModel model = (DefaultTableModel) lista_productos_venderantable1.getModel();
                model.setRowCount(0); // elimina todas las filas
                muestra_el_TOTAL.setText("$ 0.00");
                ivaa.setText("");
                total_finall.setText("");
                // (Opcional) Mostrar mensaje
                JOptionPane.showMessageDialog(null, "Formulario preparado para una nueva venta.");



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

    private void buscarClientePorCedula(String cedula) {
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE cedula = ?")) {

            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int idCliente = rs.getInt("id_cliente");
                id_cliente_que_compratextField1.setText(String.valueOf(idCliente));
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró un cliente con esa cédula.", "Cliente no encontrado", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al buscar cliente: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void mostrarVentanaSeleccionProducto() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Seleccionar Producto");
        dialog.setModal(true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);

        DefaultTableModel model = new DefaultTableModel();
        JTable productoTable = new JTable(model);
        model.addColumn("ID");
        model.addColumn("Nombre");
        model.addColumn("Precio");
        model.addColumn("Stock");

        // Cargar productos desde la base de datos
        try (Connection conn = Conexion.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT p.id_producto, p.nombre, p.precio, i.stock_actual " +
                             "FROM productos p JOIN inventario i ON p.id_producto = i.id_producto")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio"),
                        rs.getInt("stock_actual")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al cargar productos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JScrollPane scrollPane = new JScrollPane(productoTable);
        dialog.add(scrollPane);

        productoTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = productoTable.getSelectedRow();
                if (row != -1) {
                    int idProducto = (int) model.getValueAt(row, 0);
                    String nombre = (String) model.getValueAt(row, 1);
                    double precio = ((Number) model.getValueAt(row, 2)).doubleValue();
                    int stock = (int) model.getValueAt(row, 3);

                    String inputCantidad = JOptionPane.showInputDialog(
                            dialog,
                            "¿Cuántos deseas comprar? (máx. " + stock + ")",
                            "Cantidad para " + nombre,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (inputCantidad != null) {
                        try {
                            int cantidad = Integer.parseInt(inputCantidad);
                            if (cantidad <= 0 || cantidad > stock) {
                                JOptionPane.showMessageDialog(dialog, "Cantidad inválida. Debe ser entre 1 y " + stock);
                                return;
                            }

                            // Agregar a la JTable lista_productos_venderantable1
                            DefaultTableModel tablaVenta = (DefaultTableModel) lista_productos_venderantable1.getModel();

                            // Agregar columnas si no existen
                            if (tablaVenta.getColumnCount() == 0) {
                                tablaVenta.addColumn("ID Producto");
                                tablaVenta.addColumn("Nombre");
                                tablaVenta.addColumn("Cantidad");
                                tablaVenta.addColumn("Precio Unitario");
                                tablaVenta.addColumn("Subtotal");
                            }

                            double subtotal = cantidad * precio;

                            tablaVenta.addRow(new Object[]{
                                    idProducto, nombre, cantidad, precio, subtotal
                            });

                            actualizarTotalVenta();


                            dialog.dispose();

                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(dialog, "Ingrese un número válido.");
                        }
                    }
                }
            }
        });

        dialog.setVisible(true);
    }

    private void actualizarTotalVenta() {
        DefaultTableModel model = (DefaultTableModel) lista_productos_venderantable1.getModel();
        double total = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object valor = model.getValueAt(i, 4); // columna Subtotal
            if (valor instanceof Number) {
                total += ((Number) valor).doubleValue();
            }
        }

        // Mostrar total base
        muestra_el_TOTAL.setText(String.format("Total: $ %.2f", total));

        // Calcular IVA y Total Final solo si hay total mayor a 0
        if (total > 0) {
            double iva = total * 0.12;
            double totalFinal = total + iva;

            ivaa.setText(String.format("IVA (12%%): $ %.2f", iva));
            total_finall.setText(String.format("Total Final: $ %.2f", totalFinal));
        } else {
            ivaa.setText("");
            total_finall.setText("");
        }
    }



    private void ejecutarVenta() {
        try {
            int idCliente = Integer.parseInt(id_cliente_que_compratextField1.getText().trim());
            int idEmpleado = Integer.parseInt(id_empleado_que_logeo_que_vende.getText().trim());

            DefaultTableModel model = (DefaultTableModel) lista_productos_venderantable1.getModel();
            int rowCount = model.getRowCount();

            if (rowCount == 0) {
                JOptionPane.showMessageDialog(null, "Debe agregar al menos un producto.");
                return;
            }

            Integer[] productos = new Integer[rowCount];
            Integer[] cantidades = new Integer[rowCount];

            for (int i = 0; i < rowCount; i++) {
                productos[i] = (Integer) model.getValueAt(i, 0);   // ID Producto
                cantidades[i] = (Integer) model.getValueAt(i, 2);  // Cantidad
            }

            try (Connection conn = Conexion.conectar()) {
                String sql = "SELECT realizar_venta_FN(?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, idCliente);
                    stmt.setInt(2, idEmpleado);
                    stmt.setArray(3, conn.createArrayOf("INTEGER", productos));
                    stmt.setArray(4, conn.createArrayOf("INTEGER", cantidades));

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int idVenta = rs.getInt(1);
                        JOptionPane.showMessageDialog(null, "¡Venta realizada exitosamente!\nID Venta: " + idVenta);
                    }
                }
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Error: ID Cliente o ID Empleado inválidos.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al realizar la venta: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }



}
