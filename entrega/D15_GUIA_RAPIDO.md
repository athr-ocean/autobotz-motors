# AutoBotz Motors — Guia rápido de uso

**D15 — Treinamento e Capacitação**
**Versão:** 1.0 — material didático para revisão pela equipe

## Objetivo

Este guia apresenta um fluxo de trabalho que reúne três funções centrais do AutoBotz Motors: cadastrar um cliente, cadastrar um veículo e registrar uma venda. A intenção é orientar uma pessoa usuária durante a execução do processo, sem substituir as validações do sistema.

> **Revisão necessária antes de publicar:** confirme na versão integrada do AutoBotz Motors os nomes exatos das telas, botões e campos. Este guia usa os nomes funcionais dos módulos e não presume que a interface exiba rótulos específicos. Não publique capturas com dados pessoais reais.

## 1. Cadastrar um cliente

1. Abra o AutoBotz Motors e acesse o módulo de **Clientes** pela navegação disponível na versão instalada.
2. Escolha a ação de cadastro de cliente, caso esteja disponível para seu perfil.
3. Preencha os campos solicitados pela tela usando dados fictícios em treinamento e dados autorizados em uso real.
4. Confira as informações antes de salvar, especialmente os dados usados para identificar o cliente.
5. Salve o cadastro e confirme na própria aplicação se o registro foi criado.
6. Se houver mensagem de validação, leia-a e corrija os campos indicados. Não tente contornar regras de validação.

**Verificação:** o cliente deve aparecer na consulta/listagem de clientes, conforme as opções existentes na versão instalada.

## 2. Cadastrar um veículo

1. Acesse o módulo de **Veículos**.
2. Abra a ação de cadastro de veículo disponibilizada pela aplicação.
3. Preencha os campos obrigatórios apresentados na tela, usando dados de exemplo em ambiente de treinamento.
4. Revise identificadores e valores antes de salvar.
5. Salve e confirme que o veículo aparece na consulta/listagem do módulo.

**Verificação:** confirme que o registro foi persistido e que a aplicação não informou erro. Se o veículo já existir ou algum campo for inválido, siga a mensagem apresentada e não crie registros duplicados artificialmente.

## 3. Registrar uma venda

1. Acesse o módulo de **Vendas**.
2. Inicie o registro de uma venda conforme as opções disponíveis na versão integrada.
3. Selecione o cliente e o veículo cadastrados nas etapas anteriores, caso esses sejam os campos exigidos pelo fluxo atual.
4. Preencha os demais campos solicitados pela aplicação e revise os dados antes de confirmar.
5. Salve a venda e aguarde a resposta do sistema.
6. Verifique a consulta ou o relatório disponível para confirmar que a venda foi registrada.

**Verificação:** confirme que a venda aparece no módulo correspondente e que o sistema manteve a consistência dos registros relacionados. Não repita o salvamento para “garantir” o resultado se a primeira operação estiver demorando; primeiro verifique se a venda já foi registrada.

## 4. Se algo der errado

- **Campo inválido:** revise a mensagem exibida e corrija somente os campos indicados.
- **Registro não encontrado:** atualize a consulta ou refaça a pesquisa com os dados corretos; não cadastre novamente antes de verificar se o primeiro cadastro foi concluído.
- **Falha ao salvar:** registre a mensagem de erro sem incluir senhas, tokens ou dados pessoais desnecessários e comunique o suporte responsável.
- **Dúvida sobre uma função que não aparece:** confirme se a versão instalada e o perfil de acesso disponibilizam aquela função. Não presuma que um recurso ainda não integrado esteja disponível.

## 5. Boas práticas

- Use dados fictícios em demonstrações e treinamentos.
- Confira os dados antes de confirmar operações.
- Não compartilhe senhas nem dados pessoais por capturas de tela ou canais não autorizados.
- Use as permissões atribuídas ao seu perfil.
- Avise a equipe quando o guia não corresponder à interface atual, para que o material seja atualizado junto com o sistema.

## Histórico de revisão

Este material deve ser validado contra a versão integrada do ERP antes de ser publicado no portal. A equipe deve registrar a versão/revisão do sistema testada e a pessoa que conferiu os rótulos reais da interface.
