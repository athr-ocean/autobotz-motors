package autobotz.service;

import autobotz.mock.MockItemOSDAO;
import autobotz.mock.MockOrdemServicoDAO;
import autobotz.mock.MockServicoDAO;
import autobotz.mock.MockVendaDAO;
import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.model.Venda;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ServicoService {

    // Garantia de 1 ano (365 dias) a partir da data de venda do veiculo
    private static final long DIAS_GARANTIA = 365;

    private final MockOrdemServicoDAO ordemServicoDAO;
    private final MockItemOSDAO itemOSDAO;
    private final MockServicoDAO servicoDAO;
    private final MockVendaDAO vendaDAO;

    // Quando o Alexandre terminar os DAOs reais, troque os tipos Mock* aqui
    // pelos DAOs reais, mantendo os mesmos nomes de metodo usados nesta classe.
    public ServicoService(MockOrdemServicoDAO ordemServicoDAO,
                           MockItemOSDAO itemOSDAO,
                           MockServicoDAO servicoDAO,
                           MockVendaDAO vendaDAO) {
        this.ordemServicoDAO = ordemServicoDAO;
        this.itemOSDAO = itemOSDAO;
        this.servicoDAO = servicoDAO;
        this.vendaDAO = vendaDAO;
    }

    /**
     * Calcula o total de uma Ordem de Servico real, buscando os dados pelas DAOs.
     */
    public ResultadoOS calcularTotalOS(int idOrdemServico) {
        OrdemServico ordem = ordemServicoDAO.buscarPorId(idOrdemServico);
        if (ordem == null) {
            throw new IllegalArgumentException("Ordem de servico nao encontrada: " + idOrdemServico);
        }

        List<ItemOS> itens = itemOSDAO.buscarPorOrdemServico(ordem.getId());
        Venda venda = vendaDAO.buscarPorVeiculoId(ordem.getVeiculoId());
        LocalDate dataVenda = (venda != null) ? venda.getDataVenda() : null;

        return calcular(itens, dataVenda);
    }

    /**
     * Overload usado para testar a matematica do desconto isoladamente,
     * passando a data de venda direto (sem precisar montar Ordem/DAO).
     */
    public ResultadoOS calcularTotalOS(List<ItemOS> itens, LocalDate dataVenda) {
        return calcular(itens, dataVenda);
    }

    private ResultadoOS calcular(List<ItemOS> itens, LocalDate dataVenda) {
        boolean garantiaAtiva = estaDentroDaGarantia(dataVenda);

        double totalMaoDeObra = 0.0;
        double totalDesconto = 0.0;
        List<String> detalhes = new ArrayList<>();

        for (ItemOS item : itens) {
            Servico servico = servicoDAO.buscarPorId(item.getServicoId());
            String nomeServico = (servico != null) ? servico.getNome() : "Servico #" + item.getServicoId();

            double valorItem = item.getPreco() * item.getQuantidade();
            boolean isRevisao = servico != null && ehRevisao(servico.getNome());

            if (isRevisao && garantiaAtiva) {
                totalDesconto += valorItem;
                detalhes.add(String.format("%s: R$ %.2f -> R$ 0,00 (garantia)", nomeServico, valorItem));
            } else {
                totalMaoDeObra += valorItem;
                detalhes.add(String.format("%s: R$ %.2f", nomeServico, valorItem));
            }
        }

        ResultadoOS resultado = new ResultadoOS();
        resultado.setTotalMaoDeObra(arredondar(totalMaoDeObra));
        resultado.setTotalDesconto(arredondar(totalDesconto));
        resultado.setGarantiaAtiva(garantiaAtiva);
        resultado.setDetalhes(detalhes);
        return resultado;
    }

    private boolean estaDentroDaGarantia(LocalDate dataVenda) {
        if (dataVenda == null) {
            return false;
        }
        long dias = ChronoUnit.DAYS.between(dataVenda, LocalDate.now());
        return dias < DIAS_GARANTIA;
    }

    private boolean ehRevisao(String nomeServico) {
        if (nomeServico == null) return false;
        return nomeServico.toLowerCase().contains("revis"); // cobre "revisao"/"revisão"
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}