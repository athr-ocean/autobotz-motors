package autobotz.service;

import java.sql.SQLException;

import autobotz.dao.ClienteDAO;
import autobotz.util.I18nUtils;

public class ClienteService {
    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this(new ClienteDAO());
    }

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public boolean anonimizar(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("cliente.id_invalido")
            );
        }
        return clienteDAO.anonimizar(id);
    }
}
