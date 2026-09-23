package autobotz.service;

import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.model.Venda;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negocio da Oficina/OS.
 *
 * IMPORTANTE: esta classe agora usa os DAOs REAIS (nao mais os Mock*).
 * Para compilar sem erro, o Alexandre precisa adicionar 3 metodos que
 * ainda nao existem nos DAOs dele:
 *
 *   1) OrdemServicoDAO.buscarPorId(int id)          -> OrdemServico
 *   2) OrdemServicoDAO.buscarItensObjetos(int idOrdem) -> List<ItemOS>
 *   3) VendaDAO.buscarPorVeiculoId(int veiculoId)   -> Venda (ou null)
 *
 * (Mande a mensagem que o Claude preparou pro Alexandre pedindo esses 3.)
 */
public class ServicoService {

    // Garantia de 1 ano (365 dias) a partir da data de venda do veiculo
    private static final long DIAS_GARANTIA = 365;

    private final OrdemServicoDAO ordemServicoDAO;
    private final ServicoDAO servicoDAO;
    private final VendaDAO vendaDAO;

    public ServicoService(OrdemServicoDAO ordemServicoDAO,
                           ServicoDAO servicoDAO,
                           VendaDAO vendaDAO) {
        this.ordemServicoDAO = ordemServicoDAO;
        this.servicoDAO = servicoDAO;
        this.vendaDAO = vendaDAO;
    }

    /**
     * Calcula o total de uma Ordem de Servico real, buscando os dados pelas DAOs.
     */
    public ResultadoOS calcularTotalOS(int idOrdemServico) throws SQLException {
        OrdemServico ordem = ordemServicoDAO.buscarPorId(idOrdemServico);
        if (ordem == null) {
            throw new IllegalArgumentException("Ordem de servico nao encontrada: " + idOrdemServico);
        }

        List<ItemOS> itens = ordemServicoDAO.buscarItensObjetos(ordem.getId());
        Venda venda = vendaDAO.buscarPorVeiculoId(ordem.getVeiculoId());
        LocalDate dataVenda = (venda != null) ? venda.getDataVenda() : null;

        return calcular(itens, dataVenda);
    }

    /**
     * Overload usado para testar a matematica do desconto isoladamente,
     * passando a data de venda direto (sem precisar montar Ordem/DAO).
     */
    public ResultadoOS calcularTotalOS(List<ItemOS> itens, LocalDate dataVenda) throws SQLException {
        return calcular(itens, dataVenda);
    }

    private ResultadoOS calcular(List<ItemOS> itens, LocalDate dataVenda) throws SQLException {
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
