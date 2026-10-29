# D11 — Qualidade, automação de testes e sustentabilidade

**AutoBotz Motors · Equipe B · 29/10/2026**

## 1. Estratégia e casos de teste

O processo prioritário é **Fornecedor → Orçamento → Solicitação de Compra → Aprovação ou Rejeição → Pedido de Compra**, com transações InnoDB. O mapa de riscos e regras está em `docs/D11_COMPRAS_RELATORIO.md`.

Foram contemplados casos positivos e negativos: fornecedor e CNPJ, orçamento insuficiente, autorização, solicitação pendente, aprovação repetida, inserção do pedido, atualização do saldo e rollback após falha deliberada. As verificações exercitam serviços e persistência JDBC. **A simulação automática de cliques em formulário Swing ainda não foi executada**; a demonstração manual poderá ser gravada pela equipe.

## 2. Ferramentas e execução

- `javac -Xlint:all`: compilação e diagnóstico.
- `D11ComprasPermissaoTest`: regras de autorização sem banco.
- `D11ComprasTest`: fluxo de compra e rollback.
- `D11ComprasJdbcTest`: persistência, saldo e integridade transacional em MariaDB descartável.
- `.github/workflows/integration-regression.yml`: pipeline de regressão integrado.
- `scripts/test-d11-compras.sh`: execução dedicada em banco nomeado `autobotz_*_test`, usando pasta temporária sem apagar `bin/` ou JARs preexistentes.

Em 29/10/2026, o responsável executou Java 21 + MariaDB 13.0.2 e reportou **10/10 testes offline e 8/8 suites JDBC aprovadas** no fechamento anterior (`CoreRegression`, D11–D15). Os dois workflows GitHub do commit integrado `718fe28` concluíram com `success`. A limpeza posterior (#38) compilou e passou na suíte offline e no workflow integrado.

## 3. Comparação temporal — separar hipótese de medição

**Não foi realizada cronometragem manual reproduzível de ponta a ponta no mesmo cenário de compra**, nem medição de potência elétrica. Portanto, não há percentual observado que comprove redução do tempo ou da energia. Os tempos divulgados em versões anteriores (20–30 minutos de teste manual, 3–5 segundos de teste automatizado e 15–30 segundos de CI) eram **estimativas não comparáveis** e não devem ser apresentados como resultados medidos.

O professor fornece um exemplo **hipotético**: manual 2 horas versus automatizado 5 minutos. Nesse exemplo, a diferença seria 115 minutos, mas esse valor **não foi observado no AutoBotz**.

### Protocolo para medição real

1. Definir cenário idêntico em banco descartável: fornecedor, orçamento, solicitação, aprovação e conferência do pedido/saldo.
2. Cronometrar pelo menos três execuções manuais, do início à confirmação final.
3. Cronometrar pelo menos três execuções automatizadas com a mesma carga de trabalho e registrar mediana e variação.
4. Documentar máquina, versão do projeto, tipo de banco, cache, número de registros e método.
5. Se medição energética for necessária, coletar potência real com instrumentação apropriada; estimativas com potência fixa devem permanecer rotuladas como estimativas.

Fórmulas: `tempo_economizado = T_manual − T_automatizado`; `redução_% = 100 × (T_manual − T_automatizado) / T_manual`; `energia_Wh = potência_média_W × tempo_h`. Não atribuir potência aos componentes sem medição ou premissas documentadas.

## 4. Proposta de valor e limites

A automação oferece feedback repetível, reduz risco de regressão e tende a evitar retrabalho. Em matéria de sustentabilidade, **o benefício potencial depende de medição**; executar testes sem tela não prova menor gasto energético total porque runners, banco e tempo de processamento também consomem recursos.

Recomenda-se manter testes rápidos a cada PR, organizar a bateria JDBC em banco descartável, armazenar logs sem segredos e revisar testes que se tornem instáveis. A qualidade da entrega está sustentada pelos testes aprovados e pela possibilidade de repetição, **não por porcentagens inventadas de produtividade ou energia**.
