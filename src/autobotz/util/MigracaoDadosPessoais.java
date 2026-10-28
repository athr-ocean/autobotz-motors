package autobotz.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Migração explícita de dados pessoais antigos armazenados em texto puro.
 *
 * O padrão é somente diagnóstico. Alterações exigem --apply e confirmação
 * explícita do nome do banco ativo. Faça backup do banco e da chave antes.
 */
public final class MigracaoDadosPessoais {
    private MigracaoDadosPessoais() {
    }

    private record ClienteLegado(
            int id,
            String nome,
            String cpf,
            String cpfHash,
            String telefone,
            String email) {
    }

    private record ClienteProtegido(
            int id,
            String nome,
            String cpf,
            String cpfHash,
            String telefone,
            String email,
            boolean precisaAtualizar) {
    }

    public static void main(String[] args) {
        try {
            executar(args);
        } catch (Exception e) {
            System.err.println("MIGRACAO_DADOS_PESSOAIS=ERRO");
            System.err.println("MOTIVO=" + mensagemSegura(e));
            System.exit(1);
        }
    }

    private static void executar(String[] args) throws SQLException {
        boolean aplicar = args.length == 2
                && "--apply".equals(args[0])
                && args[1].startsWith("--confirm-db=");
        boolean verificar = args.length == 1 && "--check".equals(args[0]);

        if (!aplicar && !verificar) {
            throw new IllegalArgumentException(
                    "Uso: MigracaoDadosPessoais --check OU "
                            + "MigracaoDadosPessoais --apply --confirm-db=NOME_EXATO_DO_BANCO");
        }

        CriptografiaDadosPessoais.validarConfiguracao();
        Connection conexao = ConexaoBanco.getConexao();
        String bancoAtual = conexao.getCatalog();
        if (bancoAtual == null || bancoAtual.isBlank()) {
            throw new SQLException("Não foi possível identificar o banco ativo.");
        }

        if (aplicar) {
            String bancoConfirmado = args[1].substring("--confirm-db=".length());
            if (!bancoAtual.equals(bancoConfirmado)) {
                throw new SQLException(
                        "O banco ativo não corresponde ao nome confirmado. Nenhuma alteração foi feita.");
            }
        }

        boolean autoCommitAnterior = conexao.getAutoCommit();
        if (aplicar) {
            conexao.setAutoCommit(false);
        }

        try {
            List<ClienteLegado> clientes = carregar(conexao, aplicar);
            List<ClienteProtegido> pendentes = new ArrayList<>();
            for (ClienteLegado cliente : clientes) {
                ClienteProtegido protegido = preparar(cliente);
                if (protegido.precisaAtualizar()) {
                    pendentes.add(protegido);
                }
            }

            if (aplicar) {
                for (ClienteProtegido cliente : pendentes) {
                    atualizar(conexao, cliente);
                }
                conexao.commit();
            }

            System.out.println("BANCO_CONFIRMADO=" + bancoAtual);
            System.out.println("REGISTROS_ANALISADOS=" + clientes.size());
            System.out.println("REGISTROS_PENDENTES=" + pendentes.size());
            System.out.println(aplicar
                    ? "MIGRACAO_DADOS_PESSOAIS=APLICADA"
                    : "MIGRACAO_DADOS_PESSOAIS=DIAGNOSTICO_SEM_ALTERACOES");
        } catch (SQLException | RuntimeException e) {
            if (aplicar) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
            }
            throw e;
        } finally {
            if (aplicar) {
                try {
                    conexao.setAutoCommit(autoCommitAnterior);
                } catch (SQLException ignored) {
                    // A conexão será encerrada pelo processo da ferramenta.
                }
            }
        }
    }

    private static List<ClienteLegado> carregar(Connection conexao, boolean bloquear)
            throws SQLException {
        String sql = "SELECT id_cliente, nome, cpf, cpf_hash, telefone, email "
                + "FROM clientes ORDER BY id_cliente"
                + (bloquear ? " FOR UPDATE" : "");
        List<ClienteLegado> clientes = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientes.add(new ClienteLegado(
                        rs.getInt("id_cliente"),
                        rs.getString("nome"),
                        rs.getString("cpf"),
                        rs.getString("cpf_hash"),
                        rs.getString("telefone"),
                        rs.getString("email")));
            }
        }
        return clientes;
    }

    private static ClienteProtegido preparar(ClienteLegado cliente) throws SQLException {
        String nomeClaro = CriptografiaDadosPessoais.revelar(cliente.nome(), "nome");
        String cpfClaro = CriptografiaDadosPessoais.revelar(cliente.cpf(), "cpf");
        String telefoneClaro = CriptografiaDadosPessoais.revelar(cliente.telefone(), "telefone");
        String emailClaro = CriptografiaDadosPessoais.revelar(cliente.email(), "email");

        String nomeSeguro = CriptografiaDadosPessoais.isProtegido(cliente.nome())
                ? cliente.nome()
                : CriptografiaDadosPessoais.proteger(nomeClaro, "nome");
        String cpfSeguro = CriptografiaDadosPessoais.isProtegido(cliente.cpf())
                ? cliente.cpf()
                : CriptografiaDadosPessoais.proteger(cpfClaro, "cpf");
        String telefoneSeguro = cliente.telefone() == null
                ? null
                : CriptografiaDadosPessoais.isProtegido(cliente.telefone())
                        ? cliente.telefone()
                        : CriptografiaDadosPessoais.proteger(telefoneClaro, "telefone");
        String emailSeguro = cliente.email() == null
                ? null
                : CriptografiaDadosPessoais.isProtegido(cliente.email())
                        ? cliente.email()
                        : CriptografiaDadosPessoais.proteger(emailClaro, "email");
        String hash = CriptografiaDadosPessoais.fingerprintCpf(cpfClaro);

        boolean precisaAtualizar = !Objects.equals(cliente.nome(), nomeSeguro)
                || !Objects.equals(cliente.cpf(), cpfSeguro)
                || !Objects.equals(cliente.telefone(), telefoneSeguro)
                || !Objects.equals(cliente.email(), emailSeguro)
                || !Objects.equals(cliente.cpfHash(), hash);

        return new ClienteProtegido(
                cliente.id(), nomeSeguro, cpfSeguro, hash,
                telefoneSeguro, emailSeguro, precisaAtualizar);
    }

    private static void atualizar(Connection conexao, ClienteProtegido cliente)
            throws SQLException {
        String sql = "UPDATE clientes SET nome = ?, cpf = ?, cpf_hash = ?, "
                + "telefone = ?, email = ? WHERE id_cliente = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cliente.nome());
            stmt.setString(2, cliente.cpf());
            stmt.setString(3, cliente.cpfHash());
            stmt.setString(4, cliente.telefone());
            stmt.setString(5, cliente.email());
            stmt.setInt(6, cliente.id());
            if (stmt.executeUpdate() != 1) {
                throw new SQLException(
                        "Um registro de cliente mudou durante a migração. A transação será desfeita.");
            }
        }
    }

    private static String mensagemSegura(Exception e) {
        String mensagem = e.getMessage();
        if (mensagem == null || mensagem.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return mensagem.replace('\n', ' ').replace('\r', ' ');
    }
}
