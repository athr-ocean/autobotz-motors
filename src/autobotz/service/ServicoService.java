package autobotz.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ServicoService {
    private static final long PRAZO_GARANTIA_DIAS = 365;

    public boolean estaNaGarantia(LocalDate dataVenda, LocalDate dataConsulta) {
        if (dataVenda == null || dataConsulta == null || dataConsulta.isBefore(dataVenda)) {
            return false;
        }
        return ChronoUnit.DAYS.between(dataVenda, dataConsulta) < PRAZO_GARANTIA_DIAS;
    }

    public double calcularMaoDeObra(LocalDate dataVenda, LocalDate dataConsulta,
            double valorMaoDeObra) {
        if (valorMaoDeObra < 0) {
            throw new IllegalArgumentException("O valor da mao de obra nao pode ser negativo.");
        }
        return estaNaGarantia(dataVenda, dataConsulta) ? 0.0 : valorMaoDeObra;
    }
}
