-- D14 (Arthur): estrutura para criptografia dos dados pessoais de clientes.
-- Migration aditiva e idempotente. Não cria nem seleciona banco; aplique após
-- 014_d14_security.sql e 015_d15_treinamento.sql no banco escolhido.
-- A conversão dos registros legados permanece explícita em MigracaoDadosPessoais.

ALTER TABLE clientes
    MODIFY COLUMN nome VARCHAR(1024) NOT NULL,
    MODIFY COLUMN cpf VARCHAR(1024) NOT NULL,
    MODIFY COLUMN telefone VARCHAR(1024) NULL,
    MODIFY COLUMN email VARCHAR(1024) NULL;

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS cpf_hash CHAR(64) NULL AFTER cpf;

CREATE INDEX IF NOT EXISTS idx_clientes_cpf_hash ON clientes(cpf_hash);
