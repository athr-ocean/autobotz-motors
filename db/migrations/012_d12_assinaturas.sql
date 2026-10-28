-- D12: planos, assinaturas, módulos e eventos financeiros.
-- Aplicar no banco escolhido; não cria nem seleciona database.
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
