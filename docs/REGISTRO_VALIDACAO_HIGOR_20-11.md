# Registro de validação — Higor (D4 / D15)

**Projeto:** AutoBotz Motors  
**Data de consolidação:** 20/11/2026  
**Responsável por esta consolidação:** Higor  
**Referência externa de testes do portal:** PR #59 — https://github.com/athr-ocean/autobotz-motors/pull/59

## 1. Escopo deste registro

Este documento consolida as evidências disponíveis para a parte de Higor. Ele separa:

1. verificações de interface do portal de treinamento executadas por Mauro e reportadas no PR #59;
2. os cenários de usabilidade operacional do ERP Swing atribuídos ao D4;
3. a revisão do conteúdo didático do D15 e as pendências do tutorial em vídeo.

**Limite importante:** os resultados do PR #59 dizem respeito ao portal de treinamento. Eles não comprovam a execução dos fluxos de Clientes, Veículos e Vendas no ERP Swing.

## 2. Evidência disponível — PR #59 (testes executados por Mauro)

| Campo | Registro |
|---|---|
| PR de evidências | https://github.com/athr-ocean/autobotz-motors/pull/59 |
| Commit testado | `d3e692b` |
| Sistema operacional | Debian |
| Navegador | Brave 1.93.136 |
| Aplicação | Portal de treinamento |
| Telemetria | Eventos em memória |
| Executor | Mauro — único executor informado |
| Orientação | Passo a passo com apoio de IA |
| Outros participantes | Nenhum |
| Capturas/relatório | Mauro informou que o PR contém dez capturas e um relatório; os anexos não foram inspecionados independentemente nesta consolidação |
| Tempo por tarefa | Não medido |
| Notas de satisfação | Não coletadas |

### 2.1 Resultados reportados

| ID | Verificação | Resultado informado | Observação |
|---|---|---|---|
| P59-01 | Responsividade, tema claro, 320 px | Aprovado nos casos conferidos; sem cortes | Verificação de interface do portal |
| P59-02 | Responsividade, tema claro, 375 px | Aprovado nos casos conferidos; sem cortes | Verificação de interface do portal |
| P59-03 | Responsividade, tema claro, 768 px | Aprovado nos casos conferidos; sem cortes | Verificação de interface do portal |
| P59-04 | Responsividade, tema claro, 1280 px | Aprovado nos casos conferidos; sem cortes | Verificação de interface do portal |
| P59-05 | Tema escuro, 375 px | Leitura e controles visíveis nos casos conferidos | Verificação de interface do portal |
| P59-06 | Tema escuro, 1280 px | Leitura e controles visíveis nos casos conferidos | Verificação de interface do portal |
| P59-07 | Navegação por teclado, foco, abrir guia e retornar à lista | Aprovado nos passos executados | Conforme relato do executor |
| P59-08 | Zoom de 200% no tema escuro | Texto e botão Voltar acessíveis | Confirmação manual expressa de Mauro |
| P59-09 | Dispositivo Android físico | Bloqueado | Notebook não detectou o dispositivo por USB; não classificar como falha do portal |

**Resumo de contagem:** oito verificações reportadas como aprovadas e uma bloqueada, dentro dos itens listados acima. Essa contagem não é taxa de sucesso de participantes nem métrica de usabilidade do ERP.

### 2.2 Limitações da evidência

- Mauro foi o único executor informado e recebeu orientação passo a passo de IA.
- Não houve participantes de pesquisa codificados.
- Não foram medidos tempos, intervenções de ajuda por participante ou notas de satisfação.
- Os eventos do portal estavam em memória; estes testes não demonstram persistência real da telemetria.
- O teste em Android físico ficou bloqueado por reconhecimento USB.
- Mauro validou apresentação e navegação do guia no portal, não a correspondência do texto com as telas do ERP Swing.

## 3. D4 — cenários operacionais do ERP Swing

Mauro esclareceu que não executou os cenários abaixo no ERP. Nesta consolidação, **não foi fornecido um registro próprio de execução de Higor** para esses casos. Por isso, não é correto marcá-los como aprovados, reprovados ou calcular indicadores.

| ID | Cenário | Estado do registro de Higor | Dados ainda necessários |
|---|---|---|---|
| D4-01 | Localizar cliente | Sem registro de execução fornecido | Versão/commit do Swing, participante(s), tempo, conclusão, erros, ajuda e evidência |
| D4-02 | Cadastrar cliente | Sem registro de execução fornecido | Versão/commit do Swing, dados fictícios usados, tempo, erros, mensagens e confirmação |
| D4-03 | Validar campos do cliente | Sem registro de execução fornecido | Campos testados, mensagem literal sem dados pessoais, resultado e possibilidade de correção |
| D4-05 | Cadastrar veículo | Sem registro de execução fornecido | Campos, eventual associação ao cliente, tempo, conclusão e evidência |
| D4-06 | Validar campos do veículo | Sem registro de execução fornecido | Campo inválido/ausente, mensagem literal, resultado e possibilidade de correção |
| D4-07 | Registrar venda | Sem registro de execução fornecido | Seleções realizadas, confirmação, consulta posterior, prevenção de duplicidade e evidência |

### 3.1 Critério para completar os registros

Para cada sessão real no ERP Swing, registrar:

- código do participante, sem nome ou dado pessoal;
- commit/versão do ERP, data, sistema operacional, modo e idioma;
- ID do caso e resultado: concluído sem ajuda, concluído com ajuda, não concluído ou bloqueado;
- tempo em segundos, somente se cronometrado;
- erros observados e texto da mensagem sem dados pessoais;
- quantidade e descrição das intervenções do avaliador;
- nota de facilidade/satisfação, somente se perguntada ao participante;
- comentário e evidência sem CPF, telefone, e-mail ou outros dados reais.

Quando a avaliação envolver usuários, não orientar o caminho esperado. Se houver somente teste técnico do desenvolvedor, identificá-lo como verificação funcional do executor, não como pesquisa com participantes.

## 4. D15 — conteúdo de treinamento

### 4.1 Guia rápido e FAQ

A revisão documental registrada anteriormente indica que o guia cobre os fluxos de Clientes, Veículos e Vendas e que o FAQ orienta a conferir a disponibilidade conforme versão/perfil, corrigir validações e evitar repetir operações sem confirmação. Os textos usam nomes funcionais de módulos e não fixam nomes específicos de botões sem confirmação.

**Resultado da revisão de telas:** a correspondência com os rótulos, campos, mensagens de validação, consultas e confirmações da versão atual do ERP Swing permanece pendente. Mauro confirmou que o PR #59 testou apresentação e navegação no portal, não a correspondência do conteúdo com as telas Swing.

### 4.2 Tutorial em vídeo

Existe roteiro textual, mas não foi fornecida evidência de vídeo gravado e publicado. Mauro informou que gravação, edição e publicação não estão sob sua responsabilidade.

**Pendência de coordenação:** Arthur deve definir:

1. responsável pela gravação e edição;
2. responsável pela revisão técnica contra a versão atual do ERP Swing;
3. plataforma/local de publicação;
4. prazo;
5. arquivo ou URL final como evidência.

Até a publicação comprovada, o conteúdo deve continuar identificado como **“roteiro — vídeo ainda não gravado”**. Não contabilizar visualizações de vídeo.

## 5. Indicadores e conclusões permitidos

| Indicador | Situação | Conclusão permitida |
|---|---|---|
| Verificações de interface do portal no PR #59 | 8 aprovadas e 1 bloqueada, conforme Mauro | Relatar os resultados por configuração, sem converter em taxa de satisfação ou usabilidade de usuários |
| Taxa de conclusão de participantes D4 | Sem dados | Não calcular |
| Tempo mediano por tarefa D4 | Sem tempos | Não calcular |
| Taxa de erro D4 | Sem registros de erro | Não calcular |
| Necessidade de ajuda D4 | Sem sessões documentadas | Não calcular |
| Satisfação média D4 | Sem notas | Não calcular |
| Persistência da telemetria do portal | Eventos em memória no ambiente informado | Não afirmar persistência real com base no PR #59 |

Não há problemas operacionais do ERP confirmados por estes dados. Os pontos de usabilidade listados nos documentos D4 continuam como itens de investigação até serem observados e registrados na interface Swing.

## 6. Pendências e responsáveis

| Ação | Responsável | Estado |
|---|---|---|
| Fornecer/realizar registros dos casos D4-01, 02, 03, 05, 06 e 07 no ERP Swing | Higor | Sem registro de execução fornecido nesta consolidação |
| Recrutar participantes, se for exigida avaliação com usuários, e coletar tempos/notas/ajuda sem induzir respostas | Higor | Pendente de sessão real |
| Conferir guia e FAQ diretamente nas telas Swing | Higor | Pendente |
| Atualizar `docs/` e `portal-treinamento/content/` no mesmo commit se forem encontradas divergências | Higor | Condicional à inspeção |
| Definir responsável, revisor técnico, canal e prazo do vídeo | Arthur | Pendente de decisão |
| Resolver reconhecimento USB ou disponibilizar outro ambiente para teste Android | Responsável técnico a definir | Bloqueado no PR #59 |
| Entregar registros de CI/regressão da responsabilidade de Adryan | Adryan | Fora deste registro; requer evidências próprias dele |

## 7. Conclusão para entrega

A parte de Higor está documentada quanto às evidências disponíveis e às lacunas. O PR #59 registra verificações manuais do portal executadas por Mauro; não substitui os testes operacionais do ERP Swing nem constitui avaliação de usabilidade com participantes. Para fechar integralmente o D4, ainda é necessário anexar registros reais dos seis cenários operacionais e, se solicitado, sessões com usuários. No D15, falta conferir guia/FAQ contra o Swing e definir/publicar o tutorial em vídeo.

## 8. Referências

- PR #59 — portal de treinamento: https://github.com/athr-ocean/autobotz-motors/pull/59
- Guia rápido: https://raw.githubusercontent.com/athr-ocean/autobotz-motors/main/portal-treinamento/content/guia-rapido.md
- FAQ: https://raw.githubusercontent.com/athr-ocean/autobotz-motors/main/portal-treinamento/content/faq.md
