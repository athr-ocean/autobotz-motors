package autobotz.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AssinaturaCliente {

    private int id;
    private int clienteId;
    private PlanoAssinatura plano;
    private List<ModuloOpcional> modulos;
    private String status;
    private LocalDate dataInicio;
    private LocalDate dataCancelamento;
    private String motivoCancelamento;
    private String categoriaCancelamento;

    public AssinaturaCliente(int id, int clienteId,
                            PlanoAssinatura plano) {

        this.id = id;
        this.clienteId = clienteId;
        this.plano = plano;
        this.modulos = new ArrayList<>();
        this.status = "ATIVA";
        this.dataInicio = LocalDate.now();
    }
    public AssinaturaCliente(int id, int clienteId,
            PlanoAssinatura plano,
            LocalDate dataInicio,
                String status,
                LocalDate dataCancelamento,
                String categoriaCancelamento,
                String motivoCancelamento) {
                    this.dataCancelamento = dataCancelamento;
                    this.categoriaCancelamento = categoriaCancelamento;
                    this.motivoCancelamento = motivoCancelamento;

                    this.modulos = new ArrayList<>();
                this.id = id;
                this.clienteId = clienteId;
                this.plano = plano;
			this.modulos = new ArrayList<>();
			this.status = status;
			this.dataInicio = dataInicio;

			}
    public void adicionarModulo(ModuloOpcional modulo) {
        modulos.add(modulo);
    }

    public BigDecimal calcularValorMensal() {

        BigDecimal total = plano.getPrecoBase();

        for (ModuloOpcional modulo : modulos) {
            total = total.add(modulo.getPreco());
        }

        return total;
    }

    public void alterarPlano(PlanoAssinatura novoPlano) {
        this.plano = novoPlano;
    }

    public void cancelar(String motivo) {
        cancelar("OUTROS", motivo, LocalDate.now());
    }

    public void cancelar(String categoria, String motivo,
                         LocalDate data) {

        if ("CANCELADA".equals(status)) {
            throw new IllegalStateException(
                "Assinatura já cancelada"
            );
        }

        if (data == null || data.isBefore(dataInicio)) {
            throw new IllegalArgumentException(
                "Data de cancelamento inválida"
            );
        }

        this.status = "CANCELADA";
        this.dataCancelamento = data;
        this.categoriaCancelamento = categoria;
        this.motivoCancelamento = motivo;
    }

    public int getId() {
        return id;
    }
    public String getCategoriaCancelamento() {
        return categoriaCancelamento;
    }
    public int getClienteId() {
        return clienteId;
    }

    public PlanoAssinatura getPlano() {
        return plano;
    }

    public List<ModuloOpcional> getModulos() {
        return new ArrayList<>(modulos);
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataCancelamento() {
        return dataCancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }
}
