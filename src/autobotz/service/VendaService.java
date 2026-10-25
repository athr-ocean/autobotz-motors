package autobotz.service;

import autobotz.dao.VendaDAO;
import autobotz.model.Venda;
import java.sql.SQLException;

public class VendaService {
    private VendaDAO vendaDAO;

    public VendaService() {
        this.vendaDAO = new VendaDAO();
    }

    public void processarVenda(Venda venda) throws SQLException {
        vendaDAO.registrarVenda(venda);
    }

    public void realizarVenda(int idCliente, int idVeiculo, double valorFinal) throws SQLException {
        processarVenda(new Venda(idVeiculo, idCliente, valorFinal));
    }
}
