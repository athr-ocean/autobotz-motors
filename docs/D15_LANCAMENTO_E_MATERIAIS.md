# D15 — Estado dos materiais e rascunho de lançamento

**Atualizado:** 30/10/2026


**Estado geral:** protótipo local de treinamento integrado e validado por testes HTTP/JDBC; campanha e liberação pública não realizadas. A gravação de vídeo e qualquer webinar/fórum operacional permanecem ausentes.

## Inventário honesto

- **Guia rápido:** existe como texto Markdown. Precisa de revisão contra os nomes e fluxos reais da versão integrada.
- **FAQ:** existe como texto Markdown. Deve continuar a separar recursos disponíveis de recursos planeados.
- **Roteiro de vídeo de 30 minutos:** existe como texto. A gravação, edição, ensaio cronometrado e validação da reprodução continuam pendentes.
- **Vídeo gravado:** não há evidência de arquivo de vídeo final nesta entrega.
- **Webinar:** não há evidência de sessão realizada ou gravação publicada. O roteiro não deve ser apresentado como webinar realizado.
- **Fórum:** não há evidência de uma funcionalidade de fórum implementada. O FAQ é o canal de consulta disponível neste momento; não anunciar fórum ativo.
- **Pesquisa de satisfação e análise de tickets:** só podem ser apresentadas como simuladas se não existirem dados reais autorizados. Não afirmar redução de chamados, aumento de satisfação ou eficácia sem comparação válida.
- **Métricas do portal:** devem vir dos eventos persistidos e de um intervalo explícito. O endpoint administrativo depende de sessão web validada no servidor.
- **Publicação:** o portal é local (127.0.0.1) nesta etapa. Não afirmar que está disponível na internet.

## Rascunho de e-mail de lançamento — NÃO ENVIAR ANTES DA APROVAÇÃO

**Assunto:** AutoBotz Motors — materiais de treinamento disponíveis

Olá, equipa,

Estamos a preparar a disponibilização dos materiais de treinamento do AutoBotz Motors. O portal deverá reunir o guia rápido e as perguntas frequentes para apoiar a consulta dos procedimentos de clientes, veículos e vendas.

Antes de anunciar a abertura, a equipa confirmará a integração do portal com a versão aprovada do ERP, a gravação e revisão do vídeo de treinamento, a disponibilidade real dos conteúdos e os testes de acesso e persistência. Materiais em formato de roteiro serão identificados como roteiros, e exemplos ou pesquisas simuladas serão rotulados como simulação.

Por enquanto, este texto é apenas um rascunho de comunicação. A data, o endereço de acesso, o público destinatário e o canal de suporte deverão ser preenchidos e aprovados após a validação final. Não utilize um endereço local como se fosse um portal público.

Obrigado,<br>
Equipa AutoBotz Motors

## Critérios de lançamento — estado após integração

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
