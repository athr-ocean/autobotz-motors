-- 014_d14_security.sql  (D14 - Cybersecurity, parte de Joao: login seguro, bloqueio e MFA)
--
-- Migracao ADITIVA e idempotente. Nao usa CREATE DATABASE / USE: aplique-a
-- apenas no banco que voce selecionou (ex.: autobotz_<algo>_test para testes).
-- Nao apaga nem altera dados existentes.
--
-- A parte de Arthur (colunas de cifra/indice HMAC em `clientes`) deve ser
-- acrescentada por ele neste mesmo numero de migracao ou em arquivo proprio
-- combinado com Joao; nada de `clientes` e tocado aqui.

-- 1) Hash de senha versionado (pbkdf2_sha256$iter$sal$hash ~ 100 chars).
--    Hashes SHA-256 legados (64 hex) continuam validos e cabem na nova coluna.
ALTER TABLE usuarios MODIFY senha_hash VARCHAR(255) NOT NULL;

-- 2) Trilha de seguranca: tentativas de login, bloqueios, MFA, migracao de hash.
--    Nunca grava senha, codigo MFA, token ou hash. `origem` fica NULL quando a
--    origem de rede nao esta disponivel (execucao desktop/console local).
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

-- 3) Desafios MFA de uso unico (codigo guardado apenas como sal+hash).
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
