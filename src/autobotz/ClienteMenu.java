package autobotz;

import java.util.Scanner;

public class ClienteMenu {

    private ClienteService clienteService;

    public ClienteMenu() {
        this.clienteService = new ClienteService();
    }

    public void menuAnonimizacao(Scanner scanner) {

        System.out.println("\n===== ANONIMIZAÇÃO DE CLIENTE =====");
        System.out.print("Digite o ID do cliente: ");

        int id = scanner.nextInt();
        scanner.nextLine();

        clienteService.anonimizar(id);
    }
}