package ql;

import ontology.Types.ACTIONS;
import ql.Controlador.ESTADOS;

public class EstadoAccion{
    public ESTADOS key1;
    public ACTIONS key2;
 
    public EstadoAccion(ESTADOS key1, ACTIONS key2) {
        this.key1 = key1;
        this.key2 = key2;
    }
 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
 
        EstadoAccion key = (EstadoAccion) o;
 
        if (key1 != null ? !key1.equals(key.key1) : key.key1 != null) return false;
        if (key2 != null ? !key2.equals(key.key2) : key.key2 != null) return false;
 
        return true;
    }
 
    @Override
    public int hashCode() {
        int result = key1 != null ? key1.hashCode() : 0;
        result = 31 * result + (key2 != null ? key2.hashCode() : 0);
        return result;
    }
 
    @Override
    public String toString() {
        return "[" + key1 + ", " + key2 + "]";
    }
}
