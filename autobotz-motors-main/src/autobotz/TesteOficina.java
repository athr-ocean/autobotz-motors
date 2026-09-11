package autobotz;

import java.time.LocalDate;

public class TesteOficina {

    public static void main(String[] args) {

        ServicoDAO servicoDAO = new ServicoDAO();
 
        Servico servico1 = new Servico("Troca de pneu", 250.00);
        Servico servico2 = new Servico("Alinhamento", 120.00);
     
        servicoDAO.inserir(servico1);
        servicoDAO.inserir(servico2);

        System.out.println("ID do serviço 1: " + servico1.getId());
        System.out.println("ID do serviço 2: " + servico2.getId());

        OrdemServicoDAO ordemServicoDAO = new OrdemServicoDAO();

        OrdemServico os = new OrdemServico(
            2,
            2,
            LocalDate.now(),
            "Aberta"
        );
       
        ordemServicoDAO.inserirOrdem(os);

        System.out.println("ID da Ordem de Serviço: " + os.getId());
      
        ItemOS item1 = new ItemOS(
            os.getId(),
            servico1.getId(),
            1,
            servico1.getPreco()
        );

        
        ItemOS item2 = new ItemOS(
            os.getId(),
            servico2.getId(),
            1,
            servico2.getPreco()
        );

   
        ordemServicoDAO.inserirItem(item1);
        ordemServicoDAO.inserirItem(item2);

        System.out.println("Itens inseridos com sucesso.");
       
        System.out.println("\n--- DADOS DA ORDEM DE SERVIÇO ---");

        ordemServicoDAO.buscarItensDaOrdem(os.getId());
        
        double total = ordemServicoDAO.calcularTotal(os.getId());

        System.out.println("\nTotal da Ordem de Serviço: R$ " + total);
    }
}