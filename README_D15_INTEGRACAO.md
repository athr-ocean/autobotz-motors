# Pacote de contribuição D15 — Higor

Este pacote reúne os artefatos da frente de conteúdo e métricas do D15 para revisão e integração no AutoBotz Motors.

## Conteúdo

- `docs/D15_GUIA_RAPIDO.md`: guia rápido de cadastro de cliente, veículo e venda.
- `docs/D15_ROTEIRO_VIDEO.md`: roteiro narrável de vídeo de aproximadamente três minutos.
- `docs/D15_FAQ.md`: perguntas frequentes para o portal.
- `docs/D15_MATERIAIS_TRANSICAO.md`: plano de publicação e comunicado modelo.
- `docs/D15_CONTRATO_API.md`: contrato **proposto**, para acordo com Mauro; não declara rotas já existentes.
- `docs/D15_METRICAS.md`: definição de eventos, métricas, limites e testes de aceitação.
- `db/migrations/015_d15_treinamento.sql`: migração aditiva da tabela de eventos.
- `src/autobotz/model/`: modelos do evento e da métrica agregada.
- `src/autobotz/dao/EventoTreinamentoDAO.java`: persistência e agregação via `DataSource`.
- `src/autobotz/service/TreinamentoService.java`: validação dos eventos e acesso às métricas.

## Limite de integração

O repositório remoto não pôde ser clonado neste ambiente, então este pacote foi preparado a partir do plano D15 disponibilizado, sem editar arquivos compartilhados do projeto. Antes do merge, integrar e revisar estes arquivos no clone atualizado.

As classes usam `javax.sql.DataSource` para não embutir usuário/senha nem assumir uma classe de conexão que não foi examinada. No clone do grupo, conecte o serviço ao `DataSource`/provedor de conexão oficial do projeto. Não crie uma segunda configuração de banco. Se a aplicação atual não disponibilizar `DataSource`, combine com o responsável pela persistência uma adaptação pequena.

## Ordem recomendada

1. Confirmar com Mauro o contrato HTTP em `docs/D15_CONTRATO_API.md` antes de implementar endpoints.
2. Confirmar no ERP integrado os passos e nomes reais usados no guia e no vídeo.
3. Revisar a migração com Arthur e aplicar primeiro em banco descartável com nome terminado em `_test`.
4. Integrar as classes nos pacotes do projeto e ligar o serviço às rotas/backend de Mauro.
5. Testar eventos válidos/inválidos, intervalo das métricas e autorização administrativa.
6. Criar Pull Request a partir da branch `feature/d15-training-content-metrics`.

## Compilação isolada das classes incluídas

Com JDK 17 ou superior:

```bash
mkdir -p /tmp/d15-classes
javac -Xlint:all -d /tmp/d15-classes $(find src -name '*.java')
```

Este comando valida apenas as classes deste pacote quando executado dentro da pasta extraída. A integração com o projeto completo e os testes de banco continuam necessários.

## Regras de dados

- Não registrar credenciais, CPF, e-mail ou IP inventado nos eventos.
- Métricas de exemplo têm que ser rotuladas como `SIMULADO`.
- Não fazer alterações paralelas em `schema.sql`; a migração é revisada e incorporada pelo responsável definido pelo grupo.
- Não alterar `AppFrame.java` nem rotas do Mauro sem coordenação.
