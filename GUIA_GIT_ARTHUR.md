# Guia Git e empacotamento — AutoBotz Motors (29/10/2026)

O código D11–D15 foi integrado à `main` pela PR #37, merge `f56c8dd`. As PRs remanescentes foram reconciliadas e encerradas. Não recrie merges nem aplique o patch antigo: ele é anterior à integração remota.

## Atualizar a cópia principal sem perder alterações locais

Na máquina de Arthur:

```bash
cd ~/GitHub/autobotz-motors || exit 1
git status --short --branch
git fetch origin
git switch main
git merge --ff-only origin/main
git log -1 --format='%h %s'
```

Os arquivos locais não rastreados `AGENTS.md` e `project-state/` devem permanecer locais, fora do ZIP. Caso o merge se recuse a avançar por colisão com arquivos locais, não use `reset --hard` nem `git clean`; interrompa e inspecione a mensagem.

## Criar o ZIP a partir do código realmente versionado

Execute a geração no diretório do repositório após atualizar a `main`. O ZIP não inclui pastas ocultas, `.git`, caches, backups, credenciais nem dumps. Antes de publicar a entrega, confira o arquivo produzido e sua integridade. A documentação de vídeo é um guia de gravação; o vídeo precisa ser produzido pelo grupo.

Os testes JDBC foram executados antes da integração com MariaDB 13.0.2 no banco descartável `autobotz_d13_test`. Não repita testes destrutivos em um banco real e preserve a chave PII usada durante a gravação, se criar dados fictícios que precise ler depois.

## Histórico e autoria

A PR #37 foi integrada via merge normal, preservando commits. As PRs #22/#23/#24/#29/#32/#33 foram fechadas administrativamente como substituídas, não registradas falsamente como merges individuais. Os commits restantes dos integrantes permanecem consultáveis no GitHub.

Repositório: https://github.com/athr-ocean/autobotz-motors
