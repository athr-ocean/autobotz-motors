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
        List<String> lista = new ArrayList<>();
        File file = new File(ARQUIVO);
        if (!file.exists()) return lista;

        try (BufferedReader reader = new BufferedReader(new FileReader(ARQUIVO))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(";");
                if (dados.length >= 3) {
                    lista.add("ID Cliente: " + dados[0] + " | ID Veículo: " + dados[1] + " | Valor Final: R$" + dados[2]);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo txt: " + e.getMessage());
        }
        return lista;
    }
}