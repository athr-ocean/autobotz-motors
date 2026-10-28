package autobotz.service;

import autobotz.model.AssinaturaCliente;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import autobotz.model.PlanoAssinatura;
import autobotz.model.ResultadoSimulacao;
import java.util.ArrayList;
import java.util.Comparator;

public class AssinaturaService {

    public BigDecimal calcularMRR(
            List<AssinaturaCliente> assinaturas) {

        BigDecimal mrr = BigDecimal.ZERO;

        for (AssinaturaCliente assinatura : assinaturas) {

            if ("ATIVA".equals(assinatura.getStatus())) {
                mrr = mrr.add(
                    assinatura.calcularValorMensal()
                );
            }
        }

        return mrr;
    }
    public double calcularChurn(
            List<AssinaturaCliente> assinaturas,
            LocalDate inicio,
            LocalDate fim) {

        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido");
        }

        int clientesIniciais = 0;
        int cancelamentos = 0;

        for (AssinaturaCliente assinatura : assinaturas) {

            LocalDate dataInicio = assinatura.getDataInicio();
            LocalDate dataCancelamento =
                assinatura.getDataCancelamento();

            boolean ativaNoInicio =
                dataInicio.isBefore(inicio) &&
                (dataCancelamento == null ||
                 !dataCancelamento.isBefore(inicio));

            if (ativaNoInicio) {
                clientesIniciais++;
            }

            if (ativaNoInicio &&
                dataCancelamento != null &&
                !dataCancelamento.isBefore(inicio) &&
                !dataCancelamento.isAfter(fim)) {

                cancelamentos++;
            }
        }

        if (clientesIniciais == 0) {
            return 0.0;
        }

        return (cancelamentos * 100.0) / clientesIniciais;
    }
    public BigDecimal calcularReceitaPerdida(
            List<AssinaturaCliente> assinaturas,
            LocalDate inicio,
            LocalDate fim) {

        BigDecimal total = BigDecimal.ZERO;

        for (AssinaturaCliente assinatura : assinaturas) {

            LocalDate cancelamento =
                assinatura.getDataCancelamento();

            if (cancelamento != null &&
                !cancelamento.isBefore(inicio) &&
                !cancelamento.isAfter(fim)) {

                total = total.add(
                    assinatura.calcularValorMensal()
                );
            }
        }

        return total;
    }
    public ResultadoSimulacao simularMigracao(
            List<AssinaturaCliente> assinaturas,
            PlanoAssinatura planoBarato,
            PlanoAssinatura planoCaro) {

        List<AssinaturaCliente> ativas = new ArrayList<>();

        for (AssinaturaCliente assinatura : assinaturas) {
            if ("ATIVA".equals(assinatura.getStatus())) {
                ativas.add(assinatura);
            }
        }
        ativas.sort(Comparator.comparingInt(
            AssinaturaCliente::getId
        ));

        int total = ativas.size();

        int quantidadeBarato = (int) Math.floor(total * 0.30);
        int quantidadeCaro = (int) Math.floor(total * 0.20);
        int quantidadePermanecem =
            total - quantidadeBarato - quantidadeCaro;

        BigDecimal mrrAntes = calcularMRR(ativas);
        BigDecimal mrrDepois = BigDecimal.ZERO;

        for (int i = 0; i < total; i++) {

            AssinaturaCliente assinatura = ativas.get(i);

            BigDecimal valor = assinatura.calcularValorMensal();
            BigDecimal precoAtual =
                assinatura.getPlano().getPrecoBase();

            if (i < quantidadeBarato) {

                valor = valor.subtract(precoAtual)
                             .add(planoBarato.getPrecoBase());

            } else if (i >= quantidadeBarato +
                                  quantidadePermanecem) {

                valor = valor.subtract(precoAtual)
                             .add(planoCaro.getPrecoBase());
            }

            mrrDepois = mrrDepois.add(valor);
        }
        return new ResultadoSimulacao(
                    quantidadeBarato,
                    quantidadePermanecem,
                    quantidadeCaro,
                    mrrAntes,
                    mrrDepois
                );
    }
}
