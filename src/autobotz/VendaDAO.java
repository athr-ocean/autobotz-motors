package autobotz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {
    private static final String ARQUIVO = "vendas.txt";

    public void registrarVenda(Venda venda) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            writer.write(venda.getIdCliente() + ";" + venda.getIdVeiculo() + ";" + venda.getValorFinal());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erro ao salvar no txt: " + e.getMessage());
        }
    }

    public List<String> listarVendas() {
        return new ArrayList<>();
    }
}