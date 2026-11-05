# D15 — Estado dos materiais e rascunho de lançamento

**Atualizado:** 30/10/2026


**Estado geral:** o portal local passou nos testes HTTP/JDBC. Ainda não ocorreram lançamento público, gravação do tutorial de treinamento nem realização de webinar ou fórum.

## Situação dos materiais

- **Guia rápido:** existe como texto Markdown. Precisa de revisão contra os nomes e fluxos reais da versão integrada.
- **FAQ:** existe como texto Markdown. Deve continuar a separar recursos disponíveis de recursos planejados.
- **Roteiro de treinamento de 30 minutos:** texto previsto para demonstrar o ERP. Esse material é diferente da apresentação individual da disciplina, gravada separadamente.
- **Vídeo do portal:** o treinamento do D15 ainda não foi gravado nem publicado no portal.
- **Webinar:** não há evidência de sessão realizada ou gravação publicada. O roteiro não deve ser apresentado como webinar realizado.
- **Fórum:** não há evidência de uma funcionalidade de fórum implementada. O FAQ é o canal de consulta disponível neste momento; não anunciar fórum ativo.
- **Pesquisa de satisfação e análise de tickets:** os exemplos publicados não provêm de coleta real; ainda não há comparativo de satisfação ou redução de chamados.
- **Métricas do portal:** devem vir dos eventos persistidos e de um intervalo explícito. O endpoint administrativo depende de sessão web validada no servidor.
- **Publicação:** o portal está disponível somente no ambiente local (`127.0.0.1`).

## Modelo de comunicado — ainda não enviado

**Assunto:** AutoBotz Motors — materiais de treinamento disponíveis

Olá, equipe,

Estamos preparando os materiais de treinamento do AutoBotz Motors. O portal reúne um guia rápido e perguntas frequentes sobre clientes, veículos e vendas.

Antes de anunciar a abertura, a equipe confirmará a integração do portal com a versão aprovada do ERP, a gravação e revisão do vídeo de treinamento, a disponibilidade real dos conteúdos e os testes de acesso e persistência. Materiais em formato de roteiro serão identificados como roteiros, e exemplos ou pesquisas simuladas serão rotulados como simulação.

O comunicado será enviado somente após a definição da data, do endereço de acesso público e do canal de suporte.

Obrigado,
Equipe AutoBotz Motors

## Acompanhamento do lançamento

- [x] DataSource e serviço de treinamento integrados à `main`.
- [x] Portal, login/MFA e autorização 401/403/200 verificados no CI em banco descartável.
- [x] Entrada do portal pela navegação Swing e **aviso pop-up** após abertura da janela.
- [x] Roteiro curto de tutorial adicionado ao catálogo, identificado como **não gravado**.
- [x] Dashboard no navegador para consultar contagens por material/período após autenticação ADMIN.
- [ ] Ensaio visual do Swing e do dashboard no computador do responsável.
- [ ] Gravação, edição e revisão dos vídeos que serão entregues efetivamente.
- [ ] Webinar/fórum reais: a arquitetura informa **planejado**, sem simular serviço ativo.
- [ ] Aprovação e envio real do e-mail; link e destinatários externos não existem no protótipo local.
- [ ] Medição de redução real de chamados após treinamento (sem base real disponível).

O e-mail acima é uma proposta de campanha para simulação acadêmica, não um envio realizado.
