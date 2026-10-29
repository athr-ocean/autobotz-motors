-- CREATE DATABASE IF NOT EXISTS autobotz_db;
-- USE autobotz_db;

CREATE TABLE IF NOT EXISTS veiculos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    modelo VARCHAR(50) NOT NULL,
    marca VARCHAR(50) NOT NULL,
    ano INT NOT NULL,
    preco DOUBLE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL'
);

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(1024) NOT NULL,
    cpf VARCHAR(1024) NOT NULL,
    cpf_hash CHAR(64) NULL,
    telefone VARCHAR(1024),
    email VARCHAR(1024),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nome_usuario VARCHAR(50) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS vendas (
    id_venda INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_veiculo INT NOT NULL,
    valor_final DOUBLE NOT NULL,
    data_venda DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_venda_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    CONSTRAINT fk_venda_veiculo FOREIGN KEY (id_veiculo) REFERENCES veiculos(id)
);

CREATE TABLE IF NOT EXISTS logs_auditoria (
    id_log BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL,
    nome_usuario VARCHAR(50) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(100) NOT NULL,
    entidade_id INT NULL,
    detalhes TEXT,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS servicos (
    id_servico INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS ordens_servico (
    id_ordem INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_veiculo INT NOT NULL,
    data_abertura DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_ordem_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    CONSTRAINT fk_ordem_veiculo FOREIGN KEY (id_veiculo) REFERENCES veiculos(id)
);

CREATE TABLE IF NOT EXISTS itens_os (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    id_ordem INT NOT NULL,
    id_servico INT NOT NULL,
    quantidade INT NOT NULL,
    preco DOUBLE NOT NULL,
    CONSTRAINT fk_item_ordem FOREIGN KEY (id_ordem) REFERENCES ordens_servico(id_ordem),
    CONSTRAINT fk_item_servico FOREIGN KEY (id_servico) REFERENCES servicos(id_servico)
);

CREATE TABLE IF NOT EXISTS projetos (
    id_projeto INT AUTO_INCREMENT PRIMARY KEY,
    nome_projeto VARCHAR(150) NOT NULL UNIQUE,
    responsavel VARCHAR(100) NOT NULL,
    equipe VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS membros_projeto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_projeto INT NOT NULL,
    lista_membros TEXT NOT NULL,
    CONSTRAINT fk_membros_projeto_projeto
        FOREIGN KEY (id_projeto)
        REFERENCES projetos(id_projeto)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS eventos_seguranca (
    id_evento BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_hora DATETIME(3) NOT NULL,
    nome_usuario VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    sucesso BOOLEAN NOT NULL,
    causa VARCHAR(60) NULL,
    origem VARCHAR(100) NULL,
    INDEX idx_evseg_login (nome_usuario, tipo, sucesso, data_hora),
    INDEX idx_evseg_data (data_hora)
);

CREATE TABLE IF NOT EXISTS mfa_desafios (
    id_desafio BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    criado_em DATETIME(3) NOT NULL,
    expira_em DATETIME(3) NOT NULL,
    tentativas INT NOT NULL DEFAULT 0,
    max_tentativas INT NOT NULL,
    consumido BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_mfa_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    INDEX idx_mfa_usuario (id_usuario, consumido)
);

-- Compatibilidade com bancos criados antes desta correção.
ALTER TABLE membros_projeto
    ADD COLUMN IF NOT EXISTS id_projeto INT NULL AFTER id;

ALTER TABLE membros_projeto
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

-- D11: compras e fornecedores. Mantido em sincronia com db/migrations/011_d11_compras.sql.
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

-- D12: planos, assinaturas, módulos e eventos financeiros.
CREATE TABLE IF NOT EXISTS planos_assinatura (
    id_plano INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco_base DECIMAL(10,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_plano_preco CHECK (preco_base >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS modulos_opcionais (
    id_modulo INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_modulo_preco CHECK (preco >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assinaturas_clientes (
    id_assinatura INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_plano INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    data_inicio DATE NOT NULL,
    data_cancelamento DATE NULL,
    categoria_cancelamento VARCHAR(50) NULL,
    motivo_cancelamento TEXT NULL,
    feedback_simulado TEXT NULL,
    CONSTRAINT fk_assinatura_cliente FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente),
    CONSTRAINT fk_assinatura_plano FOREIGN KEY (id_plano)
        REFERENCES planos_assinatura(id_plano)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assinatura_modulos (
    id_assinatura INT NOT NULL,
    id_modulo INT NOT NULL,
    PRIMARY KEY (id_assinatura, id_modulo),
    CONSTRAINT fk_assmod_assinatura FOREIGN KEY (id_assinatura)
        REFERENCES assinaturas_clientes(id_assinatura),
    CONSTRAINT fk_assmod_modulo FOREIGN KEY (id_modulo)
        REFERENCES modulos_opcionais(id_modulo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assinatura_eventos (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    id_assinatura INT NOT NULL,
    tipo_evento VARCHAR(30) NOT NULL,
    data_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descricao TEXT NULL,
    valor_anterior DECIMAL(10,2) NULL,
    valor_novo DECIMAL(10,2) NULL,
    categoria_motivo VARCHAR(50) NULL,
    feedback_simulado TEXT NULL,
    CONSTRAINT fk_evento_assinatura FOREIGN KEY (id_assinatura)
        REFERENCES assinaturas_clientes(id_assinatura)
) ENGINE=InnoDB;

-- D13: contratos de fornecedores e ledger de integridade simulado.
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

-- D14: espaço para campos cifrados e índice de busca HMAC do CPF.
-- A migração dos dados existentes é explícita; este DDL não cifra registros.
ALTER TABLE clientes
    MODIFY COLUMN nome VARCHAR(1024) NOT NULL,
    MODIFY COLUMN cpf VARCHAR(1024) NOT NULL,
    MODIFY COLUMN telefone VARCHAR(1024) NULL,
    MODIFY COLUMN email VARCHAR(1024) NULL;

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS cpf_hash CHAR(64) NULL AFTER cpf;

CREATE INDEX IF NOT EXISTS idx_clientes_cpf_hash ON clientes(cpf_hash);

-- D15: eventos do portal de treinamento e métricas agregadas
CREATE TABLE IF NOT EXISTS treinamento_eventos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_evento VARCHAR(32) NOT NULL,
    material_slug VARCHAR(100) NULL,
    ocorrido_em DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_treinamento_ocorrido_tipo (ocorrido_em, tipo_evento),
    INDEX idx_treinamento_material_ocorrido (material_slug, ocorrido_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
