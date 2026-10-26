# D15 — Contrato de integração proposto entre portal e métricas

**Status: PROPOSTA PARA ACORDO COM MAURO.** Não representa rotas já implementadas. Congelar nomes e formatos em conjunto antes da integração.

## 1. Responsabilidades

- **Higor:** conteúdos didáticos, modelo de evento, persistência/consulta de métricas e documentação.
- **Mauro:** interface do portal e servidor/rotas que servirão os materiais e chamarão o serviço de eventos.
- **Arthur:** integração da navegação principal, migrações finais e compatibilidade geral conforme o plano de equipe.

## 2. Eventos reconhecidos

| Evento | Quando registrar | `materialSlug` |
|---|---|---|
| `PORTAL_OPEN` | Quando a página inicial do portal for aberta | nulo |
| `MATERIAL_VIEW` | Quando um material de texto for realmente aberto | slug obrigatório |
| `VIDEO_VIEW` | Quando uma página/vídeo de treinamento for aberta | slug obrigatório |

O evento deve ser registrado uma vez por abertura real, evitando chamadas repetidas automáticas. O horário é gerado no servidor/banco, e não aceito do navegador.

## 3. Endpoints sugeridos

### `GET /api/treinamentos/materiais`

Retorna o catálogo dos materiais publicados. Não retornar conteúdo marcado como futuro como se já estivesse disponível.

Resposta proposta:

```json
{
  "materiais": [
    {"slug":"guia-rapido","titulo":"Guia rápido do AutoBotz Motors","tipo":"GUIA","status":"PUBLICADO"},
    {"slug":"faq","titulo":"Perguntas frequentes","tipo":"FAQ","status":"PUBLICADO"}
  ]
}
```

### `POST /api/treinamentos/eventos`

Registra um evento de acesso/visualização.

Requisição proposta:

```json
{"tipoEvento":"MATERIAL_VIEW","materialSlug":"guia-rapido"}
```

Resposta proposta: `201 Created` com um identificador simples do evento; nunca devolver credenciais ou dados internos desnecessários.

Validações: somente os três tipos de evento reconhecidos; slug com formato permitido; tamanho de campos limitado; timestamp gerado no servidor. Não coletar CPF, senha, token, IP inventado ou texto livre desnecessário.

### `GET /api/treinamentos/metricas?inicio=2026-10-01T00:00:00&fim=2026-11-01T00:00:00`

Retorna contagens agregadas por tipo e material no intervalo `[inicio, fim)`, com `fim` exclusivo.

Resposta proposta:

```json
{
  "inicio":"2026-10-01T00:00:00",
  "fim":"2026-11-01T00:00:00",
  "metricas":[
    {"tipoEvento":"MATERIAL_VIEW","materialSlug":"guia-rapido","total":12}
  ],
  "origem":"DADOS_REAIS_DE_EXECUCAO"
}
```

Esse endpoint deve exigir permissão administrativa no servidor. Ocultar o botão no portal não substitui a autorização no endpoint/serviço.

## 4. Formato de erro proposto

```json
{"erro":{"codigo":"VALIDACAO","mensagem":"O evento informado não é válido."}}
```

Não devolver stack traces, SQL, segredos ou dados pessoais em mensagens para o navegador.

## 5. Decisões pendentes antes da implementação conjunta

- Confirmar caminhos e método HTTP com Mauro.
- Confirmar o formato de resposta e de erro.
- Confirmar como o servidor obtém o `DataSource` ou conexão do projeto.
- Confirmar a autorização do painel de métricas.
- Decidir como o catálogo de materiais é publicado/versionado.

Até essas decisões serem aprovadas, os endpoints acima são apenas um contrato proposto e não devem ser anunciados como disponíveis.
