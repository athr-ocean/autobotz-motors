-- D15 — eventos do portal de treinamento e métricas agregadas
-- Migração aditiva. Testar primeiro em banco isolado com sufixo _test.
-- Não inserir dados simulados nesta tabela durante testes de métricas de produção.

CREATE TABLE IF NOT EXISTS treinamento_eventos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_evento VARCHAR(32) NOT NULL,
    material_slug VARCHAR(100) NULL,
    ocorrido_em DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_treinamento_ocorrido_tipo (ocorrido_em, tipo_evento),
    INDEX idx_treinamento_material_ocorrido (material_slug, ocorrido_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
