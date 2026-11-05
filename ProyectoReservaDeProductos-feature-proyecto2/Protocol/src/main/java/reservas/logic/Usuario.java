package reservas.logic;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(Funcionario.class)
public class Usuario {
    @XmlID
    private String id;
    private String clave;
    private String rol;

    public static final String ADMIN = "ADMIN";
    public static final String FUNCIONARIO = "FUNCIONARIO";

    public Usuario(String id, String clave, String rol) {
        this.id = id;
        this.clave = clave;
        this.rol = rol;
    }

    public Usuario() {
        this("", "", FUNCIONARIO);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}