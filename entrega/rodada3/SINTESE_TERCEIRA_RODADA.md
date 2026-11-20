# Terceira rodada — síntese das entregas

AutoBotz Motors — Equipe B (Liga do Mal)  
Disciplina: Administração para Ciência da Computação, 2026.2

Este texto reúne o que foi apresentado pela equipe nos desafios D1 a D5. O trabalho segue a divisão do enunciado entre análise e proposta (A) e implementação demonstrável no ERP (I). Os recursos que dependem de infraestrutura externa são descritos como proposta ou simulação, sem serem confundidos com uma implantação real.

## D1 — Mudanças regulamentares

A análise de João partiu da Lei de Responsabilidade Algorítmica descrita no cenário do professor. O documento identifica regras automáticas e decisões humanas presentes no ERP, discute a rastreabilidade de operações e propõe registros auditáveis, critérios de explicação e revisão de decisões. A política de explicabilidade complementa essa avaliação. Os módulos fiscal e de crédito, destacados no enunciado, foram discutidos como possíveis áreas de impacto; o AutoBotz atual não dispõe de um módulo de concessão de crédito.

A entrega do D1 é uma análise, não a implementação de uma nova lei ou a certificação jurídica do software. O enunciado não apresenta artigos ou prazos da lei hipotética.

Documentos: [análise de responsabilidade algorítmica](D1_RESPONSABILIDADE_ALGORITMICA.md) e [política de explicabilidade](D1_POLITICA_EXPLICABILIDADE.md).

## D2 — Demanda do cliente

A equipe propôs REST/JSON como protocolo inicial para a integração com o GovConnect. Alexandre implementou uma demonstração que consulta pedidos do módulo de compras, verifica a autorização de envio e utiliza OAuth 2.0 Client Credentials para comunicação entre serviços simulados. Gabriel documentou as alternativas REST, GraphQL e SOAP, os dados envolvidos e os riscos de segurança e continuidade.

O fluxo foi testado com servidores locais e banco MariaDB descartável. Não houve acesso a um serviço governamental real. Também não foi implantado login federado de usuários: o fluxo Client Credentials é de máquina para máquina. A integração real dependeria de especificações e credenciais oficiais.

Documentos: [implementação demonstrativa](D2_GOVCONNECT_IMPLEMENTACAO_ALEXANDRE.md) e [análise técnica de Gabriel](../../docs/D2_ANALISE_INTEGRACAO_GOVCONNECT_GABRIEL.md).

## D3 — Concorrência

André comparou o AutoBotz a um concorrente fictício que utiliza IA generativa para gerar relatórios e dashboards. A análise reconhece os relatórios e indicadores já presentes no sistema, identifica lacunas em filtros, visualizações e interação por linguagem natural, e propõe melhorias por prioridade e esforço. Os recursos atribuídos ao concorrente além do que está no enunciado são tratados como hipóteses.

Este desafio pede análise e proposta. As funcionalidades sugeridas não foram implementadas como parte do D3.

Documento: [análise de concorrência](../../docs/D3_CONCORRENCIA.md).

## D4 — Feedback do cliente

Higor organizou a análise de usabilidade e o material de registro dos testes. Mauro desenvolveu ajustes no portal de treinamento, incluindo navegação entre materiais, busca, apresentação em telas estreitas, contraste, foco e restauração da interface após recarga.

Há registros de verificação manual do portal em diferentes larguras de tela e temas, além de testes automatizados e capturas. Um teste em aparelho Android físico foi tentado, mas não concluído por dificuldade de reconhecimento USB no ambiente utilizado. As evidências correspondem ao portal web; não demonstram responsividade de todas as telas do ERP Java Swing, nem uma pesquisa de satisfação com participantes.

Documentos: [análise de usabilidade](../../docs/D4_FEEDBACK_CLIENTE.md), [registro de Higor](../../docs/REGISTRO_VALIDACAO_HIGOR_20-11.md) e [validação manual de Mauro](../../docs/evidencias-d4/manual-local/VALIDACAO.md).

## D5 — Problemas técnicos

Arthur implementou uma prova de conceito para proteger, verificar e recuperar registros de log por meio de cópias locais e manifestos SHA-256. Os testes simulam adulteração, perda e restauração de logs, falhas de transação em MariaDB descartável, backup e recuperação SQL, além de uma publicação defeituosa do portal seguida de retorno aos arquivos estáveis.

A redundância geográfica e a continuidade entre regiões foram tratadas no plano técnico. Não houve implantação multi-região nem failover de nuvem. A revisão complementar de Adryan foi prevista pela equipe, mas não deve ser declarada concluída sem registro próprio.

Documentos: [plano de recuperação](D5_PLANO_RECUPERACAO.md) e [evidências e demonstração](D5_FECHAMENTO_E_DEMONSTRACAO.md).

## Verificação e apresentação

Os três workflows de GitHub Actions da versão `35bcd2bdc5` passaram: regressão integrada, D12 e segurança D14. A regressão integrada inclui testes de compras, GovConnect simulado, segurança, portal e recuperação D5 em bancos descartáveis. Essas verificações não equivalem a testes de produção ou validação de serviços externos.

O enunciado prevê vídeo de 30 minutos sobre a trajetória dos desafios, texto finalizado em **01/12/2026** e defesa em **04/12/2026**. O repositório reúne código, relatórios e evidências técnicas; a conclusão e a entrega dos materiais audiovisuais e do artigo devem ser conferidas separadamente.
