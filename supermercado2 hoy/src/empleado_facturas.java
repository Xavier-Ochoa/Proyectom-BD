import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Vector;
import java.awt.BorderLayout;

public class empleado_facturas extends JFrame {
    public JPanel panel1;
    private JTable ventas_table1;
    private JTable productos_de_ventatable2;
    private JButton actualizarrbutton1;
    private JButton generar_PDFButton;
    private JButton volverrButton;
    private User usuarioLogeado;

    private Connection conn;

    // IVA 15%
    private static final double IVA = 0.15;

    public empleado_facturas(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;
        setTitle("Ventas y Detalles");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel1 = new JPanel(new BorderLayout());

        ventas_table1 = new JTable();
        JScrollPane scrollVentas = new JScrollPane(ventas_table1);
        scrollVentas.setBorder(BorderFactory.createTitledBorder("Ventas"));

        productos_de_ventatable2 = new JTable();
        JScrollPane scrollDetalles = new JScrollPane(productos_de_ventatable2);
        scrollDetalles.setBorder(BorderFactory.createTitledBorder("Detalles de la venta seleccionada"));

        actualizarrbutton1 = new JButton("Refrescar ventas");
        generar_PDFButton = new JButton("Generar factura PDF");
        volverrButton = new JButton("Volver");

        JPanel panelBoton = new JPanel();
        panelBoton.add(actualizarrbutton1);
        panelBoton.add(generar_PDFButton);
        panelBoton.add(volverrButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollVentas, scrollDetalles);
        splitPane.setDividerLocation(300);

        panel1.add(splitPane, BorderLayout.CENTER);
        panel1.add(panelBoton, BorderLayout.SOUTH);

        setContentPane(panel1);

        conn = Conexion.conectar();

        cargarVentas();

        ventas_table1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && ventas_table1.getSelectedRow() != -1) {
                    int fila = ventas_table1.getSelectedRow();
                    int idVenta = (int) ventas_table1.getValueAt(fila, 0);
                    cargarDetallesVenta(idVenta);
                }
            }
        });

        actualizarrbutton1.addActionListener(e -> cargarVentas());

        generar_PDFButton.addActionListener(e -> {
            int filaSeleccionada = ventas_table1.getSelectedRow();
            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una venta para generar la factura.");
                return;
            }
            int idVenta = (int) ventas_table1.getValueAt(filaSeleccionada, 0);
            generarFacturaPDF(idVenta);
        });
        volverrButton.addActionListener(new ActionListener() {
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

    private void cargarVentas() {
        try {
            String sql = "SELECT id_venta, id_cliente, id_empleado, fecha_venta, total FROM ventas ORDER BY fecha_venta DESC";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel modelVentas = new DefaultTableModel(
                    new Object[]{"ID Venta", "ID Cliente", "ID Empleado", "Fecha Venta", "Total"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                Vector<Object> fila = new Vector<>();
                fila.add(rs.getInt("id_venta"));
                fila.add(rs.getInt("id_cliente"));
                fila.add(rs.getInt("id_empleado"));
                fila.add(rs.getDate("fecha_venta"));
                fila.add(rs.getBigDecimal("total"));
                modelVentas.addRow(fila);
            }
            ventas_table1.setModel(modelVentas);

            productos_de_ventatable2.setModel(new DefaultTableModel());

            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando ventas: " + e.getMessage());
        }
    }

    private void cargarDetallesVenta(int idVenta) {
        try {
            String sql = "SELECT dv.id_producto, p.nombre, dv.cantidad, dv.precio_unitario, " +
                    "(dv.cantidad * dv.precio_unitario) AS subtotal " +
                    "FROM detalle_ventas dv " +
                    "JOIN productos p ON dv.id_producto = p.id_producto " +
                    "WHERE dv.id_venta = ?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idVenta);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel modelDetalles = new DefaultTableModel(
                    new Object[]{"ID Producto", "Nombre Producto", "Cantidad", "Precio Unitario", "Subtotal"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            double totalVenta = 0;

            while (rs.next()) {
                int cantidad = rs.getInt("cantidad");
                double precioUnitario = rs.getDouble("precio_unitario");
                double subtotal = cantidad * precioUnitario;
                totalVenta += subtotal;

                Vector<Object> fila = new Vector<>();
                fila.add(rs.getInt("id_producto"));
                fila.add(rs.getString("nombre"));
                fila.add(cantidad);
                fila.add(precioUnitario);
                fila.add(subtotal);

                modelDetalles.addRow(fila);
            }

            Vector<Object> filaTotal = new Vector<>();
            filaTotal.add("");
            filaTotal.add("TOTAL");
            filaTotal.add("");
            filaTotal.add("");
            filaTotal.add(totalVenta);
            modelDetalles.addRow(filaTotal);

            productos_de_ventatable2.setModel(modelDetalles);

            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando detalles: " + e.getMessage());
        }
    }

    private void generarFacturaPDF(int idVenta) {
        try {
            // Obtener info de la venta
            String sqlVenta = "SELECT v.id_venta, v.fecha_venta, v.total, c.nombre AS cliente_nombre, e.nombre AS empleado_nombre " +
                    "FROM ventas v " +
                    "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                    "LEFT JOIN empleados e ON v.id_empleado = e.id_empleado " +
                    "WHERE v.id_venta = ?";
            PreparedStatement pstVenta = conn.prepareStatement(sqlVenta);
            pstVenta.setInt(1, idVenta);
            ResultSet rsVenta = pstVenta.executeQuery();

            if (!rsVenta.next()) {
                JOptionPane.showMessageDialog(this, "No se encontró la venta seleccionada.");
                return;
            }

            String clienteNombre = rsVenta.getString("cliente_nombre");
            String empleadoNombre = rsVenta.getString("empleado_nombre");
            Date fechaVenta = rsVenta.getDate("fecha_venta");

            // Obtener detalles
            String sqlDetalles = "SELECT p.nombre, dv.cantidad, dv.precio_unitario " +
                    "FROM detalle_ventas dv " +
                    "JOIN productos p ON dv.id_producto = p.id_producto " +
                    "WHERE dv.id_venta = ?";
            PreparedStatement pstDetalles = conn.prepareStatement(sqlDetalles);
            pstDetalles.setInt(1, idVenta);
            ResultSet rsDetalles = pstDetalles.executeQuery();

            // Crear documento PDF
            Document document = new Document();
            String fileName = "Factura_Venta_" + idVenta + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Agregar título con fuente iText correcta
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph title = new Paragraph("Factura de Venta", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("ID Venta: " + idVenta));
            document.add(new Paragraph("Fecha: " + fechaVenta));
            document.add(new Paragraph("Cliente: " + (clienteNombre != null ? clienteNombre : "N/A")));
            document.add(new Paragraph("Empleado: " + (empleadoNombre != null ? empleadoNombre : "N/A")));
            document.add(new Paragraph(" "));

            // Tabla con detalles
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3, 7, 2, 3, 3});
            tabla.addCell("Producto");
            tabla.addCell("Descripción");
            tabla.addCell("Cantidad");
            tabla.addCell("Precio Unitario");
            tabla.addCell("Subtotal");

            double subtotalTotal = 0;

            while (rsDetalles.next()) {
                String nombreProducto = rsDetalles.getString("nombre");
                int cantidad = rsDetalles.getInt("cantidad");
                double precioUnitario = rsDetalles.getDouble("precio_unitario");
                double subtotal = cantidad * precioUnitario;
                subtotalTotal += subtotal;

                tabla.addCell(nombreProducto);
                tabla.addCell(""); // Puedes agregar descripción si la tienes
                tabla.addCell(String.valueOf(cantidad));
                tabla.addCell(String.format("%.2f", precioUnitario));
                tabla.addCell(String.format("%.2f", subtotal));
            }

            document.add(tabla);

            // Calcular IVA y total final
            double ivaMonto = subtotalTotal * IVA;
            double totalFinal = subtotalTotal + ivaMonto;

            document.add(new Paragraph(" "));
            document.add(new Paragraph(String.format("Subtotal: %.2f", subtotalTotal)));
            document.add(new Paragraph(String.format("IVA (%.0f%%): %.2f", IVA * 100, ivaMonto)));
            document.add(new Paragraph(String.format("Total Final: %.2f", totalFinal)));

            document.close();

            JOptionPane.showMessageDialog(this, "Factura generada: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generando factura PDF: " + e.getMessage());
        }
    }
}
