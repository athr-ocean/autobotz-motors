package autobotz;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

public class AutorizacaoService {
    private final ResourceBundle bundle;
    public enum Permissao {
        CADASTRAR_VEICULO,
        LISTAR_VEICULOS,
        ATUALIZAR_VEICULO,
        EXCLUIR_VEICULO,

        CADASTRAR_CLIENTE,
        LISTAR_CLIENTES,

        REALIZAR_VENDA,
        LISTAR_VENDAS,

        ACESSAR_OFICINA,
        ACESSAR_RELATORIOS,
        ACESSAR_CLIENTE_LGPD,

        CONSULTAR_MEMBROS,
        ATUALIZAR_MEMBROS,


        CONSULTAR_PROJETOS,
        CRIAR_PROJETOS,
        ATUALIZAR_PROJETOS,
        EXCLUIR_PROJETOS,

        ACESSAR_CRM



    }

    private final Map<PerfilUsuario, Set<Permissao>> matriz;

    public AutorizacaoService(ResourceBundle bundle) {
        this.bundle = bundle;
        matriz = new EnumMap<>(PerfilUsuario.class);

        /*
         * MATRIZ PROVISÓRIA
         *
         * ADMIN:
         * pode fazer tudo.
         *
         * VENDEDOR:
         * pode listar veículos,
         * cadastrar/listar clientes
         * e realizar vendas.
         *
         * ATENÇÃO:
         * esta matriz precisa ser confirmada pela equipe.
         */

        matriz.put(
            PerfilUsuario.ADMIN,
            EnumSet.allOf(Permissao.class)
        );

        matriz.put(
            PerfilUsuario.VENDEDOR,
            EnumSet.of(
                Permissao.LISTAR_VEICULOS,
                Permissao.CADASTRAR_CLIENTE,
                Permissao.LISTAR_CLIENTES,
                Permissao.REALIZAR_VENDA,
                Permissao.CONSULTAR_MEMBROS,
                Permissao.CONSULTAR_PROJETOS,
                Permissao.ACESSAR_CRM
            )
        );
    }

    public boolean temPermissao(Permissao permissao) {

        Usuario usuario =
            SessaoUsuario.getInstancia().getUsuario();

        if (usuario == null) {
            return false;
        }

        Set<Permissao> permissoes =
            matriz.get(usuario.getPerfil());

        if (permissoes == null) {
            return false;
        }

        return permissoes.contains(permissao);
    }

    public boolean exigirPermissao(Permissao permissao) {

        if (temPermissao(permissao)) {
            return true;
        }

        System.out.println(bundle.getString("Autorizacao"));

        return false;
    }
}