CREATE DATABASE IF NOT EXISTS autobotz_db;
USE autobotz_db;

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
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(150),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nome_usuario VARCHAR(50) NOT NULL UNIQUE,
    senha_hash CHAR(64) NOT NULL,
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
CREATE TABLE IF NOT EXISTS planos_assinatura (
    id_plano INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco_base DECIMAL(10,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_plano_preco
        CHECK (preco_base >= 0)
);

CREATE TABLE IF NOT EXISTS modulos_opcionais (
    id_modulo INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_modulo_preco
        CHECK (preco >= 0)
);

CREATE TABLE IF NOT EXISTS assinaturas_clientes (
    id_assinatura INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_plano INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    data_inicio DATE NOT NULL,
    data_cancelamento DATE,
    categoria_cancelamento VARCHAR(50),
    motivo_cancelamento TEXT,
    feedback_simulado TEXT,

    CONSTRAINT fk_assinatura_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente),

    CONSTRAINT fk_assinatura_plano
        FOREIGN KEY (id_plano)
        REFERENCES planos_assinatura(id_plano)
);

CREATE TABLE IF NOT EXISTS assinatura_modulos (
    id_assinatura INT NOT NULL,
    id_modulo INT NOT NULL,

    PRIMARY KEY (id_assinatura, id_modulo),

    CONSTRAINT fk_assmod_assinatura
        FOREIGN KEY (id_assinatura)
        REFERENCES assinaturas_clientes(id_assinatura),

    CONSTRAINT fk_assmod_modulo
        FOREIGN KEY (id_modulo)
        REFERENCES modulos_opcionais(id_modulo)
);

CREATE TABLE IF NOT EXISTS assinatura_eventos (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    id_assinatura INT NOT NULL,
    tipo_evento VARCHAR(30) NOT NULL,
    data_evento DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    descricao TEXT,
    valor_anterior DECIMAL(10,2),
    valor_novo DECIMAL(10,2),
    categoria_motivo VARCHAR(50),
    feedback_simulado TEXT,

    CONSTRAINT fk_evento_assinatura
        FOREIGN KEY (id_assinatura)
        REFERENCES assinaturas_clientes(id_assinatura)
);