package autobotz.service;

import autobotz.dao.ClienteDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.Cliente;
import autobotz.model.Venda;

import java.sql.SQLException;
import java.util.List;

public class CRMService {

    private final ClienteDAO clienteDAO;
    private final VendaDAO vendaDAO;

    public CRMService() {
        this.clienteDAO = new ClienteDAO();
        this.vendaDAO = new VendaDAO();
    }

    public void exibirHistoricoCliente(int idCliente) throws SQLException {

        Cliente cliente = clienteDAO.buscarPorId(idCliente);

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }

        List<Venda> vendas = vendaDAO.listarVendasPorCliente(idCliente);

        double totalGasto = 0;

        System.out.println("\n===== HISTÓRICO DO CLIENTE =====");
        System.out.println("Cliente: " + cliente.getNome());
        System.out.println("CPF: " + cliente.getCpf());
        System.out.println("Telefone: " + cliente.getTelefone());
        System.out.println("Email: " + cliente.getEmail());

        System.out.println("\nCompras:");

        for (Venda venda : vendas) {
            System.out.println(
                "Veículo ID: " + venda.getIdVeiculo()
                + " | Valor: R$ " + venda.getValorTotal()
                + " | Data: " + venda.getDataVenda()
            );

            totalGasto += venda.getValorTotal();
        }

        System.out.println("\nQuantidade de compras: " + vendas.size());
        System.out.println("Total gasto: R$ " + totalGasto);
    }
}