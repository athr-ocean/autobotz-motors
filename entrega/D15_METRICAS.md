# D15 — Modelo de eventos, métricas e validação

## 1. Objetivo

Registrar eventos agregáveis de uso do portal de treinamento e consultar contagens por material e período, para apoiar a avaliação de acesso aos conteúdos.

## 2. Eventos

- `PORTAL_OPEN`: abertura do portal; não exige material.
- `MATERIAL_VIEW`: abertura de um material publicado; requer `material_slug`.
- `VIDEO_VIEW`: abertura de uma página/vídeo publicado; requer `material_slug`.

O evento registra somente o necessário para a contagem: tipo, slug do material e timestamp gerado no banco. A implementação proposta não grava nome, CPF, e-mail, senha, token ou endereço IP.

## 3. Métricas

O relatório deve filtrar por um intervalo temporal, usando `inicio` inclusivo e `fim` exclusivo. Deve agrupar por tipo de evento e slug do material. Isso permite mostrar, por exemplo, quantas vezes o guia foi aberto durante uma semana.

**Limite de interpretação:** acesso/visualização não comprova que a pessoa terminou ou aprendeu o conteúdo. Não deduplicar pessoas se não existe uma forma autorizada e claramente definida de identificação.

## 4. Dados reais versus simulados

- **Dados reais de execução local:** eventos gravados pelo sistema enquanto o portal é utilizado de fato.
- **Dados simulados:** conjunto didático criado para testar gráficos ou exemplos; deve ser claramente rotulado `SIMULADO` e armazenado separadamente da telemetria real.
- **Comparação pré/pós:** só apresentar quando houver período comparável, linha de base confiável e fonte dos chamados/satisfação documentada. Sem isso, declarar que não foi possível concluir o efeito do treinamento.

Exemplo **SIMULADO**, apenas para ilustrar como um relatório pode ser descrito (não é resultado medido):

| Evento | Material | Total simulado |
|---|---|---:|
| `PORTAL_OPEN` | — | 20 |
| `MATERIAL_VIEW` | `guia-rapido` | 12 |
| `VIDEO_VIEW` | `video-cadastro` | 8 |

Esses valores são exemplos inventados para demonstração de formato. Não devem ser misturados às métricas obtidas durante a execução real.

## 5. Testes de aceitação sugeridos

1. Um evento válido é persistido e recebe ID.
2. `PORTAL_OPEN` é aceito sem slug.
3. `MATERIAL_VIEW` e `VIDEO_VIEW` sem slug são rejeitados antes da persistência.
4. Tipo desconhecido, slug inválido, intervalo nulo e intervalo com fim anterior ao início são rejeitados.
5. O relatório conta somente eventos dentro de `[inicio, fim)`.
6. O resultado é agrupado por tipo e material, inclusive eventos sem material.
7. O endpoint/painel de métricas exige permissão administrativa.
8. Os testes de gravação usam banco descartável com nome terminado em `_test` e não tocam em `autobotz_db`.
9. A saída de exemplo simulada está rotulada e não é apresentada como telemetria real.

## 6. Operação

A tabela D15 é aditiva. Sua migração deve ser aplicada somente depois de revisão e em banco de teste. Arthur consolida a migração no schema final; a migração específica não deve editar `schema.sql` em paralelo.
