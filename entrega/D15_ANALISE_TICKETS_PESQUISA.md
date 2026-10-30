# D15 — Análise didática de tickets e pesquisa de satisfação

**AutoBotz Motors — Equipe B · 29/10/2026.** Documento específico do Produto Final da segunda rodada.

## Origem e limites

O desafio solicita a análise de tickets de três meses por categoria e funcionalidade, além de pesquisa **simulada**. O ERP desenvolvido não recebeu uma base histórica verificada de tickets de suporte. Portanto, todos os números abaixo são **DADOS FICTÍCIOS**, usados para demonstrar como a análise direcionaria os materiais de capacitação. Não foram coletados de clientes, nem criados por uso real do portal.

## Tickets por categoria — cenário SIMULADO

| Categoria ilustrativa | Julho/2026 | Agosto/2026 | Setembro/2026 | Total |
|---|---:|---:|---:|---:|
| Login e autenticação | 5 | 4 | 5 | 14 |
| Cadastro de clientes | 4 | 4 | 4 | 12 |
| Compras e aprovações | 3 | 3 | 4 | 10 |
| Consultas e relatórios | 3 | 3 | 3 | 9 |
| **Total fictício** | **15** | **14** | **16** | **45** |

Pela distribuição inventada, o primeiro material a priorizar seria um guia de acesso, perfis e MFA, seguido de cadastro de clientes e fluxo de compra. **Essa prioridade é uma hipótese pedagógica**, não uma necessidade diagnosticada em usuários reais. O guia existente `entrega/D15_GUIA_RAPIDO.md` explica clientes, veículos e vendas; a documentação `entrega/D14_LOGIN_MFA.md` cobre os detalhes de autenticação. O vídeo tutorial de três minutos está somente em formato de roteiro, em `entrega/D15_ROTEIRO_VIDEO.md`.

### Como distinguir o tipo de dificuldade

- **Erro de uso:** o usuário não conhece uma sequência de cliques ou o significado de um campo; corrigir com guia passo a passo.
- **Usabilidade:** o formulário está pouco claro para o público-alvo; registrar a hipótese para teste supervisionado.
- **Defeito de software:** o procedimento correto falha ou produz resultado incorreto; abrir issue reproduzível e não tentar resolvê-lo apenas com treinamento.

Nenhuma dessas classificações foi confirmada por entrevistas reais nesta rodada.

## Questionário de satisfação — exercício SIMULADO

Perguntas previstas para uma eventual pesquisa autorizada:

1. “Qual sua maior dificuldade ao utilizar o ERP?”
2. “A documentação atual é útil para concluir suas tarefas?”
3. “Você gostaria de treinamentos em vídeo curtos?”
4. “Qual tarefa você gostaria de ver explicada passo a passo?”
5. “Em qual módulo encontrou mais dúvidas no último mês?”

**Exemplo de 20 respostas totalmente inventadas**, apenas para praticar interpretação:

| Pergunta | Respostas simuladas | Interpretação possível |
|---|---|---|
| Maior dificuldade | login 8; clientes 6; compras 4; relatórios 2 | guia de login seria prioridade |
| Documentação útil? | sim 7; parcialmente 8; não 5 | revisar clareza e cobertura |
| Quer vídeos curtos? | sim 16; não 4 | vídeo de três minutos merece protótipo |

O instrumento **não foi disparado**, e essas respostas **não são depoimentos de usuários reais**. Para uso real, seriam necessários consentimento, público, método de coleta e guarda adequada das respostas.

## Conteúdo e portal implementados

- Portal HTTP local com catálogo de materiais publicados, guia rápido e FAQ.
- Roteiro de tutorial curto em `entrega/D15_ROTEIRO_VIDEO.md` e materiais em `portal-treinamento/content/`.
- `TreinamentoService` registra eventos `PORTAL_OPEN`, `MATERIAL_VIEW` e `VIDEO_VIEW` em MariaDB, sem identificação pessoal.
- Métricas por período e material; consulta administrativa protegida por sessão e perfil. Testes `D15PersistenciaTest` e `D15AuthHttpJdbcTest` aprovados em banco descartável, incluindo 401/403/200.
- Materiais de lançamento preparados em `entrega/D15_LANCAMENTO_E_MATERIAIS.md`; **nenhum e-mail em massa foi enviado**.

O portal roda em `127.0.0.1`; não é um serviço público hospedado. Não há evidência de webinar gravado ou fórum interativo implementado. Um FAQ existente **não é** um fórum.

## Dashboard e avaliação contínua

O painel permite observar aberturas e visualizações realmente registradas pelo portal no banco utilizado. **Visualização não equivale a aprendizagem** e não demonstra, por si só, redução de chamados.

Para comparar a taxa de tickets antes/depois de um lançamento real:

1. Fixar a mesma categoria e uma janela temporal equivalente (por exemplo 30 dias).
2. Normalizar por número de usuários ativos ou clientes atendidos.
3. Registrar as condições comparadas e eventuais mudanças simultâneas de produto/suporte.
4. Calcular `taxa = tickets / usuários ativos` e `variação % = (taxa_depois / taxa_antes − 1) × 100`.
5. Documentar sazonalidade e ausência de causalidade comprovada; não usar os 45 tickets fictícios como linha de base.

**Conclusão:** o sistema tem infraestrutura técnica para acompanhar acesso ao treinamento, mas ainda não há série histórica real de tickets ou pesquisa que permita afirmar aumento de satisfação ou redução de suporte.

## Implementação adicional da interface do dashboard

O portal disponibiliza formulário de login/MFA demonstrativo e um painel visual de acesso restrito que consulta `GET /api/treinamentos/metricas` com datas inicial/final. O backend valida a sessão e o perfil: **401** sem autenticação, **403** para usuário sem privilégio e **200** apenas para ADMIN autenticado. A página agrega os dados retornados por tipo de evento e material; não interpreta visualizações como conclusão de treinamento. O roteiro de tutorial de três minutos está publicado no catálogo **como texto não gravado**, sem contagem artificial de `VIDEO_VIEW`. Seções de webinar e fórum aparecem como propostas em preparação.
