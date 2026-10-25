package autobotz.ui;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import autobotz.dao.VeiculoDAO;
import autobotz.model.Veiculo;
import autobotz.util.I18nUtils;

public class VeiculoMenu {
    private final VeiculoDAO veiculoDAO;
    private final Scanner scanner;

    public VeiculoMenu() {
        this.veiculoDAO = new VeiculoDAO();
        this.scanner = new Scanner(System.in);
    }

    public void exibirMenu() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println(I18nUtils.getString("veiculo.menu.titulo"));
            System.out.println(I18nUtils.getString("veiculo.menu.cadastrar"));
            System.out.println(I18nUtils.getString("veiculo.menu.listar"));
            System.out.println(I18nUtils.getString("veiculo.menu.buscar"));
            System.out.println(I18nUtils.getString("menu.opcao0"));
            System.out.print(I18nUtils.getString("menu.escolha"));

            try {
                opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> listar();
                    case 3 -> buscarPorId();
                    case 0 -> System.out.println(I18nUtils.getString("sistema.encerrando"));
                    default -> System.out.println(I18nUtils.getString("sistema.opcao_invalida"));
                }
            } catch (NumberFormatException e) {
                System.out.println(I18nUtils.getString("comum.numero_invalido"));
            } catch (SQLException e) {
                System.out.println(I18nUtils.getString("comum.erro_banco") + e.getMessage());
            }
        }
    }

    private void cadastrar() throws SQLException {
        System.out.print(I18nUtils.getString("veiculo.placa"));
        String placa = scanner.nextLine();
        System.out.print(I18nUtils.getString("veiculo.modelo"));
        String modelo = scanner.nextLine();
        System.out.print(I18nUtils.getString("veiculo.marca"));
        String marca = scanner.nextLine();
        System.out.print(I18nUtils.getString("veiculo.ano"));
        int ano = Integer.parseInt(scanner.nextLine());
        System.out.print(I18nUtils.getString("veiculo.preco"));
        double preco = Double.parseDouble(scanner.nextLine());

        Veiculo veiculo = new Veiculo(placa, modelo, marca, ano, preco);
        veiculoDAO.salvar(veiculo);
        System.out.println(I18nUtils.getString("veiculo.gravado") + veiculo.getId());
    }

    private void listar() throws SQLException {
        List<Veiculo> veiculos = veiculoDAO.listarTodos();
        if (veiculos.isEmpty()) {
            System.out.println(I18nUtils.getString("veiculo.nenhum"));
            return;
        }
        for (Veiculo veiculo : veiculos) {
            System.out.println(formatarVeiculo(veiculo));
        }
    }

    private void buscarPorId() throws SQLException {
        System.out.print(I18nUtils.getString("veiculo.id_buscar"));
        int id = Integer.parseInt(scanner.nextLine());
        Veiculo veiculo = veiculoDAO.buscarPorId(id);
        System.out.println(
                veiculo == null
                        ? I18nUtils.getString(
                                "veiculo.nao_encontrado"
                        )
                        : formatarVeiculo(veiculo)
        );
    }

    private String formatarVeiculo(Veiculo veiculo) {

        return I18nUtils.getString("comum.id")
                + veiculo.getId()
                + " | "
                + I18nUtils.getString("veiculo.placa")
                + veiculo.getPlaca()
                + " | "
                + I18nUtils.getString("veiculo.marca")
                + veiculo.getMarca()
                + " | "
                + I18nUtils.getString("veiculo.modelo")
                + veiculo.getModelo()
                + " | "
                + I18nUtils.getString("veiculo.ano_label")
                + veiculo.getAno()
                + " | "
                + I18nUtils.getString("veiculo.preco")
                + I18nUtils.formatCurrency(
                        veiculo.getPreco()
                )
                + " | "
                + I18nUtils.getString("veiculo.status")
                + I18nUtils.formatVehicleStatus(
                        veiculo.getStatus()
                );
    }

}