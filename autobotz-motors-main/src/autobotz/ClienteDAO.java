package autobotz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    private static final String ARQUIVO = "clientes.txt";

    public void salvar(Cliente cliente) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            // Salva todos os atributos separados por ponto e vírgula
            writer.write(cliente.getId() + ";" + 
                         cliente.getNome() + ";" + 
                         cliente.getCpf() + ";" + 
                         cliente.getTelefone() + ";" + 
                         cliente.getEmail());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erro ao salvar no arquivo .txt: " + e.getMessage());
        }
    }

    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();
        File file = new File(ARQUIVO);
        if (!file.exists()) return lista;

        try (BufferedReader reader = new BufferedReader(new FileReader(ARQUIVO))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(";");
                // Verifica se a linha possui os 5 campos esperados
                if (dados.length >= 5) {
                    lista.add(new Cliente(
                        Integer.parseInt(dados[0]), // id
                        dados[1],                   // nome
                        dados[2],                   // cpf
                        dados[3],                   // telefone
                        dados[4]                    // email
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo .txt: " + e.getMessage());
        }
        return lista;
    }
}