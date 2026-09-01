package autobotz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VeiculoDAO {
    private static final String ARQUIVO = "veiculos.txt";

    public void cadastrar(Veiculo veiculo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            writer.write(veiculo.getMarca() + ";" + veiculo.getModelo() + ";" + veiculo.getAno() + ";" + veiculo.getPreco());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erro ao salvar no txt: " + e.getMessage());
        }
    }

    public List<Veiculo> listar() {
        List<Veiculo> lista = new ArrayList<>();
        // TODO: Implementar leitura completa do arquivo veiculos.txt na próxima rodada
        return lista;
    }
    
    public void atualizar(Veiculo veiculo) {}
    public void deletar(int id) {}
}