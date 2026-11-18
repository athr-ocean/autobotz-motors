# D4 — Feedback do Cliente e Análise de Usabilidade

**Responsável:** Higor  
**Projeto:** AutoBotz Motors  
**Situação desta entrega:** análise inicial e plano de avaliação. Os pontos abaixo são hipóteses de usabilidade a verificar na interface; não são apresentados como defeitos observados nem como resultados de testes já executados.

## 1. Objetivo

Avaliar a facilidade de uso dos fluxos operacionais prioritários do AutoBotz Motors, identificar possíveis barreiras de navegação e preenchimento, definir critérios objetivos de avaliação e orientar os testes da interface após os ajustes.

A análise considera o ERP como uma ferramenta utilizada por operadores da concessionária. Os cenários devem usar contas e dados fictícios em ambiente de teste.

## 2. Escopo considerado

A análise inicial prioriza os fluxos já conhecidos do sistema:

1. localizar ou cadastrar um cliente;
2. cadastrar um veículo e relacioná-lo ao cliente, conforme as opções disponíveis na versão executada;
3. registrar uma venda utilizando os dados cadastrados.

O fluxo de ordens de serviço/oficina pode ser incluído como prioridade seguinte, desde que esteja disponível e estável na versão que será avaliada. Esta entrega não pressupõe que telas, campos ou mensagens específicas já tenham sido inspecionados.

## 3. Pontos de usabilidade a investigar

Os itens seguintes formam uma lista de verificação para a observação da interface. Cada item só deverá ser classificado como problema confirmado depois de ser reproduzido por um participante ou identificado diretamente durante a inspeção da versão testada.

| Código | Ponto a investigar | Como verificar | Impacto possível |
|---|---|---|---|
| U01 | Clareza dos nomes dos menus e ações | Solicitar que o participante encontre cadastro, consulta e registro de venda sem orientação | Dificuldade para localizar funções |
| U02 | Identificação de campos obrigatórios e formatos aceitos | Pedir o preenchimento de formulários com dados fictícios e observar dúvidas ou erros | Retrabalho e cadastros incompletos |
| U03 | Clareza das mensagens de validação | Enviar o formulário incompleto ou com valores inválidos e observar se a mensagem explica como corrigir | Erros repetidos e dependência de suporte |
| U04 | Confirmação após salvar | Verificar se a interface informa o resultado e permite localizar o registro criado | Incerteza sobre a conclusão da operação |
| U05 | Pesquisa e localização de registros | Solicitar que o participante encontre um cliente ou veículo já cadastrado | Demora e possível duplicidade de cadastro |
| U06 | Associação entre cliente, veículo e venda | Observar se é fácil selecionar os registros corretos e perceber associações inválidas | Registro incorreto de venda |
| U07 | Cancelamento e retorno entre telas | Iniciar um cadastro, voltar ou cancelar e verificar se a interface comunica o que será descartado | Perda acidental de dados |
| U08 | Consistência de idioma e mensagens | Repetir tarefas em PT-BR e EN-US, quando ambos estiverem disponíveis | Confusão causada por textos inconsistentes |

## 4. Priorização dos fluxos

A prioridade considera impacto operacional, frequência esperada e dependência entre os cadastros. A classificação é uma proposta inicial e deve ser confirmada com o responsável pelo produto/equipe.

| Prioridade | Fluxo | Justificativa | Resultado esperado |
|---|---|---|---|
| P1 | Localizar ou cadastrar cliente | É a base para operações posteriores e evita cadastros duplicados | Cliente localizado ou salvo e recuperável pela pesquisa |
| P1 | Cadastrar veículo e associá-lo ao cliente | O veículo é uma informação central da concessionária e pode ser usado no registro de venda | Veículo salvo e associado corretamente, quando a associação existir na versão |
| P1 | Registrar venda | Conclui o fluxo comercial que utiliza cliente e veículo | Venda registrada uma única vez e consultável no sistema |
| P2 | Consultar/acompanhar ordem de serviço | Fluxo operacional relevante, mas depende de confirmar a disponibilidade da tela e do processo na versão avaliada | Ordem localizada e estado apresentado de forma compreensível |

## 5. Critérios de avaliação

Para cada participante e tarefa, registrar:

- **Conclusão da tarefa:** concluída sem ajuda, concluída com ajuda ou não concluída.
- **Tempo:** duração entre a apresentação da tarefa e sua conclusão. Não comparar participantes que receberam instruções diferentes sem registrar essa diferença.
- **Erros:** quantidade e tipo de erros observados, como campo incorreto, seleção errada, tentativa inválida ou repetição desnecessária.
- **Ajuda necessária:** quantidade de intervenções do avaliador.
- **Satisfação percebida:** nota de 1 a 5 após a tarefa, acompanhada de comentário opcional.
- **Integridade do resultado:** confirmar se o registro foi salvo uma vez, com os vínculos corretos, e pode ser localizado depois.

### Indicadores

| Indicador | Cálculo | Interpretação |
|---|---|---|
| Taxa de conclusão | tarefas concluídas sem ajuda ÷ tarefas executadas × 100 | Facilidade de concluir o fluxo |
| Tempo mediano | mediana dos tempos registrados para a mesma tarefa | Esforço/tempo necessário |
| Taxa de erro | tarefas com ao menos um erro ÷ tarefas executadas × 100 | Frequência de dificuldade operacional |
| Taxa de ajuda | tarefas que exigiram ajuda ÷ tarefas executadas × 100 | Dependência de orientação externa |
| Satisfação média | soma das notas ÷ número de respostas | Percepção do participante; interpretar junto dos comentários |

Não calcular ou divulgar indicadores sem registrar o número de participantes, as tarefas executadas, a versão testada e a origem dos dados. Com amostra pequena, apresentar os resultados como avaliação exploratória, não como prova estatística ou causal.

## 6. Regras para considerar um ajuste eficaz

Um ajuste de interface será considerado aprovado no cenário testado quando:

1. a tarefa prioritária puder ser concluída sem bloqueio;
2. mensagens de validação indicarem o problema e a ação corretiva de forma compreensível;
3. a operação concluída apresentar confirmação clara;
4. o dado persistido puder ser localizado na consulta correspondente;
5. não ocorrer duplicidade por clique repetido ou reenvio da mesma operação;
6. o cancelamento não descartar dados silenciosamente nem deixar o usuário sem orientação;
7. a navegação e as mensagens permanecerem coerentes no idioma escolhido, quando PT-BR e EN-US forem oferecidos.

Quando houver uma versão anterior disponível, executar os mesmos cenários antes e depois dos ajustes, com dados equivalentes. Relatar diferenças observadas e limitações. Não afirmar melhoria percentual sem medições comparáveis.

## 7. Procedimento de avaliação

1. Preparar uma base de teste isolada e dados fictícios.
2. Registrar a versão/commit da aplicação, o modo de execução e o idioma.
3. Convidar usuários que representem operadores do sistema; explicar que será avaliada a interface, não o desempenho pessoal.
4. Entregar as tarefas sem revelar o caminho de navegação esperado.
5. Observar, sem interferir, dúvidas, erros, retornos e pedidos de ajuda.
6. Ao final de cada tarefa, registrar tempo, conclusão, erros e nota de satisfação.
7. Agrupar os problemas observados por frequência e impacto; priorizar primeiro os bloqueios e riscos de registro incorreto.
8. Após os ajustes, repetir os mesmos cenários e comparar os resultados.

## 8. Limitações e resultados disponíveis nesta entrega

Nesta entrega foi estruturado o plano de análise, os fluxos prioritários, a lista de pontos a investigar e os critérios de avaliação. Não foram fornecidos registros de entrevistas, respostas de usuários, capturas das telas atuais nem evidências de execução da interface ajustada. Portanto:

- nenhum problema listado em U01–U08 está confirmado apenas por este documento;
- nenhuma taxa de conclusão, tempo, erro ou satisfação foi medida;
- não é possível afirmar que a interface já foi corrigida ou que os ajustes melhoraram a experiência;
- os resultados reais deverão ser adicionados após a execução do roteiro em `docs/D4_PLANO_TESTES_USABILIDADE.md`.

## 9. Conclusão

O D4 organiza uma avaliação prática da usabilidade do AutoBotz Motors, concentrada nos fluxos de cliente, veículo e venda. A prioridade é reduzir dúvidas na navegação, erros de preenchimento e incertezas após operações. A confirmação das dificuldades e a avaliação dos ajustes dependem da execução dos testes com usuários e do registro das evidências correspondentes.
