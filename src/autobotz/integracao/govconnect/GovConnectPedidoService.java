package autobotz.integracao.govconnect;

import autobotz.integracao.govconnect.GovConnectIntegracao.Cliente;
import autobotz.integracao.govconnect.GovConnectIntegracao.PedidoRequest;
import autobotz.integracao.govconnect.GovConnectIntegracao.PedidoResponse;
import autobotz.model.Fornecedor;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.model.StatusSolicitacao;
import autobotz.service.ComprasException;
import autobotz.service.ComprasService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Ponte de aplicação entre D11 e o adaptador GovConnect.
 *
 * As consultas de ComprasService concluem suas transações antes da chamada HTTP.
 * Assim, nenhuma transação JDBC fica aberta durante uma operação de rede.
 */
public final class GovConnectPedidoService {
    private final ComprasService compras;
    private final Cliente cliente;

    public GovConnectPedidoService(ComprasService compras, Cliente cliente) {
        this.compras = Objects.requireNonNull(compras, "compras");
        this.cliente = Objects.requireNonNull(cliente, "cliente");
    }

    /**
     * Envia um pedido já persistido e gerado pelo fluxo de aprovação D11.
     * Exige que o usuário/serviço tenha permissão CONSULTAR no ComprasService.
     */
    public PedidoResponse enviarPedidoDaSolicitacao(int solicitacaoId)
            throws ComprasException, SQLException, IOException, InterruptedException {
        if (solicitacaoId <= 0) throw new IllegalArgumentException("solicitacaoId deve ser positivo");

        SolicitacaoCompra solicitacao = compras.consultarSolicitacao(solicitacaoId);
        if (solicitacao.getStatus() != StatusSolicitacao.PEDIDO_GERADO)
            throw new IllegalStateException("A solicitação ainda não possui pedido aprovado para envio");

        PedidoCompra pedido = compras.consultarPedidoDaSolicitacao(solicitacaoId);
        if (pedido == null) throw new IllegalStateException("Pedido persistido não encontrado para a solicitação");

        Fornecedor fornecedor = compras.buscarFornecedor(pedido.getFornecedorId());
        PedidoRequest request = PedidoRequest.de(pedido, solicitacao, fornecedor);
        return cliente.enviarPedido(request);
    }
}
