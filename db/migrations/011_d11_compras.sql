-- D11: fornecedores compartilhados, orçamento e fluxo de compras.
-- Aplicar no banco escolhido; não cria nem seleciona database.
CREATE TABLE IF NOT EXISTS fornecedores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cnpj VARCHAR(20) NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NULL,
    telefone VARCHAR(20) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_fornecedores_cnpj (cnpj)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orcamentos_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    saldo_inicial DECIMAL(12,2) NOT NULL,
    saldo_atual DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_orcamento_saldo CHECK (
        saldo_atual >= 0 AND saldo_atual <= saldo_inicial
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solicitacoes_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    orcamento_id INT NOT NULL,
    fornecedor_id INT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    solicitante VARCHAR(60) NOT NULL,
    aprovador VARCHAR(60) NULL,
    motivo_rejeicao VARCHAR(200) NULL,
    decidido_em DATETIME NULL,
    CONSTRAINT chk_solicitacao_valor CHECK (valor > 0),
    CONSTRAINT fk_solicitacao_orcamento FOREIGN KEY (orcamento_id)
        REFERENCES orcamentos_compra(id),
    CONSTRAINT fk_solicitacao_fornecedor FOREIGN KEY (fornecedor_id)
        REFERENCES fornecedores(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pedidos_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    solicitacao_id INT NOT NULL,
    orcamento_id INT NOT NULL,
    fornecedor_id INT NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_emissao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pedido_solicitacao (solicitacao_id),
    CONSTRAINT chk_pedido_valor CHECK (valor > 0),
    CONSTRAINT fk_pedido_solicitacao FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacoes_compra(id),
    CONSTRAINT fk_pedido_orcamento FOREIGN KEY (orcamento_id)
        REFERENCES orcamentos_compra(id),
    CONSTRAINT fk_pedido_fornecedor FOREIGN KEY (fornecedor_id)
        REFERENCES fornecedores(id)
) ENGINE=InnoDB;
