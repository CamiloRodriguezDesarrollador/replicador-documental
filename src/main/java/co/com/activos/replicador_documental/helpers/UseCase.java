package co.com.activos.replicador_documental.helpers;

public interface UseCase <T, R> {
    R ejecutar(T command);
}
