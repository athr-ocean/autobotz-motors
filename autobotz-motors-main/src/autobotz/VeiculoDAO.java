package autobotz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VeiculoDAO {
    private static final String ARQUIVO = "veiculos.txt";

    // Gera um ID sequencial baseado no maior ID existente
    private int gerarProximoId() {
        List<Veiculo> veiculos = listar();
        int maxId = 0;
        for (Veiculo v : veiculos) {
            if (v.getId() > maxId) maxId = v.getId();
        }
        return maxId + 1;
    }

    public void cadastrar(Veiculo veiculo) {
        veiculo.setId(gerarProximoId());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            writer.write(veiculo.getId() + ";" + veiculo.getMarca() + ";" + 
                         veiculo.getModelo() + ";" + veiculo.getAno() + ";" + 
                         veiculo.getPreco() + ";" + veiculo.getStatus());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erro ao salvar no txt: " + e.getMessage());
        }
    }

    public List<Veiculo> listar() {
        List<Veiculo> lista = new ArrayList<>();
        File file = new File(ARQUIVO);
        if (!file.exists()) return lista;

        try (BufferedReader reader = new BufferedReader(new FileReader(ARQUIVO))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(";");
                if (dados.length >= 6) {
                    lista.add(new Veiculo(
                        Integer.parseInt(dados[0]),
                        dados[1],
                        dados[2],
                        Integer.parseInt(dados[3]),
                        Double.parseDouble(dados[4]),
                        dados[5]
                    ));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao ler o arquivo txt: " + e.getMessage());
        }
        return lista;
    }
    
    public void atualizar(Veiculo veiculo) {
        List<Veiculo> veiculos = listar();
        boolean atualizou = false;
        for (int i = 0; i < veiculos.size(); i++) {
            if (veiculos.get(i).getId() == veiculo.getId()) {
                veiculo.setStatus(veiculos.get(i).getStatus());
                veiculos.set(i, veiculo);
                atualizou = true;
                break;
            }
        }
        if (atualizou) reescreverArquivo(veiculos);
    }

    public void deletar(int id) {
        List<Veiculo> veiculos = listar();
        boolean removeu = veiculos.removeIf(v -> v.getId() == id);
        if (removeu) reescreverArquivo(veiculos);
    }

    private void reescreverArquivo(List<Veiculo> veiculos) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, false))) {
            for (Veiculo v : veiculos) {
                writer.write(v.getId() + ";" + v.getMarca() + ";" + 
                             v.getModelo() + ";" + v.getAno() + ";" + 
                             v.getPreco() + ";" + v.getStatus());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao reescrever txt: " + e.getMessage());
        }
    }
}