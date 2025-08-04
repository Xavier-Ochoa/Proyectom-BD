import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Collections;

public class administrador_bitacora {
    public JPanel panel1;
    private JTable bitacoratable1;
    private JButton ordenarSegunaccionDeButton;
    private JButton actualizarButton;
    private JButton ordenarSegunLaTablaButton;
    private JButton segunElUsuarioButton;
    private JButton ordenarTablaButton;
    private JButton volverButton;

    private User usuarioLogeado;

    public administrador_bitacora(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        actualizarTablaBitacora();

        actualizarButton.addActionListener(e -> actualizarTablaBitacora());

        ordenarSegunaccionDeButton.addActionListener(e -> filtrarPorAccion());

        ordenarSegunLaTablaButton.addActionListener(e -> filtrarPorTabla());

        segunElUsuarioButton.addActionListener(e -> filtrarPorUsuario());

        ordenarTablaButton.addActionListener(e -> ordenarTabla());

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

    private void actualizarTablaBitacora() {
        String[] columnas = {"ID Log", "Nombre Tabla", "Acción", "ID Registro", "Usuario", "Fecha"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            // Para que las celdas no sean editables
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (Connection conn = Conexion.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_log, nombre_tabla, accion, id_registro, usuario, fecha FROM bitacora")) {

            while (rs.next()) {
                Object[] fila = new Object[6];
                fila[0] = rs.getInt("id_log");
                fila[1] = rs.getString("nombre_tabla");
                fila[2] = rs.getString("accion");
                fila[3] = rs.getInt("id_registro");
                fila[4] = rs.getString("usuario");
                fila[5] = rs.getTimestamp("fecha");
                modelo.addRow(fila);
            }

            bitacoratable1.setModel(modelo);
            configurarOrdenamiento(bitacoratable1);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar bitácora: " + ex.getMessage());
        }
    }

    private void filtrarPorAccion() {
        String[] acciones = {"INSERT", "UPDATE", "DELETE"};
        String seleccion = (String) JOptionPane.showInputDialog(panel1,
                "Seleccione acción para filtrar:",
                "Filtrar por acción",
                JOptionPane.QUESTION_MESSAGE,
                null, acciones, acciones[0]);

        if (seleccion == null) return;

        filtrar("accion", seleccion);
    }

    private void filtrarPorTabla() {
        String[] tablas = {
                "categorias",
                "productos",
                "clientes",
                "empleados",
                "proveedores",
                "compras",
                "detalle_compras",
                "ventas",
                "detalle_ventas",
                "inventario",
                "bitacora",
                "promociones",
                "producto_promocion"
        };

        String seleccion = (String) JOptionPane.showInputDialog(
                panel1,
                "Seleccione la tabla para filtrar:",
                "Filtrar por tabla",
                JOptionPane.QUESTION_MESSAGE,
                null,
                tablas,
                tablas[0]);

        if (seleccion == null) return;  // Usuario canceló

        filtrar("nombre_tabla", seleccion);
    }


    private void filtrarPorUsuario() {
        String usuario = JOptionPane.showInputDialog(panel1, "Ingrese el usuario para filtrar:");
        if (usuario == null || usuario.trim().isEmpty()) return;
        filtrar("usuario", usuario.trim());
    }

    private void filtrar(String columna, String valor) {
        String[] columnas = {"ID Log", "Nombre Tabla", "Acción", "ID Registro", "Usuario", "Fecha"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        String sql = "SELECT id_log, nombre_tabla, accion, id_registro, usuario, fecha FROM bitacora WHERE " + columna + " = ?";

        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();

            boolean tieneDatos = false;
            while (rs.next()) {
                Object[] fila = new Object[6];
                fila[0] = rs.getInt("id_log");
                fila[1] = rs.getString("nombre_tabla");
                fila[2] = rs.getString("accion");
                fila[3] = rs.getInt("id_registro");
                fila[4] = rs.getString("usuario");
                fila[5] = rs.getTimestamp("fecha");
                modelo.addRow(fila);
                tieneDatos = true;
            }

            if (!tieneDatos) {
                JOptionPane.showMessageDialog(panel1, "No hay registros para el filtro aplicado.");
            }

            bitacoratable1.setModel(modelo);
            configurarOrdenamiento(bitacoratable1);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al filtrar: " + ex.getMessage());
        }
    }

    private void ordenarTabla() {
        // Preguntar por la columna y orden asc/desc
        String[] columnas = {"ID Log", "Nombre Tabla", "Acción", "ID Registro", "Usuario", "Fecha"};
        String[] ordenes = {"Ascendente", "Descendente"};

        String columnaSeleccionada = (String) JOptionPane.showInputDialog(
                panel1, "Seleccione columna para ordenar:", "Ordenar tabla",
                JOptionPane.QUESTION_MESSAGE, null, columnas, columnas[0]);

        if (columnaSeleccionada == null) return;

        String ordenSeleccionado = (String) JOptionPane.showInputDialog(
                panel1, "Seleccione orden:", "Ordenar tabla",
                JOptionPane.QUESTION_MESSAGE, null, ordenes, ordenes[0]);

        if (ordenSeleccionado == null) return;

        int columnaIndex = -1;
        for (int i = 0; i < columnas.length; i++) {
            if (columnas[i].equals(columnaSeleccionada)) {
                columnaIndex = i;
                break;
            }
        }
        if (columnaIndex == -1) return;

        SortOrder orden = ordenSeleccionado.equals("Ascendente") ? SortOrder.ASCENDING : SortOrder.DESCENDING;

        DefaultTableModel modelo = (DefaultTableModel) bitacoratable1.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

        // Definir comparadores numéricos para columnas id_log (0) y id_registro (3)
        sorter.setComparator(0, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));
        sorter.setComparator(3, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));

        bitacoratable1.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
        sorter.sort();
    }

    private void configurarOrdenamiento(JTable tabla) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

        // Para columnas numéricas:
        sorter.setComparator(0, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));
        sorter.setComparator(3, (a, b) -> Integer.compare(Integer.parseInt(a.toString()), Integer.parseInt(b.toString())));

        tabla.setRowSorter(sorter);
    }
}
