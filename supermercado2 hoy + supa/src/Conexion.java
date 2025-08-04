import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    // Tu pooler (modo Transaction sobre el puerto 6543 de Supavisor)
    private static final String URL =
            "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres";
    private static final String USER = "postgres.eoybmysrdeajgwoabuvk";
    private static final String PASSWORD = "1234";  // o la contraseña real

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("❌ Error al conectar con Supabase: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("🔄 Probando conexión a Supabase...");
        try (Connection conn = conectar()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Conexión exitosa a Supabase");
            } else {
                System.out.println("❌ No se pudo conectar o se cerró inmediatamente");
            }
        } catch (SQLException e) {
            System.out.println("⚠️ Error inesperado al cerrar conexión: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
}
