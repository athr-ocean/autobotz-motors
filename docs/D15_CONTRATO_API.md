# D15 — Contrato de integração entre portal e métricas

**Status: proposta atualizada para validação conjunta com Mauro, Arthur e João.** Este documento define o formato esperado; não afirma que as rotas já estejam implementadas ou publicadas.

## 1. Responsabilidades

- **Higor:** conteúdos didáticos, modelo de evento, persistência/consulta de métricas e documentação.
- **Mauro:** interface do portal e servidor/rotas que servirão os materiais e chamarão o serviço de eventos.
- **Arthur:** integração da navegação principal, migrações finais e compatibilidade geral conforme o plano da equipe.
- **Arthur e João:** alinhamento da sessão e das permissões administrativas usadas pelo portal.

## 2. Eventos reconhecidos

| Evento | Quando registrar | `materialSlug` |
|---|---|---|
| `PORTAL_OPEN` | Quando a página inicial do portal for aberta | nulo |
| `MATERIAL_VIEW` | Quando um material de texto for realmente aberto | slug obrigatório |
| `VIDEO_VIEW` | Quando uma página/vídeo de treinamento for aberta | slug obrigatório |

O evento deve ser registrado uma vez por abertura real, evitando chamadas repetidas automáticas. O horário é gerado no servidor/banco, e não aceito do navegador.

## 3. Endpoints

### 3.1 `GET /api/treinamentos/materiais`

Retorna somente o catálogo dos materiais publicados. Conteúdos em preparação, futuros ou não publicados não devem aparecer como disponíveis.

Resposta `200 OK` proposta:

```json
{
  "materiais": [
    {
      "slug": "guia-rapido",
      "titulo": "Guia rápido do AutoBotz Motors",
      "tipo": "GUIA",
      "status": "PUBLICADO"
    },
    {
      "slug": "faq",
      "titulo": "Perguntas frequentes",
      "tipo": "FAQ",
      "status": "PUBLICADO"
    }
  ]
}
```

O catálogo pode ser mantido em um `catalogo.json` versionado. O servidor usa o slug para localizar o recurso cadastrado no catálogo; não deve expor ao cliente um caminho interno de arquivo.

### 3.2 `GET /api/treinamentos/materiais/{slug}`

Retorna o conteúdo de um material individual, desde que o slug exista no catálogo do servidor e o material esteja publicado.

**Regras obrigatórias:**

- Aceitar somente slugs cadastrados no catálogo e com status `PUBLICADO`.
- Usar uma lista/mapa de materiais mantida no servidor para resolver o conteúdo.
- Nunca aceitar um caminho de arquivo enviado pelo navegador (`path`, `file`, `filename` ou equivalente).
- Validar o slug como identificador, por exemplo: letras minúsculas sem acentos, números e hífens; rejeitar separadores de diretório, `..`, barras e valores inválidos.
- Não concatenar o slug recebido diretamente a um caminho de arquivo. O caminho interno deve vir dos metadados confiáveis do catálogo, e a resolução final deve permanecer dentro da pasta autorizada de conteúdo.
- Não permitir que materiais não publicados sejam recuperados por chamada direta, mesmo que o usuário conheça o slug.

Resposta `200 OK` proposta para um guia em Markdown:

```json
{
  "slug": "guia-rapido",
  "titulo": "Guia rápido do AutoBotz Motors",
  "tipo": "GUIA",
  "status": "PUBLICADO",
  "formato": "MARKDOWN",
  "conteudo": "# Guia rápido\n\nEste é o conteúdo do material..."
}
```

`conteudo` contém o conteúdo textual do material. `formato` informa como a interface deve apresentá-lo. O campo de caminho físico do arquivo não faz parte da resposta.

Se o slug não existir, ou o material existir mas não estiver publicado, responder `404 Not Found` com o formato de erro definido na seção 4. Não revelar se um material em preparação existe.

### 3.3 `POST /api/treinamentos/eventos`

Registra um evento de acesso/visualização.

Requisição `application/json` proposta:

```json
{
  "tipoEvento": "MATERIAL_VIEW",
  "materialSlug": "guia-rapido"
}
```

Resposta proposta: `201 Created` com identificador simples do evento; nunca devolver credenciais ou dados internos desnecessários.

Validações: somente os três tipos de evento reconhecidos; slug com formato permitido e publicado quando obrigatório; tamanho de campos limitado; timestamp gerado no servidor. Não coletar CPF, senha, token, IP inventado ou texto livre desnecessário.

### 3.4 `GET /api/treinamentos/metricas?inicio=2026-10-01T00:00:00&fim=2026-11-01T00:00:00`

Retorna contagens agregadas por tipo e material no intervalo `[inicio, fim)`, com `fim` exclusivo.

Resposta proposta:

```json
{
  "inicio": "2026-10-01T00:00:00",
  "fim": "2026-11-01T00:00:00",
  "metricas": [
    {"tipoEvento": "MATERIAL_VIEW", "materialSlug": "guia-rapido", "total": 12}
  ],
  "origem": "DADOS_REAIS_DE_EXECUCAO"
}
```

Esse endpoint deve exigir permissão administrativa no servidor. Ocultar o botão no portal não substitui a autorização no endpoint/serviço.

## 4. Formato de erro

Todas as rotas devem responder erros de API em JSON, sem stack traces ou detalhes internos.

Exemplo para entrada inválida:

```json
{
  "erro": {
    "codigo": "VALIDACAO",
    "mensagem": "O parâmetro informado não é válido."
  }
}
```

Códigos HTTP propostos:

- `400 Bad Request`: formato inválido de slug, consulta ou evento.
- `401 Unauthorized`: não existe sessão autenticada válida quando ela é exigida.
- `403 Forbidden`: sessão válida sem permissão administrativa para consultar métricas.
- `404 Not Found`: material desconhecido ou não publicado, sem revelar conteúdo privado.
- `500 Internal Server Error`: falha inesperada, com mensagem genérica ao cliente e detalhes somente no log seguro.

Não devolver stack traces, SQL, segredos ou dados pessoais em mensagens para o navegador.

## 5. Decisões restantes antes da implementação conjunta

- Confirmar o catálogo versionado e o mapeamento seguro de slug para arquivo com Mauro.
- Confirmar o formato final das respostas e dos erros.
- Confirmar com Arthur a maneira correta de fornecer conexões próprias por operação, evitando fechar uma `Connection` compartilhada.
- Confirmar com Arthur e João como a sessão autenticada e a permissão administrativa serão reconhecidas pelo portal.
- Definir os testes de integração no banco isolado antes do merge.

Até as rotas serem implementadas e testadas no ambiente integrado, elas são um contrato acordado/proposto e não devem ser anunciadas como disponíveis.
