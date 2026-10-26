# D15 — Checklist de aceitação para a equipe

## Conteúdo
- [ ] Guia rápido foi conferido na versão integrada do AutoBotz Motors.
- [ ] Roteiro de vídeo foi gravado/ensaiado na versão real e dura aproximadamente 3 minutos.
- [ ] Vídeo, guia e FAQ usam os mesmos nomes e passos verificados.
- [ ] Não há dados pessoais reais nas capturas, exemplos ou vídeo.
- [ ] Materiais futuros/simulados estão identificados como tal.

## Contrato com Mauro
- [ ] Rotas, payloads e erros foram acordados antes de serem implementados.
- [ ] O portal registra evento uma vez por abertura real.
- [ ] O catálogo só marca como publicado o que está disponível.
- [ ] O painel de métricas valida permissão no servidor.

## Persistência e métricas
- [ ] Migração testada em banco isolado terminado em `_test`.
- [ ] Evento `PORTAL_OPEN` aceita slug nulo.
- [ ] Eventos `MATERIAL_VIEW` e `VIDEO_VIEW` exigem slug válido.
- [ ] Tipo desconhecido e slug inválido são rejeitados antes da persistência.
- [ ] Métricas filtram por `[inicio, fim)` e agrupam por tipo e material.
- [ ] Os resultados são conferidos contra eventos inseridos no banco de teste.
- [ ] Testes preservam `autobotz_db` e os dados normais.
- [ ] Exemplos simulados não estão misturados a eventos reais.

## Integração
- [ ] `scripts/run.sh build` passa no clone atualizado.
- [ ] `javac -Xlint:all` foi executado segundo os scripts do projeto.
- [ ] Regressão existente D6–D10 continua passando.
- [ ] Pull Request descreve mudanças, migração, testes, limitações e revisão necessária.
