# D14 — Sessão de servidor para o portal (contrato para Mauro)

Classe: `autobotz.service.SessaoWebService` · Testes: `tests/D14SessaoWebTest.java`

O portal HTTP **não** usa `SessaoUsuario` (singleton do desktop) como identidade. A identidade de cada
requisição é um token opaco emitido pelo servidor depois de senha (+ MFA para ADMIN) e invalidado no logout.

## Regra de acesso a `/api/treinamentos/metricas`

| Situação | Resposta |
|---|---|
| Sem token, token inválido, expirado ou de logout | **401** |
| Sessão válida, perfil sem `CONSULTAR_METRICAS_TREINAMENTO` (VENDEDOR) | **403** (gera `ACESSO_NEGADO` na trilha) |
| Sessão válida de ADMIN (que passou por senha **e** MFA) | **200** |

Enquanto o handler não chamar `autorizarMetricas`, o endpoint deve continuar negado.

## Como o handler usa

```java
// UMA instância para o processo inteiro (as sessões ficam na memória dela).
SessaoWebService sessoes = new SessaoWebService();

// handler de /api/treinamentos/metricas
String token = SessaoWebService.extrairToken(
        ex.getRequestHeaders().getFirst("Authorization"),   // "Bearer <token>"
        ex.getRequestHeaders().getFirst("Cookie"));          // ABM_SESSAO=<token>
var decisao = sessoes.autorizarMetricas(token);              // lança SQLException -> responder 500
if (!decisao.permitido()) {
    responder(ex, decisao.httpStatus(), "{\"erro\":{\"codigo\":\"ACESSO\",\"mensagem\":\"Acesso negado.\"}}");
    return;
}
// somente aqui: treinamentoService.consultarMetricas(inicio, fim)
```

## Rotas de login/logout (o servidor HTTP é do Mauro)

| Rota sugerida | Chama | Resposta |
|---|---|---|
| `POST /api/login` `{usuario, senha}` | `iniciarLogin` | `AUTENTICADO` → 200 + token · `MFA_NECESSARIA` → 202 + `desafioId`, `expiraEm` · `BLOQUEADO` → 429 + `Retry-After: segundosRestantes` · `NEGADO` → 401 (mensagem genérica) |
| `POST /api/login/mfa` `{desafioId, codigo}` | `concluirMfa` | sucesso → 200 + token · `mfaStatus=INVALIDO` → 401 (pode tentar de novo) · `EXPIRADO`/`ESGOTADO`/`JA_UTILIZADO`/`INEXISTENTE` → 401 e refazer o passo 1 |
| `POST /api/logout` | `logout(token)` | 204 (idempotente) |

- O token deve ir num cookie `ABM_SESSAO` com `HttpOnly; SameSite=Strict; Path=/` (e `Secure` quando houver HTTPS) ou em `Authorization: Bearer`.
- O MFA continua **simulado**: se o servidor devolver `desafio.codigoSimulado()` ao navegador, deve rotulá-lo como DEMONSTRAÇÃO.
- Nunca registrar token, senha nem código MFA em log; `LoginWeb.toString()` já mascara o token.

## O que a classe garante (todos testados)

- Token de 256 bits (`SecureRandom`), só o hash SHA-256 fica em memória.
- Só `AuthService` abre sessão: ADMIN e VENDEDOR sem MFA válido nunca recebem token; o bloqueio de 5 falhas/10 min vale também na web.
- Expira após 30 min ociosa ou 8 h no total (mesmo com uso). Logout invalida no servidor.
- O perfil é relido do banco a cada requisição: ADMIN rebaixado passa a receber 403 e usuário removido passa a receber 401.
- Login/logout web não tocam `SessaoUsuario`; um ADMIN logado no Swing **não** autoriza requisições HTTP.

## Limites

- Nesta árvore fornecida existe o serviço de sessão e seu contrato de integração, mas não foi encontrada uma implementação de produção dos handlers HTTP em `src/`. A autorização está testada pelo serviço e por um servidor HTTP criado dentro do teste; a integração da rota real precisa ser verificada com o código do responsável pelo portal antes de declarar o portal protegido de ponta a ponta.
- Sessões vivem na memória do processo: reiniciar o servidor encerra todas (é intencional nesta prova de conceito).
- Não há HTTPS, limite por IP, "lembrar-me" nem renovação de token.
- Proteção CSRF além de `SameSite=Strict` (ex.: checar `Origin` nas rotas `POST`) fica a cargo de quem implementa as rotas.

## MFA para dados de clientes e CRM

A política atual exige o segundo fator para os perfis `ADMIN` e `VENDEDOR`. O vendedor pode consultar/cadastrar clientes e acessar CRM; portanto, senha correta, isoladamente, devolve `MFA_NECESSARIA`, não uma sessão/token. A camada web continua a revalidar o token e a permissão do perfil no servidor em cada requisição. `LISTAR_CLIENTES`, `CADASTRAR_CLIENTE` e `ACESSAR_CRM` somente retornam 200 depois que o MFA for concluído e desde que a matriz permita a ação.

O segundo fator é **SIMULADO**: a interface de demonstração exibe o código rotulado como demonstração. Não há entrega real por SMS ou e-mail.
