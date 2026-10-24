# D15 — Contrato do portal de treinamento (rascunho v0.2)

Donos: Mauro (servidor HTTP e interface) e Higor (conteúdo, TreinamentoService e métricas).
Status: aguardando revisão do Higor. Nada abaixo é definitivo até ele concordar.

## Divisão de responsabilidades

- Higor: `TreinamentoService`, com operações para listar o catálogo, buscar um material,
  registrar um evento e consultar métricas agregadas, mais o conteúdo dos materiais.
- Mauro: servidor HTTP e interface web. O servidor não acessa o banco diretamente.
  Ele conversa com a interface `autobotz.portal.PortalBackend`.
- Ponto de integração: hoje `MockPortalBackend` responde com dados de exemplo (simulados).
  Quando o serviço do Higor existir, basta uma classe que implemente `PortalBackend`
  chamando o `TreinamentoService`, e trocar a linha no `PortalLauncher`.

## Rotas (JSON em UTF-8, porta padrão 8085, somente localhost)

| Método | Rota                 | Descrição                                      |
|--------|----------------------|------------------------------------------------|
| GET    | /api/catalogo        | Lista os materiais. Filtro opcional: ?tipo=    |
| GET    | /api/catalogo/{id}   | Retorna um material                            |
| POST   | /api/eventos         | Registra um evento de acesso ou visualização   |
| GET    | /api/metricas        | Métricas agregadas (hoje responde 403, ver pendências) |

## Material

    { "id": "guia-venda", "tipo": "GUIA", "titulo": "...", "descricao": "...",
      "arquivo": "", "simulado": true }

- tipo: GUIA, VIDEO, WEBINAR, FAQ ou FORUM.
- simulado: true quando o material é de exemplo, não conteúdo oficial.

## Evento (POST /api/eventos)

Entrada:

    { "tipoEvento": "MATERIAL_VIEW", "materialId": "guia-venda", "sessaoId": "uuid" }

- tipoEvento: PORTAL_OPEN, MATERIAL_VIEW ou VIDEO_VIEW.
- materialId: obrigatório em MATERIAL_VIEW e VIDEO_VIEW; deve ser nulo em PORTAL_OPEN.
- sessaoId: identificador anônimo gerado no navegador. Sem nome, CPF ou IP.

Resposta 201:

    { "id": 123, "registradoEm": "2026-10-27T14:30:00" }

## Métricas (GET /api/metricas)

Formato proposto para quando a permissão estiver definida:

    { "periodo": { "de": "AAAA-MM-DD", "ate": "AAAA-MM-DD" },
      "totalAcessos": 10,
      "porMaterial": [ { "materialId": "guia-venda", "visualizacoes": 4 } ],
      "origem": "MEDIDO" }

Dados simulados (chamados, satisfação) devem vir separados e rotulados SIMULADO.

## Erros

Formato: { "erro": { "codigo": "...", "mensagem": "..." } }

| HTTP | codigo                  | Quando                                   |
|------|-------------------------|------------------------------------------|
| 400  | EVENTO_INVALIDO         | campo faltando, tipo inválido ou materialId inexistente |
| 403  | ACESSO_NEGADO           | métricas sem permissão                   |
| 404  | MATERIAL_NAO_ENCONTRADO | id inexistente                           |
| 404  | ROTA_NAO_ENCONTRADA     | rota desconhecida                        |
| 500  | ERRO_INTERNO            | falha interna, sem detalhes técnicos     |

## Pendências (decidir antes de fechar o contrato)

1. Permissão das métricas: como o portal sabe que quem consulta é ADMIN, já que o login
   está no Swing? (Arthur e João)
2. Nome e porta do modo `portal` no scripts/run.sh. (Arthur revisa)
3. Catálogo em arquivo JSON ou no banco? (Higor)
4. Os nomes dos campos do evento servem para o modelo EventoTreinamento do Higor?
