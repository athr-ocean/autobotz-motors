package autobotz;

import java.util.List;

public class VendaService {

private VendaDAO vendaDAO;
private VeiculoDAO veiculoDAO;
private ClienteDAO clienteDAO;

public VendaService() {
    this.vendaDAO = new VendaDAO();
    this.veiculoDAO = new VeiculoDAO();
    this.clienteDAO = new ClienteDAO();
}

public void realizarVenda(int idCliente, int idVeiculo, double valorFinal) {

    Veiculo veiculoEncontrado = null;
    String statusAnterior = null;

    try {

        // 1. Valida os dados recebidos
        if (idCliente <= 0) {
            throw new IllegalArgumentException("ID do cliente inválido.");
        }

        if (idVeiculo <= 0) {
            throw new IllegalArgumentException("ID do veículo inválido.");
        }

        if (valorFinal <= 0) {
            throw new IllegalArgumentException("Valor da venda inválido.");
        }

        // 2. Procura o veículo
        List<Veiculo> veiculos = veiculoDAO.listar();

        for (Veiculo veiculo : veiculos) {
            if (veiculo.getId() == idVeiculo) {
                veiculoEncontrado = veiculo;
                break;
            }
        }

        // 4. Verifica se o veículo está disponível
        if (veiculoEncontrado == null) {
            throw new IllegalArgumentException("Veículo não encontrado.");
        }
        
        if (!veiculoEncontrado.getStatus().equals("Disponível")) {
            throw new IllegalArgumentException("Veículo não está disponível.");
        }

        // 5. Procura o cliente
        List<Cliente> clientes = clienteDAO.listar();

        Cliente clienteEncontrado = null;

        for (Cliente cliente : clientes) {
            if (cliente.getId() == idCliente) {
                clienteEncontrado = cliente;
                break;
            }
        }

        // 6. Verifica se o cliente existe
        if (clienteEncontrado == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }

        // Guarda o estado original para possibilitar rollback
        statusAnterior = veiculoEncontrado.getStatus();

        // 7. Muda o veículo para vendido
        veiculoEncontrado.setStatus("Vendido");

        // 8. Salva a alteração no arquivo
        veiculoDAO.atualizar(veiculoEncontrado);

        // 9. Cria a venda
        Venda venda = new Venda(idCliente, idVeiculo, valorFinal);

        // 10. Registra a venda
        vendaDAO.registrarVenda(venda);

        System.out.println("Venda realizada com sucesso!");

    } catch (IllegalArgumentException e) {

        System.out.println("Erro na venda: " + e.getMessage());

        // Rollback manual
        if (veiculoEncontrado != null && statusAnterior != null) {
            veiculoEncontrado.setStatus(statusAnterior);
            veiculoDAO.atualizar(veiculoEncontrado);

            System.out.println("Alteração do veículo desfeita.");
        }

    } catch (Exception e) {

        System.out.println("Erro ao realizar venda: " + e.getMessage());

        // Rollback manual
        if (veiculoEncontrado != null && statusAnterior != null) {
            veiculoEncontrado.setStatus(statusAnterior);
            veiculoDAO.atualizar(veiculoEncontrado);

            System.out.println("Alteração do veículo desfeita.");
        }
    }
}

}
