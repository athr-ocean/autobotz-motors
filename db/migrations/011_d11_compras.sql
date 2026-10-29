-- 011_d11_compras.sql — D11: fornecedores, orçamentos e fluxo de compras.
-- Migration aditiva/idempotente: não cria nem seleciona database.
-- Aplicar primeiro em banco descartável cujo nome termine em _test.
-- As tabelas usam InnoDB para garantir rollback real das operações do ComprasService.

CREATE TABLE IF NOT EXISTS fornecedores (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cnpj CHAR(14) NULL,
    CONSTRAINT uq_d11_fornecedores_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_d11_fornecedor_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_d11_fornecedor_cnpj CHECK (cnpj IS NULL OR cnpj REGEXP '^[0-9]{14}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orcamentos_compra (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    saldo_inicial DECIMAL(12,2) NOT NULL,
    saldo_atual DECIMAL(12,2) NOT NULL,
    CONSTRAINT ck_d11_orcamento_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_d11_orcamento_saldos CHECK (
        saldo_inicial >= 0 AND saldo_atual >= 0 AND saldo_atual <= saldo_inicial
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS solicitacoes_compra (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    orcamento_id INT NOT NULL,
    fornecedor_id INT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    solicitante VARCHAR(60) NOT NULL,
    aprovador VARCHAR(60) NULL,
    motivo_rejeicao VARCHAR(200) NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decidido_em TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_d11_solicitacao_status (status, criado_em),
    INDEX idx_d11_solicitacao_orcamento (orcamento_id),
    INDEX idx_d11_solicitacao_fornecedor (fornecedor_id),
    CONSTRAINT fk_d11_solicitacao_orcamento FOREIGN KEY (orcamento_id)
        REFERENCES orcamentos_compra(id),
    CONSTRAINT fk_d11_solicitacao_fornecedor FOREIGN KEY (fornecedor_id)
        REFERENCES fornecedores(id),
    CONSTRAINT ck_d11_solicitacao_valor CHECK (valor > 0),
    CONSTRAINT ck_d11_solicitacao_status CHECK (
        status IN ('PENDENTE', 'APROVADA', 'REJEITADA', 'PEDIDO_GERADO')
    ),
    CONSTRAINT ck_d11_solicitacao_decisao CHECK (
        status = 'PENDENTE' OR aprovador IS NOT NULL
    ),
    CONSTRAINT ck_d11_solicitacao_rejeicao CHECK (
        status <> 'REJEITADA' OR motivo_rejeicao IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pedidos_compra (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    solicitacao_id INT NOT NULL,
    orcamento_id INT NOT NULL,
    fornecedor_id INT NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_d11_pedido_solicitacao UNIQUE (solicitacao_id),
    INDEX idx_d11_pedido_orcamento (orcamento_id),
    INDEX idx_d11_pedido_fornecedor (fornecedor_id),
    CONSTRAINT fk_d11_pedido_solicitacao FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacoes_compra(id),
    CONSTRAINT fk_d11_pedido_orcamento FOREIGN KEY (orcamento_id)
        REFERENCES orcamentos_compra(id),
    CONSTRAINT fk_d11_pedido_fornecedor FOREIGN KEY (fornecedor_id)
        REFERENCES fornecedores(id),
    CONSTRAINT ck_d11_pedido_valor CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
