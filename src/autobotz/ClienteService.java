package autobotz;

public class ClienteService {

    private ClienteDAO clienteDAO;

    public ClienteService() {
        this.clienteDAO = new ClienteDAO();
    }

    public boolean anonimizar(int id) {

        Cliente cliente = clienteDAO.buscarPorId(id);

        if (cliente == null) {
            System.out.println("Cliente não encontrado.");
            return false;
        }

        if (!cliente.isAtivo()) {
            System.out.println("Cliente já está inativo.");
            return false;
        }

        boolean sucesso = clienteDAO.anonimizar(id);

        if (sucesso) {
            System.out.println("Cliente anonimizado com sucesso.");
        }

        return sucesso;
    }
}