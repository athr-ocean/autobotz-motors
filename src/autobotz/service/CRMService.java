package autobotz.service;

import autobotz.dao.ClienteDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.Cliente;
import autobotz.model.Venda;
import autobotz.util.I18nUtils;

import java.sql.SQLException;
import java.util.List;

public class CRMService {
    public record Historico(Cliente cliente, List<Venda> vendas, double total) { }

    public Historico consultarHistorico(int idCliente) throws SQLException {
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) throw new IllegalArgumentException(I18nUtils.getString("crm.cliente_nao_encontrado"));
        List<Venda> vendas = vendaDAO.listarVendasPorCliente(idCliente);
        return new Historico(cliente, List.copyOf(vendas), vendas.stream().mapToDouble(Venda::getValorTotal).sum());
    }

    private final ClienteDAO clienteDAO;
    private final VendaDAO vendaDAO;

    public CRMService() {
        this.clienteDAO = new ClienteDAO();
        this.vendaDAO = new VendaDAO();
    }

    public void exibirHistoricoCliente(int idCliente)
            throws SQLException {

        Historico historico = consultarHistorico(idCliente);
        Cliente cliente = historico.cliente();
        List<Venda> vendas = historico.vendas();

        double totalGasto = 0;

        System.out.println(
                I18nUtils.getString("crm.titulo")
        );

        System.out.println(
                I18nUtils.getString("crm.cliente")
                + cliente.getNome()
        );

        System.out.println(
                I18nUtils.getString("crm.cpf")
                + cliente.getCpf()
        );

        System.out.println(
                I18nUtils.getString("crm.telefone")
                + cliente.getTelefone()
        );

        System.out.println(
                I18nUtils.getString("crm.email")
                + cliente.getEmail()
        );

        System.out.println(
                I18nUtils.getString("crm.compras")
        );

        for (Venda venda : vendas) {

            System.out.println(
                    I18nUtils.getString("crm.veiculo")
                    + venda.getIdVeiculo()
                    + " | "
                    + I18nUtils.getString("crm.valor")
                    + I18nUtils.formatCurrency(
                            venda.getValorTotal()
                    )
                    + " | "
                    + I18nUtils.getString("crm.data")
                    + I18nUtils.formatDate(
                            venda.getDataVenda()
                    )
            );

            totalGasto +=
                    venda.getValorTotal();
        }

        System.out.println(
                I18nUtils.getString("crm.quantidade")
                + vendas.size()
        );

        System.out.println(
                I18nUtils.getString("crm.total")
                + I18nUtils.formatCurrency(
                        totalGasto
                )
        );
    }
}
