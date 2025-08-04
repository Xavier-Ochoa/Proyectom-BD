public class User {
    private String usuario;
    private String rol;
    private int idEmpleado;

    public User(String usuario, String rol, int idEmpleado) {
        this.usuario = usuario;
        this.rol = rol;
        this.idEmpleado = idEmpleado;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }
}
