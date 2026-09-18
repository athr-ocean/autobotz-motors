package autobotz.service;

import java.sql.SQLException;

import autobotz.dao.ClienteDAO;

public class ClienteService {
    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this(new ClienteDAO());
    }

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public boolean anonimizar(int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID do cliente invalido.");
        return clienteDAO.anonimizar(id);
    }
}
