package br.edu.estoquefacil;
import java.io.IOException;
import br.edu.estoquefacil.Modelo.Dados;

/** Contrato que permite testar o serviço sem depender da interface gráfica. */
public interface Repositorio extends AutoCloseable {
    Dados carregar() throws IOException;
    void salvar(Dados dados) throws IOException;
    @Override default void close() throws IOException {}
}
