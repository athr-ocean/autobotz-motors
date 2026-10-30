# Autoria e integração

Este documento registra contribuições originais identificadas no histórico Git e distingue essas contribuições de adaptações feitas durante a integração. Nenhuma adaptação desta intervenção é atribuída aos autores dos módulos.

| Integrante | Evidência no histórico | Contribuição original preservada |
|---|---|---|
| Arthur | `9e130ea`, `eeac6be` | Entrada principal, conexão JDBC e integração de Oficina ao fluxo da aplicação. |
| Alexandre | `e0b53c8`, `f5a4210` | DAOs e persistência da Oficina, itens de OS, consultas e cálculo de valores. |
| Mauro | `09dd6ed`, `d0efa9e` | Regras e fluxo de serviços da Oficina. |
| Gabriel | `3552ac8`, `f676853` | Internacionalização PT-BR e EN-US e mensagens da aplicação. |
| Higor | `0219a18` | Operações de clientes e tratamento de dados pessoais. |
| André | `4638bb8`, `17592ef` | CRM e integração do histórico de vendas. |
| João | `62f20c5`, `eeac6be` | Autorização, projetos e vínculo de membros a cada projeto. |

Adaptações posteriores incluíram compatibilização dos DAOs e serviços com a integração, suporte às telas Swing, ajustes de formatação local, correções de validação e ferramentas de execução/teste. Essas adaptações não alteram a autoria dos commits originais e não são atribuídas individualmente aos integrantes acima.

O commit `f676853`, exclusivo da `main` no snapshot analisado, traduz uma versão anterior do menu de Oficina e altera seu construtor. A integração final manteve o contrato usado no código consolidado. Nenhum merge ou reescrita de histórico é registrado aqui como concluído.

O material oficial associado ao D6 indica uma exigência de Java Swing. A divergência entre esse requisito e a divisão interna da equipe deve ser confirmada com o professor. O trabalho individual de Adryan ligado ao D10 e ao teste de stress/freeze não foi reaberto.
