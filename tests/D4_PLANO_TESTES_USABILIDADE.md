# D4 — Plano de Testes de Usabilidade da Interface

**Responsável:** Higor  
**Tipo:** roteiro de teste manual exploratório  
**Estado inicial:** planejado; não executado nesta entrega.

## 1. Preparação

- Executar a versão da aplicação que será avaliada e registrar o commit/versão.
- Utilizar exclusivamente banco de teste isolado; não utilizar dados reais de clientes.
- Criar dados fictícios e identificáveis como teste.
- Registrar o idioma selecionado e o modo de execução (Swing ou outro modo efetivamente usado).
- Preparar uma folha de observação por participante.
- Não orientar o participante durante a tarefa, exceto para esclarecer o enunciado. Registrar toda ajuda prestada.

## 2. Dados de teste sugeridos

Utilizar nomes e informações fictícias. Não reutilizar CPF, telefone, e-mail ou outros dados pessoais reais. Os valores exatos devem respeitar as validações implementadas na versão testada.

## 3. Casos de teste

| ID | Fluxo | Instrução ao participante | Resultado esperado | Evidência a registrar |
|---|---|---|---|---|
| D4-01 | Localizar cliente | “Localize o cliente de teste informado.” | O usuário encontra o registro correto sem confundir resultados | Tempo, tentativas, erros e conclusão |
| D4-02 | Cadastrar cliente | “Cadastre um novo cliente usando os dados fictícios fornecidos.” | Formulário compreensível; dados aceitos; confirmação exibida; registro localizável | Campos que geraram dúvida, erros, confirmação e consulta posterior |
| D4-03 | Validação de cliente | “Tente salvar o formulário sem preencher os dados exigidos.” | A interface impede o salvamento inválido e informa o que precisa ser corrigido | Texto da mensagem, clareza e possibilidade de correção |
| D4-04 | Pesquisa após cadastro | “Encontre o cliente que acabou de cadastrar.” | O registro aparece na pesquisa/listagem correspondente | Tempo e facilidade para localizar |
| D4-05 | Cadastrar veículo | “Cadastre o veículo de teste e associe-o ao cliente indicado, se a tela oferecer essa opção.” | Veículo salvo com dados corretos e associação esperada | Passos, erros e confirmação |
| D4-06 | Validação de veículo | “Tente salvar o veículo com um dado obrigatório ausente ou inválido.” | A interface indica o dado que precisa ser corrigido | Mensagem e reação do participante |
| D4-07 | Registrar venda | “Registre uma venda de teste selecionando o cliente e o veículo fornecidos.” | A venda é concluída uma única vez e fica consultável | Seleções, erros, confirmação e consulta posterior |
| D4-08 | Prevenir repetição | “Após acionar salvar, observe o resultado e não repita a ação sem necessidade.” | A interface comunica o processamento e não gera duas vendas para uma única operação | Quantidade de registros gerados e feedback visual |
| D4-09 | Cancelar/voltar | “Inicie um cadastro e depois escolha voltar ou cancelar.” | A interface informa o efeito do cancelamento e não descarta dados silenciosamente | Mensagem, perda ou preservação de dados e compreensão |
| D4-10 | Consistência de idioma | “Repita uma tarefa simples nos idiomas oferecidos pela versão.” | Menus, campos, validações e confirmações mantêm o idioma escolhido | Textos inconsistentes ou não traduzidos |
| D4-11 | Ordem de serviço (opcional) | “Localize ou consulte uma ordem de serviço de teste.” | O fluxo pode ser concluído sem ambiguidade, caso esteja disponível na versão | Disponibilidade, passos e dificuldades |

## 4. Ficha de registro por tarefa

Copiar e preencher uma ficha para cada participante e caso executado:

| Campo | Registro |
|---|---|
| Participante (código, sem nome) | P___ |
| Caso de teste | D4-___ |
| Versão/commit | ___ |
| Data e idioma | ___ |
| Tempo decorrido | ___ segundos |
| Conclusão | Sem ajuda / Com ajuda / Não concluída |
| Erros observados | ___ |
| Quantidade de intervenções | ___ |
| Nota de satisfação (1 a 5) | ___ |
| Comentários/observações | ___ |
| Evidência (captura sem dados pessoais/log) | ___ |

## 5. Perguntas após a tarefa

1. Foi fácil entender por onde começar? (1 = muito difícil; 5 = muito fácil)
2. Os nomes dos menus e botões indicavam claramente o que aconteceria? (1–5)
3. Os campos e formatos esperados estavam claros? (1–5)
4. As mensagens de erro ajudaram a corrigir o problema? (1–5; marcar “não se aplica” se não houve erro)
5. Ficou claro quando a operação foi concluída? (1–5)
6. Qual foi a maior dificuldade encontrada? (resposta aberta)
7. O que deveria ser melhorado primeiro? (resposta aberta)

## 6. Consolidação dos resultados

Após executar os testes, preencher a tabela abaixo. Não inserir números estimados ou inventados.

| Caso | Participantes | Concluídas sem ajuda | Tempo mediano | Participantes com erro | Satisfação média | Problemas confirmados |
|---|---:|---:|---:|---:|---:|---|
| D4-01 | — | — | — | — | — | Pendente de execução |
| D4-02 | — | — | — | — | — | Pendente de execução |
| D4-03 | — | — | — | — | — | Pendente de execução |
| D4-05 | — | — | — | — | — | Pendente de execução |
| D4-07 | — | — | — | — | — | Pendente de execução |

## 7. Registro final do teste

- **Comando/procedimento executado:** preencher com o procedimento real usado para iniciar a aplicação e acessar os fluxos.
- **Ambiente:** preencher sistema operacional, versão/commit, banco de teste e idioma.
- **Resultado:** preencher somente após executar. Indicar casos aprovados, reprovados e bloqueados.
- **Limitações:** informar quantidade e perfil dos participantes, telas não disponíveis e problemas de ambiente.

**Nota:** a execução deste roteiro não foi realizada na preparação do documento. O estado atual é “planejado”, não “aprovado”.
