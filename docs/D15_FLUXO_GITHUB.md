# D15 — Como subir a contribuição e fazer merge

Este procedimento respeita o fluxo definido no plano da equipe: branch de trabalho, commits pequenos, Pull Request, revisão e testes. Não enviar diretamente para `main`.

## 1. Atualizar o clone sem descartar mudanças locais

Entre na pasta do clone do AutoBotz Motors e confira:

```bash
git status --short --branch
```

Se houver arquivos modificados que você quer manter, salve/commite-os antes de trocar de branch. Não use `git reset --hard`. Arquivos locais como `lib/` não devem ser adicionados sem aprovação e não fazem parte desta entrega.

Se a árvore estiver pronta para trocar de branch:

```bash
git fetch origin --prune
git switch main
git pull --ff-only origin main
git switch -c feature/d15-training-content-metrics
```

Se a branch já existir no clone, use `git switch feature/d15-training-content-metrics` em vez de criá-la novamente. Confira que a `main` ficou atualizada antes de começar.

## 2. Extrair o pacote fora do repositório

Ajuste o caminho para o local em que o navegador salvou o ZIP:

```bash
mkdir -p /tmp/d15-higor
unzip ~/Downloads/D15_Higor_Pacote.zip -d /tmp/d15-higor
```

Copie os arquivos para os caminhos correspondentes do clone, sem copiar a pasta de pacote inteira como uma subpasta do projeto:

```bash
cp -r /tmp/d15-higor/d15_higor_package/docs/. docs/
cp /tmp/d15-higor/d15_higor_package/db/migrations/015_d15_treinamento.sql db/migrations/
cp /tmp/d15-higor/d15_higor_package/src/autobotz/model/EventoTreinamento.java src/autobotz/model/
cp /tmp/d15-higor/d15_higor_package/src/autobotz/model/MetricaTreinamento.java src/autobotz/model/
cp /tmp/d15-higor/d15_higor_package/src/autobotz/dao/EventoTreinamentoDAO.java src/autobotz/dao/
cp /tmp/d15-higor/d15_higor_package/src/autobotz/service/TreinamentoService.java src/autobotz/service/
cp /tmp/d15-higor/d15_higor_package/tests/D15_CHECKLIST_ACEITACAO.md tests/
cp /tmp/d15-higor/d15_higor_package/README_D15_INTEGRACAO.md .
```

Se as pastas do seu clone tiverem nomes diferentes, pare e confira o layout antes de copiar. Leia o diff e não sobrescreva alterações de colegas.

## 3. Antes do commit

1. Alinhar `docs/D15_CONTRATO_API.md` com Mauro. As rotas ali são propostas, não endpoints existentes.
2. Integrar as classes ao mecanismo de conexão oficial do projeto, usando o `DataSource` existente ou uma adaptação aprovada. Não criar usuário/senha de banco no código.
3. Solicitar a Arthur a revisão da migração; aplicar primeiro somente em banco descartável cujo nome termine em `_test`. Não executar teste de escrita em `autobotz_db` normal.
4. Conferir os passos do guia e do roteiro de vídeo na versão real da interface.
5. Executar os comandos previstos no repositório:

```bash
git diff --check
bash scripts/run.sh build
```

Execute também os testes D15 e regressão existentes conforme o procedimento do projeto. Se o build do projeto falhar por integração, não declare a frente testada só porque as classes isoladas compilam.

## 4. Revisar e enviar a branch

```bash
git status --short
git diff --stat
git diff
```

Adicione somente os arquivos D15 esperados. Não use `git add .` sem revisar:

```bash
git add docs/D15_GUIA_RAPIDO.md docs/D15_ROTEIRO_VIDEO.md docs/D15_FAQ.md docs/D15_MATERIAIS_TRANSICAO.md docs/D15_CONTRATO_API.md docs/D15_METRICAS.md docs/D15_FLUXO_GITHUB.md db/migrations/015_d15_treinamento.sql src/autobotz/model/EventoTreinamento.java src/autobotz/model/MetricaTreinamento.java src/autobotz/dao/EventoTreinamentoDAO.java src/autobotz/service/TreinamentoService.java tests/D15_CHECKLIST_ACEITACAO.md README_D15_INTEGRACAO.md

git diff --cached --check
git diff --cached --stat
git commit -m "feat(d15): adiciona conteúdo e métricas de treinamento"
git push -u origin feature/d15-training-content-metrics
```

Só faça commit depois de resolver as observações de integração e registrar os testes realmente executados.

## 5. Abrir Pull Request e fazer merge pelo GitHub

1. Abra `https://github.com/athr-ocean/autobotz-motors` e entre na aba **Pull requests**.
2. Clique em **New pull request**.
3. Selecione `base: main` e `compare: feature/d15-training-content-metrics`.
4. Confira a lista de arquivos. O PR deve conter apenas a contribuição D15 e qualquer integração previamente combinada.
5. Use um título como `feat(d15): conteúdo e métricas de treinamento`.
6. Na descrição, registre objetivo, arquivos, migração, build/testes executados, limitações e a revisão do contrato com Mauro. Solicite revisão de Arthur e dos responsáveis pelo contrato.
7. Espere os testes/checks e a revisão. Corrija comentários e envie novos commits para a mesma branch quando necessário.
8. Quando as proteções do repositório e os revisores aprovarem, clique em **Merge pull request** e confirme. Se o botão estiver bloqueado, não tente contornar a proteção; resolva os checks ou peça ao mantenedor.
9. Depois do merge:

```bash
git switch main
git pull --ff-only origin main
git status --short --branch
```

A exclusão da branch é opcional e deve ocorrer somente depois de confirmar que o PR foi incorporado.

## Importante

Ser colaborador permite criar branch e propor um Pull Request, mas não garante permissão para fazer o merge. As regras do repositório podem exigir revisão, checks verdes ou um mantenedor. A contribuição não foi enviada ao GitHub por este pacote: você precisará aplicá-la ao clone e abrir o PR.
