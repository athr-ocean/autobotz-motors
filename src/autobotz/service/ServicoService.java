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

public class ServicoService {

    // Regra original do modulo de Oficina:
    // garantia de 1 ano (365 dias) a partir da data da venda.
    private static final long DIAS_GARANTIA = 365;

    private final OrdemServicoDAO ordemServicoDAO;
    private final ServicoDAO servicoDAO;
    private final VendaDAO vendaDAO;

    /*
     * Os mocks usados durante o desenvolvimento foram substituidos
     * pelos DAOs reais do projeto.
     *
     * A regra de negocio do modulo permanece a mesma.
     */
    public ServicoService(
            OrdemServicoDAO ordemServicoDAO,
            ServicoDAO servicoDAO,
            VendaDAO vendaDAO) {

        this.ordemServicoDAO = ordemServicoDAO;
        this.servicoDAO = servicoDAO;
        this.vendaDAO = vendaDAO;
    }

    /**
     * Calcula o total de uma Ordem de Servico persistida no banco.
     */
    public ResultadoOS calcularTotalOS(int idOrdemServico)
            throws SQLException {

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(idOrdemServico);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    "Ordem de servico nao encontrada: "
                    + idOrdemServico
            );
        }

        List<ItemOS> itens =
                ordemServicoDAO.buscarItensObjetos(
                        ordem.getId()
                );

        Venda venda =
                buscarVendaDoVeiculo(
                        ordem.getVeiculoId()
                );

        LocalDate dataVenda =
                venda != null
                        ? venda.getDataVenda()
                        : null;

        return calcular(itens, dataVenda);
    }

    /**
     * Mantem o modo de teste da regra de garantia criado
     * originalmente para o modulo da Oficina.
     */
    public ResultadoOS calcularTotalOS(
            List<ItemOS> itens,
            LocalDate dataVenda)
            throws SQLException {

        return calcular(itens, dataVenda);
    }

    private Venda buscarVendaDoVeiculo(int idVeiculo)
            throws SQLException {

        Venda vendaEncontrada = null;

        for (Venda venda : vendaDAO.listarVendas()) {

            if (venda.getIdVeiculo() != idVeiculo) {
                continue;
            }

            if (vendaEncontrada == null
                    || venda.getDataVenda().isAfter(
                            vendaEncontrada.getDataVenda())) {

                vendaEncontrada = venda;
            }
        }

        return vendaEncontrada;
    }

    private ResultadoOS calcular(
            List<ItemOS> itens,
            LocalDate dataVenda)
            throws SQLException {

        boolean garantiaAtiva =
                estaDentroDaGarantia(dataVenda);

        double totalMaoDeObra = 0.0;
        double totalDesconto = 0.0;

        List<String> detalhes =
                new ArrayList<>();

        for (ItemOS item : itens) {

            Servico servico =
                    servicoDAO.buscarPorId(
                            item.getServicoId()
                    );

            String nomeServico =
                    servico != null
                            ? servico.getNome()
                            : "Servico #"
                              + item.getServicoId();

            double valorItem =
                    item.getPreco()
                    * item.getQuantidade();

            boolean revisao =
                    servico != null
                    && ehRevisao(
                            servico.getNome()
                    );

            if (revisao && garantiaAtiva) {

                totalDesconto += valorItem;

                detalhes.add(
                        String.format(
                                "%s: R$ %.2f -> "
                                + "R$ 0,00 (garantia)",
                                nomeServico,
                                valorItem
                        )
                );

            } else {

                totalMaoDeObra += valorItem;

                detalhes.add(
                        String.format(
                                "%s: R$ %.2f",
                                nomeServico,
                                valorItem
                        )
                );
            }
        }

        ResultadoOS resultado =
                new ResultadoOS();

        resultado.setTotalMaoDeObra(
                arredondar(totalMaoDeObra)
        );

        resultado.setTotalDesconto(
                arredondar(totalDesconto)
        );

        resultado.setGarantiaAtiva(
                garantiaAtiva
        );

        resultado.setDetalhes(
                detalhes
        );

        return resultado;
    }

    private boolean estaDentroDaGarantia(
            LocalDate dataVenda) {

        if (dataVenda == null) {
            return false;
        }

        long dias =
                ChronoUnit.DAYS.between(
                        dataVenda,
                        LocalDate.now()
                );

        return dias < DIAS_GARANTIA;
    }

    private boolean ehRevisao(
            String nomeServico) {

        if (nomeServico == null) {
            return false;
        }

        // cobre revisao / revisão
        return nomeServico
                .toLowerCase()
                .contains("revis");
    }

    private double arredondar(
            double valor) {

        return Math.round(
                valor * 100.0
        ) / 100.0;
    }
}
