# AutoBotz Motors - Sistema de Gestão

Sistema de gerenciamento de concessionária desenvolvido em **Java** utilizando **JDBC** para integração com banco de dados **MySQL** sob a arquitetura **DAO (Data Access Object)**.

### 🚀 Funcionalidades
* **Veículos**: Cadastro, listagem, atualização de dados e exclusão (CRUD Completo).
* **Clientes**: Cadastro e listagem de clientes.
* **Vendas**: Registro de transações vinculando cliente e veículo, com alteração automática do status do veículo para `Vendido`.

### 🛠️ Tecnologias Utilizadas
* **Linguagem**: Java (JDK 21)
* **Banco de Dados**: MySQL / phpMyAdmin
* **Conectividade**: JDBC (Java Database Connectivity)
* **IDE**: Eclipse IDE

### 🗄️ Estrutura do Banco de Dados
```sql
CREATE DATABASE IF NOT EXISTS autobotz_db;
USE autobotz_db;

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS veiculos (
    id_veiculo INT AUTO_INCREMENT PRIMARY KEY,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    ano INT NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'Disponível'
);

CREATE TABLE IF NOT EXISTS vendas (
    id_venda INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_veiculo INT NOT NULL,
    data_venda DATETIME DEFAULT CURRENT_TIMESTAMP,
    valor_final DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    FOREIGN KEY (id_veiculo) REFERENCES veiculos(id_veiculo)
);
