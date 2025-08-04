import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class empleado_categ {
    public JPanel panel1;
    private JTextField ingreso_del_nombretextField1;
    private JButton registarNuevoCategoriaButton;
    private JTable tabla_de_categoriastable1;
    private JButton eliminar_cliuenteButton;
    private JButton actualizarButton;
    private JButton modificarButton;
    private JButton volverButton;
    private JLabel descripc_jlabel;
    private JCheckBox es_perecible_checkBox1;
    private JTextArea descrip_categ_textArea1;
    private JButton ordenar_tabla_button1;
    private User usuarioLogeado;

    public empleado_categ(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        actualizarTablaCategorias();

        registarNuevoCategoriaButton.addActionListener(e -> insertarCategoria());

        actualizarButton.addActionListener(e -> actualizarTablaCategorias());

        modificarButton.addActionListener(e -> modificarCategorias());

        eliminar_cliuenteButton.addActionListener(e -> eliminarCategoria());

        ordenar_tabla_button1.addActionListener(e -> ordenarTabla());

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

    private void insertarCategoria() {
        String nombre = ingreso_del_nombretextField1.getText().trim();
        String descripcion = descrip_categ_textArea1.getText().trim();
        boolean esPerecible = es_perecible_checkBox1.isSelected();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(panel1, "El nombre de la categoría es obligatorio.");
            return;
        }

        try (Connection conn = Conexion.conectar();
             CallableStatement stmt = conn.prepareCall("SELECT insertar_categoria(?, ?, ?)")) {

            stmt.setString(1, nombre);
            stmt.setString(2, descripcion.isEmpty() ? null : descripcion);
            stmt.setBoolean(3, esPerecible);

            stmt.execute();

            JOptionPane.showMessageDialog(panel1, "Categoría registrada exitosamente.");
            actualizarTablaCategorias();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al insertar categoría: " + ex.getMessage());
        }
    }

    private void actualizarTablaCategorias() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Descripción", "Es Perecible"}, 0);

        try (Connection conn = Conexion.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_categoria, nombre_categoria, descripcion, es_perecible FROM categorias")) {

            while (rs.next()) {
                Object[] fila = new Object[4];
                fila[0] = rs.getInt("id_categoria");
                fila[1] = rs.getString("nombre_categoria");
                fila[2] = rs.getString("descripcion");
                fila[3] = rs.getBoolean("es_perecible");
                modelo.addRow(fila);
            }

            tabla_de_categoriastable1.setModel(modelo);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar categorías: " + ex.getMessage());
        }
    }

    private void modificarCategorias() {
        if (tabla_de_categoriastable1.isEditing()) {
            tabla_de_categoriastable1.getCellEditor().stopCellEditing();
        }

        DefaultTableModel modelo = (DefaultTableModel) tabla_de_categoriastable1.getModel();
        int filas = modelo.getRowCount();
        int modificados = 0;

        try (Connection conn = Conexion.conectar()) {
            for (int i = 0; i < filas; i++) {
                int id = (int) modelo.getValueAt(i, 0);
                String nombre = modelo.getValueAt(i, 1).toString().trim();
                String descripcion = modelo.getValueAt(i, 2) != null ? modelo.getValueAt(i, 2).toString().trim() : null;
                boolean esPerecible = Boolean.parseBoolean(modelo.getValueAt(i, 3).toString());

                String sql = "UPDATE categorias SET nombre_categoria = ?, descripcion = ?, es_perecible = ? WHERE id_categoria = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, descripcion);
                    stmt.setBoolean(3, esPerecible);
                    stmt.setInt(4, id);
                    stmt.executeUpdate();
                    modificados++;
                }
            }

            JOptionPane.showMessageDialog(panel1, modificados + " categoría(s) modificada(s).");
            actualizarTablaCategorias();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al modificar categorías: " + ex.getMessage());
        }
    }

    private void eliminarCategoria() {
        int filaSeleccionada = tabla_de_categoriastable1.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(panel1, "Selecciona una categoría para eliminar.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(panel1, "¿Estás seguro de que deseas eliminar esta categoría?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        int idCategoria = (int) tabla_de_categoriastable1.getValueAt(filaSeleccionada, 0);

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM categorias WHERE id_categoria = ?")) {

            stmt.setInt(1, idCategoria);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(panel1, "Categoría eliminada correctamente.");
            actualizarTablaCategorias();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al eliminar categoría: " + ex.getMessage());
        }
    }

    private void ordenarTabla() {
        String[] columnas = {"ID", "Nombre", "Descripción", "Es Perecible"};
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

        int columnaIndex = Arrays.asList(columnas).indexOf(columnaSeleccionada);
        SortOrder orden = ordenSeleccionada.equals("Ascendente") ? SortOrder.ASCENDING : SortOrder.DESCENDING;

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tabla_de_categoriastable1.getModel());

        // Asegurar orden numérico para ID
        if (columnaSeleccionada.equals("ID")) {
            sorter.setComparator(0, Comparator.comparingInt(o -> Integer.parseInt(o.toString())));
        }

        tabla_de_categoriastable1.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(columnaIndex, orden)));
        sorter.sort();
    }
}
