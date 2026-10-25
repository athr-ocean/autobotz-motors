package autobotz.service;

import java.util.List;

public class ResultadoOS {
    private double totalMaoDeObra;
    private double totalDesconto;
    private boolean garantiaAtiva;
    private List<String> detalhes;

    public double getTotalMaoDeObra() { return totalMaoDeObra; }
    public void setTotalMaoDeObra(double totalMaoDeObra) { this.totalMaoDeObra = totalMaoDeObra; }

    public double getTotalDesconto() { return totalDesconto; }
    public void setTotalDesconto(double totalDesconto) { this.totalDesconto = totalDesconto; }

    public boolean isGarantiaAtiva() { return garantiaAtiva; }
    public void setGarantiaAtiva(boolean garantiaAtiva) { this.garantiaAtiva = garantiaAtiva; }

    public List<String> getDetalhes() { return detalhes; }
    public void setDetalhes(List<String> detalhes) { this.detalhes = detalhes; }
}