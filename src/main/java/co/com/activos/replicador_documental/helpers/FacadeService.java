package co.com.activos.replicador_documental.helpers;

public interface FacadeService <T, R> {
    R consumir(T command);
}
