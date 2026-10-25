package autobotz.util;

import java.sql.*;

/** Migração opcional de D9. Não inventa o projeto de membros legados nem remove dados. */
public final class MigracaoMembrosProjeto {
    public static void main(String[] args) throws SQLException {
        boolean aplicar = args.length == 1 && "--apply".equals(args[0]);
        if (args.length > 0 && !aplicar) throw new IllegalArgumentException("Use sem argumentos para verificar ou --apply para migrar.");
        try (Connection c = ConexaoBanco.getConexao()) {
            boolean coluna;
            try (var r = c.getMetaData().getColumns(c.getCatalog(), null, "membros_projeto", "id_projeto")) {
                coluna = r.next();
            }
            try (Statement s = c.createStatement()) {
                if (!coluna) {
                    try (var r = s.executeQuery("SELECT COUNT(*) FROM membros_projeto")) {
                        r.next();
                        if (r.getInt(1) > 0) throw new SQLException("Membros legados sem projeto. Associe-os explicitamente antes de migrar; nenhum dado foi alterado.");
                    }
                    if (!aplicar) { System.out.println("Pendente: criar id_projeto e FK. Tabela vazia."); return; }
                    s.executeUpdate("ALTER TABLE membros_projeto ADD COLUMN id_projeto INT NULL AFTER id");
                }
                try (var r = s.executeQuery("SELECT COUNT(*) FROM membros_projeto m LEFT JOIN projetos p "
                        + "ON p.id_projeto=m.id_projeto WHERE m.id_projeto IS NULL OR p.id_projeto IS NULL")) {
                    r.next();
                    if (r.getInt(1) > 0) throw new SQLException("Membros sem projeto válido. Corrija os vínculos manualmente; nenhum dado foi removido.");
                }
                boolean fk = false;
                try (var r = c.getMetaData().getImportedKeys(c.getCatalog(), null, "membros_projeto")) {
                    while (r.next()) if ("id_projeto".equals(r.getString("FKCOLUMN_NAME"))
                            && "projetos".equals(r.getString("PKTABLE_NAME"))) fk = true;
                }
                if (!aplicar) { System.out.println(fk ? "FK presente; vínculos válidos." : "Vínculos válidos; FK pendente. Use --apply após backup."); return; }
                s.executeUpdate("ALTER TABLE membros_projeto MODIFY COLUMN id_projeto INT NOT NULL");
                if (!fk) s.executeUpdate("ALTER TABLE membros_projeto ADD CONSTRAINT fk_membros_projeto_projeto "
                        + "FOREIGN KEY (id_projeto) REFERENCES projetos(id_projeto) ON DELETE CASCADE");
                System.out.println("D9: vínculos validados e FK presente.");
            }
        }
    }
}
