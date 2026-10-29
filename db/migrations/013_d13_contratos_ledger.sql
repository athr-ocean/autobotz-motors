-- D13 - contratos de fornecedor e ledger de integridade simulado.
-- Pré-requisito: D11 deve criar fornecedores(id INT PRIMARY KEY) antes desta migração.
-- Aplicar somente em banco de teste/homologação durante a validação.

CREATE TABLE IF NOT EXISTS contratos_fornecedor (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fornecedor_id INT NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'RASCUNHO',
    nota_fiscal_validada BOOLEAN NOT NULL DEFAULT FALSE,
    pagamento_simulado_liberado BOOLEAN NOT NULL DEFAULT FALSE,
    ledger_ultima_sequencia INT NOT NULL DEFAULT 0,
    ledger_ultimo_hash CHAR(64) NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000',
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_contrato_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_contrato_ledger_sequencia CHECK (ledger_ultima_sequencia >= 0),
    CONSTRAINT fk_contrato_fornecedor
        FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_contrato_vencimento (data_vencimento, estado, nota_fiscal_validada)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ledger_contratos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contrato_id BIGINT NOT NULL,
    sequencia INT NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    payload TEXT NOT NULL,
    hash_conteudo CHAR(64) NOT NULL,
    hash_anterior CHAR(64) NOT NULL,
    hash_registro CHAR(64) NOT NULL,
    data_hora DATETIME(6) NOT NULL,
    CONSTRAINT uq_ledger_contrato_sequencia UNIQUE (contrato_id, sequencia),
    CONSTRAINT chk_ledger_sequencia_positiva CHECK (sequencia > 0),
    CONSTRAINT fk_ledger_contrato
        FOREIGN KEY (contrato_id) REFERENCES contratos_fornecedor(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_ledger_contrato_data (contrato_id, data_hora)
) ENGINE=InnoDB;
