import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class empleado_promociones {
    public JPanel panel1;
    private JTextField ingreso_del_nombrepromociontextField1;
    private JTextField ingreso_de_descriptextField2;
    private JTextField ingreso_de_descuentoporcentajetextField3;
    private JTextField ingreso_de_fechainicotextField4;
    private JTextField ingreso_de_fechafintextField5;
    private JButton registarNuevaPromoButton;
    private JTable tabla_de_promostable1;
    private JButton eliminar_promoButton;
    private JButton actualizar_tablapromoButton;
    private JButton modificarButton;
    private JButton volverButton;
    private JButton ordenarTablaButton;
    private JTextField ingreso_idpromociontextField1;
    private JButton buscarid_productButton;
    private JButton buscarCategoriaButton;
    private JTextField ingreso_categirTextField;
    private JLabel categorial_jlabel;
    private User usuarioLogeado;

    public empleado_promociones(User usuarioLogeado) {
        this.usuarioLogeado = usuarioLogeado;

        actualizarTablaPromociones();

        buscarid_productButton.addActionListener(e -> mostrarListaProductosConConfirmacion());
        buscarCategoriaButton.addActionListener(e -> mostrarListaCategoriasConConfirmacion());
        registarNuevaPromoButton.addActionListener(e -> registrarPromocion());
        actualizar_tablapromoButton.addActionListener(e -> actualizarTablaPromociones());
        modificarButton.addActionListener(e -> modificarPromociones());
        ordenarTablaButton.addActionListener(e -> ordenarTabla());
        eliminar_promoButton.addActionListener(e -> eliminarPromocion());
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

    private void mostrarListaProductosConConfirmacion() {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        Map<String, Integer> mapa = new LinkedHashMap<>();

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_producto, nombre FROM productos ORDER BY nombre");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                String item = id + " - " + nombre;
                modelo.addElement(item);
                mapa.put(item, id);
            }

            JList<String> lista = new JList<>(modelo);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(lista);

            int opcion = JOptionPane.showConfirmDialog(panel1, scroll, "Selecciona un producto", JOptionPane.OK_CANCEL_OPTION);
            if (opcion == JOptionPane.OK_OPTION && lista.getSelectedValue() != null) {
                String sel = lista.getSelectedValue();
                int idSel = mapa.get(sel);

                int confirm = JOptionPane.showConfirmDialog(panel1,
                        "¿Asignar el producto \"" + sel + "\" a la promoción?",
                        "Confirmar producto", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    ingreso_idpromociontextField1.setText(String.valueOf(idSel));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar productos: " + e.getMessage());
        }
    }

    private void mostrarListaCategoriasConConfirmacion() {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        Map<String, Integer> mapa = new LinkedHashMap<>();

        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_categoria, nombre_categoria FROM categorias ORDER BY nombre_categoria");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_categoria");
                String nombre = rs.getString("nombre_categoria");
                String item = id + " - " + nombre;
                modelo.addElement(item);
                mapa.put(item, id);
            }

            JList<String> lista = new JList<>(modelo);
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(lista);

            int opcion = JOptionPane.showConfirmDialog(panel1, scroll, "Selecciona una categoría", JOptionPane.OK_CANCEL_OPTION);
            if (opcion == JOptionPane.OK_OPTION && lista.getSelectedValue() != null) {
                String sel = lista.getSelectedValue();
                int idSel = mapa.get(sel);

                int confirm = JOptionPane.showConfirmDialog(panel1,
                        "¿Asignar la categoría \"" + sel + "\" a la promoción?",
                        "Confirmar categoría", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    ingreso_categirTextField.setText(String.valueOf(idSel));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void registrarPromocion() {
        String nombre = ingreso_del_nombrepromociontextField1.getText().trim();
        String descripcion = ingreso_de_descriptextField2.getText().trim();
        String descuentoStr = ingreso_de_descuentoporcentajetextField3.getText().trim();
        String fechaInicioStr = ingreso_de_fechainicotextField4.getText().trim();
        String fechaFinStr = ingreso_de_fechafintextField5.getText().trim();
        String idProductoStr = ingreso_idpromociontextField1.getText().trim();
        String idCategoriaStr = ingreso_categirTextField.getText().trim();

        if (nombre.isEmpty() || fechaInicioStr.isEmpty() || fechaFinStr.isEmpty()) {
            JOptionPane.showMessageDialog(panel1, "Los campos obligatorios (nombre, fechas) deben completarse.");
            return;
        }

        BigDecimal descuento = null;
        if (!descuentoStr.isEmpty()) {
            try {
                descuento = new BigDecimal(descuentoStr);
                if (descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(new BigDecimal("100")) > 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel1, "El descuento debe ser un número entre 0 y 100.");
                return;
            }
        }

        java.util.Date ini, fin;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
            sdf.setLenient(false);
            ini = sdf.parse(fechaInicioStr);
            fin = sdf.parse(fechaFinStr);
            if (fin.before(ini)) {
                JOptionPane.showMessageDialog(panel1, "La fecha fin debe ser posterior o igual a la fecha inicio.");
                return;
            }
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(panel1, "Formato de fecha inválido. Usa DD/MM/AA.");
            return;
        }

        Integer idProducto = null;
        if (!idProductoStr.isEmpty()) {
            try { idProducto = Integer.parseInt(idProductoStr); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel1, "El ID de producto debe ser numérico.");
                return;
            }
        }

        Integer idCategoria = null;
        if (!idCategoriaStr.isEmpty()) {
            try { idCategoria = Integer.parseInt(idCategoriaStr); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel1, "El ID de categoría debe ser numérico.");
                return;
            }
        }

        try (Connection conn = Conexion.conectar();
             CallableStatement call = conn.prepareCall("SELECT insertar_promocion_con_producto_opcional(?, ?, ?, ?, ?, ?, ?)")) {

            call.setString(1, nombre);
            call.setDate(2, new java.sql.Date(ini.getTime()));
            call.setDate(3, new java.sql.Date(fin.getTime()));
            call.setString(4, descripcion.isEmpty() ? null : descripcion);
            if (descuento != null) call.setBigDecimal(5, descuento); else call.setNull(5, Types.NUMERIC);
            if (idProducto != null) call.setInt(6, idProducto); else call.setNull(6, Types.INTEGER);
            if (idCategoria != null) call.setInt(7, idCategoria); else call.setNull(7, Types.INTEGER);

            call.execute();
            JOptionPane.showMessageDialog(panel1, "Promoción registrada exitosamente.");
            actualizarTablaPromociones();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al registrar promoción: " + ex.getMessage());
        }
    }





    private void actualizarTablaPromociones() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Descripción", "Descuento", "Inicio", "Fin", "ID Producto"}, 0);
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.id_promocion, p.nombre_promocion, p.descripcion, p.descuento_porcentaje," +
                             " p.fecha_inicio, p.fecha_fin, pp.id_producto " +
                             "FROM promociones p LEFT JOIN producto_promocion pp ON p.id_promocion = pp.id_promocion");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                modelo.addRow(new Object[]{
                        rs.getInt("id_promocion"),
                        rs.getString("nombre_promocion"),
                        rs.getString("descripcion"),
                        rs.getBigDecimal("descuento_porcentaje"),
                        rs.getDate("fecha_inicio"),
                        rs.getDate("fecha_fin"),
                        rs.getObject("id_producto")
                });
            }
            tabla_de_promostable1.setModel(modelo);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al cargar promociones: " + ex.getMessage());
        }
    }

    private void modificarPromociones() {
        if (tabla_de_promostable1.isEditing()) tabla_de_promostable1.getCellEditor().stopCellEditing();

        DefaultTableModel modelo = (DefaultTableModel) tabla_de_promostable1.getModel();
        try (Connection conn = Conexion.conectar()) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                int id = (int) modelo.getValueAt(i, 0);
                String nom = modelo.getValueAt(i, 1).toString().trim();
                String desc = modelo.getValueAt(i, 2) != null ? modelo.getValueAt(i, 2).toString().trim(): null;
                BigDecimal descPor = modelo.getValueAt(i, 3) != null ? new BigDecimal(modelo.getValueAt(i, 3).toString()): null;
                java.sql.Date fi = java.sql.Date.valueOf(modelo.getValueAt(i, 4).toString());
                java.sql.Date ff = java.sql.Date.valueOf(modelo.getValueAt(i, 5).toString());
                Object idProdObj = modelo.getValueAt(i, 6);

                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE promociones SET nombre_promocion=?, descripcion=?, descuento_porcentaje=?, fecha_inicio=?, fecha_fin=? WHERE id_promocion=?");
                ps.setString(1, nom);
                ps.setString(2, desc);
                if (descPor != null) ps.setBigDecimal(3, descPor); else ps.setNull(3, Types.NUMERIC);
                ps.setDate(4, fi); ps.setDate(5, ff);
                ps.setInt(6, id);
                ps.executeUpdate();

                PreparedStatement del = conn.prepareStatement("DELETE FROM producto_promocion WHERE id_promocion=?");
                del.setInt(1, id);
                del.executeUpdate();

                if (idProdObj != null) {
                    int idP = Integer.parseInt(idProdObj.toString());
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO producto_promocion(id_producto,id_promocion) VALUES (?,?)");
                    ins.setInt(1, idP);
                    ins.setInt(2, id);
                    ins.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(panel1, "Promociones modificadas correctamente.");
            actualizarTablaPromociones();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al modificar promociones: " + ex.getMessage());
        }
    }

    private void ordenarTabla() {
        String[] columnas = {"ID", "Nombre", "Descripción", "Descuento", "Inicio", "Fin", "ID Producto"};
        String[] ordenes = {"Ascendente", "Descendente"};
        String colSel = (String) JOptionPane.showInputDialog(panel1, "Selecciona columna:", "Ordenar", JOptionPane.QUESTION_MESSAGE, null, columnas, columnas[0]);
        if (colSel == null) return;
        String ordSel = (String) JOptionPane.showInputDialog(panel1, "Tipo de orden:", "Ordenar", JOptionPane.QUESTION_MESSAGE, null, ordenes, ordenes[0]);
        if (ordSel == null) return;
        int colIndex = Arrays.asList(columnas).indexOf(colSel);
        SortOrder order = ordSel.equals("Ascendente") ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel)tabla_de_promostable1.getModel());
        if (colIndex == 0 || colIndex == 6) {
            sorter.setComparator(colIndex, Comparator.comparingInt(o -> Integer.parseInt(o.toString())));
        }
        tabla_de_promostable1.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(colIndex, order)));
        sorter.sort();
    }

    private void eliminarPromocion() {
        int fila = tabla_de_promostable1.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(panel1, "Selecciona una promoción para eliminar.");
            return;
        }
        int id = (int) tabla_de_promostable1.getValueAt(fila, 0);
        int conf = JOptionPane.showConfirmDialog(panel1, "¿Eliminar promoción ID " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = Conexion.conectar();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM promociones WHERE id_promocion=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(panel1, "Promoción eliminada correctamente.");
            actualizarTablaPromociones();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(panel1, "Error al eliminar promoción: " + ex.getMessage());
        }
    }
}
