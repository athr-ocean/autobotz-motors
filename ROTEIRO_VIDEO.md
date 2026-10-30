# Roteiro falado — AutoBotz Motors (Arthur, apresentação individual)

> Vídeo apenas com Arthur falando para a câmera, sem slides, terminal ou compartilhamento de tela. Os títulos são marcas de leitura e não entram na gravação. Tempo aproximado: 29–32 minutos, a conferir em ensaio. Os exemplos identificados como simulações não representam operações ou clientes reais.

## 1. Abertura: o projeto e a segunda rodada

Olá, professor. Eu sou Arthur, responsável pela coordenação e integração do AutoBotz Motors, e vou apresentar sozinho o trabalho da Equipe B na segunda rodada. O nosso projeto é um sistema de gestão de uma empresa do setor automotivo, construído em Java e MariaDB. Na primeira rodada, a equipe desenvolveu funcionalidades como clientes, veículos, vendas, oficina, CRM, projetos, relatórios e controle de acesso. Agora vou explicar como ampliamos esse sistema sem abandonar o que já funcionava.

Nesta etapa nós trabalhamos com cinco desafios. O primeiro organiza compras e discute a qualidade do software e a sustentabilidade. O segundo trabalha a ideia de uma crise econômica, com planos de assinatura e simulações de receita. O terceiro envolve contratos e rastreabilidade. O quarto é segurança cibernética, com autenticação e proteção de informações pessoais. E o quinto é treinamento: como ajudar as pessoas a usar um sistema que ganhou várias funcionalidades.

Durante a apresentação, eu vou distinguir o que foi de fato implementado e testado do que é uma simulação acadêmica. Quando eu falar de churn, pesquisa de satisfação ou pagamento simulado, não estou falando de dados de clientes reais. Meu objetivo é explicar o trabalho da equipe com transparência, mostrar os módulos integrados e apresentar as verificações disponíveis.

Antes de falar do lado técnico, quero situar a ideia principal. Um sistema de gestão só é útil quando as áreas conversam entre si. Não adianta a empresa cadastrar um fornecedor num lugar e ter de fazer todo o controle de contratos em outro, ou possuir uma boa regra de segurança que não seja respeitada pelo portal de treinamento. Por isso, nessa segunda rodada, o nosso maior esforço foi dar continuidade ao sistema, e não simplesmente entregar cinco programas isolados. Eu vou seguir uma sequência lógica para que seja possível entender cada desafio mesmo sem acompanhar o código.

## 2. Como o AutoBotz funciona e por que a integração importa

Antes de entrar nos desafios novos, vale explicar rapidamente a estrutura. O AutoBotz não é composto por programas separados para cada integrante. A nossa meta foi que tudo funcionasse dentro de uma mesma aplicação. Por isso temos modelos de dados, classes DAO que acessam o MariaDB, serviços que aplicam regras de negócio, uma interface Swing e uma opção de console. O portal do treinamento é a parte web, mas ele também se conecta ao sistema.

Essa separação faz diferença na prática. Por exemplo: numa compra, não basta o botão de aprovar existir. A regra precisa conferir se o usuário tem permissão, se ainda existe orçamento e se a gravação foi concluída. Se uma etapa dá erro, a operação tem de ser revertida. Isso é tratado no serviço e no banco. A tela é a forma de o usuário interagir, mas a consistência não pode depender só dela.

Outra parte importante são os arquivos de atualização do banco. Na primeira rodada nós já tínhamos clientes, veículos, usuários, vendas, oficina e projetos. Agora entram tabelas de compras, planos, contratos, eventos de segurança e treinamento. As migrations vão da 011 até a 016, e a ordem importa: o contrato de fornecedor, por exemplo, depende de uma estrutura criada antes pelo módulo de compras.

Na validação final, nós preparamos um banco descartável para os testes, aplicamos o schema e executamos essa sequência de migrations duas vezes. A repetição terminou sem erro. Também executamos a regressão do sistema anterior, para conferir que as novas funcionalidades não tinham quebrado cadastro de cliente, venda, oficina, CRM, autenticação e outras partes da primeira etapa. Então a principal ideia aqui é continuidade: o projeto cresceu, mas continuou sendo um único ERP.

As migrations são como um histórico organizado das mudanças estruturais no banco. Em vez de cada integrante criar tabelas de maneira independente e depois tentar juntar tudo, nós numeramos as alterações e respeitamos suas dependências. Isso ajuda a identificar por que uma tabela existe, em que momento ela entrou e quais módulos precisam dela. O cuidado é ainda maior quando tratamos segurança: uma atualização não pode fazer a aplicação perder a capacidade de autenticar usuários ou ler dados que já foram cadastrados. Foi por isso que a compatibilidade entre as migrations virou um ponto central da revisão final.

## 3. D11: compras, testes e sustentabilidade

Começando pelo D11: o Alexandre ficou com a frente principal de compras, e eu vou explicar como ela foi integrada ao ERP. O fluxo é simples de entender: cadastrar um fornecedor, criar um orçamento, registrar uma solicitação e decidir se ela será aprovada ou rejeitada. Ao aprovar, o sistema cria o pedido e reduz o saldo previsto. Se o valor solicitado ultrapassar o que ainda existe no orçamento, ele não pode consumir dinheiro nem gerar pedido.

Na interface Swing existe o painel de compras, mas a parte principal da regra está no ComprasService. Assim, a aprovação é validada além da tela. Outra preocupação foi impedir o mesmo pedido de ser aprovado duas vezes. Uma solicitação que já gerou um pedido não pode continuar debitando o orçamento toda vez que alguém clicar. O sistema trata isso como uma transição inválida.

Vou dar um exemplo simples, só para visualizar a regra. Suponha que a empresa reserve mil reais para uma categoria de compras e receba uma solicitação de trezentos reais. Se ela for aprovada corretamente, o orçamento passa a ter setecentos reais disponíveis, e o pedido correspondente fica registrado. Se a solicitação for rejeitada, o saldo continua igual. E se ocorrer um erro no meio da aprovação, o sistema não pode ficar com setecentos reais sem ter criado o pedido. É exatamente esse tipo de situação que torna as transações importantes numa aplicação administrativa.

Também implementamos o uso de transações no banco. Imagine que o valor tenha sido descontado, mas aconteça uma falha bem na hora de salvar o pedido. Se o saldo ficasse reduzido sem pedido registrado, a informação estaria errada. Por isso utilizamos rollback: a operação volta ao estado anterior quando não pode ser concluída. Os testes foram escritos para provocar esse cenário e verificar o resultado, não apenas para executar o caminho que dá certo.

Além da implementação funcional, o Adryan contribuiu com os testes e com a integração contínua. No D11 nós temos testes de permissão, das regras de compra e de JDBC em um banco descartável. Eles verificam saldo insuficiente, aprovação repetida, geração do pedido e rollback após erro provocado. Assim, eu consigo demonstrar que a lógica foi validada além da aparência do formulário.

O projeto também usa GitHub Actions para automatizar a regressão. Quando uma alteração entra em revisão, a automação pode compilar o código e repetir os cenários. Isso é útil porque o grupo está trabalhando em vários módulos: uma mudança numa classe compartilhada pode causar um erro em outro desafio, e sem teste seria mais difícil perceber.

O enunciado também pede uma discussão sobre eficiência e sustentabilidade. Aqui nós preferimos separar o que é demonstrado do que é expectativa. A automação torna as verificações repetíveis, mas nós não medimos de forma comparável quanto tempo uma pessoa gastaria fazendo todos os testes manualmente. Também não medimos consumo real de energia. Então não seria correto dizer que economizamos um percentual específico. Nossa proposta de melhoria é reduzir retrabalho, documentar as execuções e criar um processo mais confiável.

## 4. D12: crise econômica e planos de assinatura

No D12, a frente de planos e assinaturas foi conduzida pelo André. O problema proposto parte de uma situação econômica difícil: alguns clientes precisam reduzir despesas, e a empresa também precisa planejar como isso afeta a receita. Para tratar esse cenário, o ERP ganhou planos de assinatura, módulos opcionais e registros de eventos, como contratação, alteração e cancelamento. A ideia é analisar diferentes possibilidades sem alterar contratos verdadeiros.

Duas expressões aparecem nessa parte. A primeira é MRR, ou receita mensal recorrente: o total mensal dos contratos ativos, considerando o critério do cálculo. A segunda é churn, que representa os cancelamentos numa base definida. Não basta dizer que houve uma queda de faturamento; precisamos saber se a perda veio de cancelamentos, de mudanças para planos mais baratos ou de outros fatores.

Essa diferença entre receita e retenção é muito importante para a tomada de decisão. Uma empresa pode aumentar o preço de um plano e, mesmo assim, ganhar menos se uma parcela significativa dos clientes cancelar. O contrário também acontece: oferecer uma opção mais acessível pode reduzir o valor por contrato, mas ajudar a manter uma relação comercial que seria perdida. A simulação existe justamente para explorar essas possibilidades. Ela transforma uma discussão abstrata sobre crise econômica em cenários que podem ser comparados, deixando claro que o resultado continua dependendo do comportamento real dos clientes.

Nós também preparamos um relatório acadêmico de seis meses, com possíveis motivos de cancelamento, como preço alto, baixa utilização e necessidade de um plano básico. Mas esse histórico é fictício, porque não recebemos uma base real de clientes nem respostas reais de pesquisa. Ele serve para mostrar como seria feita a análise e quais dados precisariam ser coletados numa situação empresarial verdadeira.

Para explicar essa lógica de maneira concreta, nós integramos um simulador à interface. Ele permite definir a quantidade de clientes, o preço do plano atual, do básico e do premium e marcar módulos adicionais, como CRM e Vendas Avançadas. O programa distribui o cenário em trinta por cento migrando para o plano básico, cinquenta por cento ficando e vinte por cento indo para o premium. Esse cálculo usa o serviço de assinaturas do projeto e não grava os clientes fictícios no banco.

Por exemplo, com cem assinaturas de cento e vinte reais, incluindo o módulo CRM, o MRR inicial seria doze mil reais. Considerando os preços fictícios do cenário, a distribuição resulta em doze mil e quatrocentos reais por mês. Isso significa uma diferença projetada de quatrocentos reais, não um ganho real da empresa. Se houver novos cancelamentos, o resultado muda; se houver custos e impostos maiores, a margem também muda. É por isso que simular ajuda a pensar, mas não substitui uma análise financeira completa.

No exemplo dos cem contratos, a distribuição é fácil de acompanhar: trinta clientes mudam para o básico, cinquenta permanecem onde estavam e vinte passam ao premium. O módulo opcional entra no cálculo para mostrar que o valor mensal não depende somente do nome do plano. Essa conta também explica por que a equipe precisou alinhar o modelo de assinatura, os módulos e a classe de resultado do simulador. Se cada parte calculasse de um jeito, a interface poderia apresentar um total diferente do serviço. Os testes servem para evitar exatamente essa divergência.

É importante destacar uma limitação da proposta. Nosso relatório fala de razões possíveis para cancelamento e apresenta respostas hipotéticas de pesquisa, porque nós não recebemos um histórico real de contratos e entrevistas. Se a empresa quisesse adotar a mudança de verdade, precisaria medir a quantidade de clientes ativos em cada período, o motivo dos cancelamentos, o custo dos módulos, os impostos e o retorno efetivo de cada plano. Só depois desses dados seria possível decidir qual proposta é financeiramente sustentável.

Os cálculos e a persistência foram verificados em testes. O simulador novo também ganhou testes para entradas inválidas e para a abertura do painel. Por último, o plano de comunicação escrito propõe explicar mudanças de preço com clareza, sem alterar um contrato de alguém apenas porque o exercício apontou um cenário mais rentável.

## 5. D13: contratos e rastreabilidade

Agora o D13, que foi desenvolvido principalmente pelo Gabriel. Ele amplia o cadastro de fornecedores que nasceu no D11, adicionando contratos com datas, estados, valores e condições. Um contrato não pode ficar isolado numa planilha, sem relação com seu fornecedor. O sistema cria esse vínculo e permite acompanhar vencimento, alterações e critérios de elegibilidade para um pagamento que, neste projeto, é apenas simulado.

Uma preocupação é o vencimento. Se a empresa não acompanha a data final de um contrato ou uma nota fiscal pendente, ela pode perder prazo ou autorizar uma operação inadequada. O serviço aplica regras que permitem identificar situações como contrato vencido, documento não validado ou pagamento ainda não elegível. Quando falamos em pagamento, é importante destacar que o sistema apenas simula a autorização e o registro; ele não transfere dinheiro para ninguém.

A parte de inovação ficou com uma prova de conceito de integridade. Nós usamos um ledger, que é como um livro de registros, com hashes encadeados. Cada evento recebe um resumo criptográfico calculado com SHA-256 e fica ligado ao registro anterior. Quando alguém altera um registro antigo de forma indevida, o processo de verificação consegue detectar que a sequência perdeu a consistência esperada.

Pense, por exemplo, num contrato cuja situação mudou de ativo para encerrado. Essa mudança precisa ficar registrada com contexto, e o histórico não deveria aceitar alterações silenciosas. O ledger ajuda a demonstrar esse controle. Os testes D13LedgerIntegrityTest e D13JdbcIntegrationTest verificam o encadeamento e situações de persistência. A frente do Gabriel também incluiu uma API simulada para representar uma comunicação externa, com regras sobre quais informações podem ser trocadas.

Um hash, nesse contexto, funciona como uma impressão digital calculada a partir do conteúdo do registro. Ele não é uma fotografia do contrato nem substitui a assinatura de todas as partes. Seu papel no nosso protótipo é ajudar a verificar se os eventos registrados permanecem consistentes. Como cada registro considera o anterior, alterar um trecho antigo provoca uma diferença que pode ser percebida na verificação da cadeia. É uma forma de demonstrar rastreabilidade e auditoria de maneira didática. Isso não transforma automaticamente o sistema em uma rede blockchain, e por isso nós tratamos a solução como prova de conceito local.

Não estamos chamando essa solução de blockchain pública. Ela não possui rede de nós independentes, consenso distribuído nem contratos inteligentes financeiros. É um piloto local para mostrar como técnicas de encadeamento criptográfico podem apoiar a rastreabilidade. Essa distinção é importante, porque o objetivo acadêmico é explicar a técnica e seus limites, não apresentar uma infraestrutura externa que não foi construída.

## 6. D14: segurança, autenticação e proteção de dados

No D14, o João conduziu a parte de autenticação, tentativas de acesso e MFA, enquanto eu fiquei responsável pela integração com a proteção dos dados pessoais. O primeiro problema era o ataque por tentativas repetidas de senha. O fluxo desenvolvido registra falhas e bloqueia a conta depois de cinco tentativas incorretas numa janela de dez minutos. Isso dificulta a adivinhação de credenciais dentro do cenário implementado.

Também acrescentamos uma etapa de MFA demonstrativo. Depois que a senha é aceita, os perfis com acesso a informações críticas precisam concluir esse segundo passo antes de utilizar a sessão. Os testes cobrem desafios de autenticação, códigos incorretos, vencidos e reutilizados. O sistema ainda registra eventos de segurança para que a equipe possa investigar o que aconteceu, dentro do escopo da aplicação.

O material do desafio traz também um cenário de ransomware. Isso não significa que o AutoBotz tenha sofrido um ataque de verdade. Nós elaboramos uma resposta simulada: identificar o incidente, isolar o ambiente afetado, preservar evidências, avaliar backups, comunicar os responsáveis e documentar o retorno seguro. A proposta é mostrar que segurança não se resume a um campo de senha; ela inclui prevenção, detecção e um procedimento de reação.

Na parte que ficou sob minha responsabilidade no D14, eu trabalhei com a proteção dos dados pessoais dos clientes. Não basta impedir a visualização de uma tela: nome, CPF, telefone e e-mail também precisam ser protegidos quando são gravados. Para isso usamos AES-GCM, com uma chave mantida fora do repositório. Essa proteção está na persistência para não depender de cada usuário lembrar de cifrar manualmente.

Existe uma dificuldade importante: o sistema precisa localizar um cliente pelo CPF mesmo depois de esse campo estar cifrado. Para isso criamos um índice HMAC, que permite comparar o CPF procurado sem precisar armazenar um índice legível com o número completo. É uma técnica diferente de usar hash puro ou de deixar o CPF exposto na tabela.

Vale separar duas ferramentas que podem parecer iguais, mas têm funções diferentes. A criptografia AES-GCM protege o conteúdo do dado: ela é usada quando o sistema precisa guardar e, com a chave correta, recuperar a informação. O HMAC, por outro lado, é útil para comparar um valor de maneira determinística, sem publicar aquele valor como coluna de busca em texto puro. Assim conseguimos procurar o CPF sem depender de uma lista legível de números pessoais. Esse desenho também obriga a pensar em gestão de chaves: se a chave usada para cifrar os dados for perdida, não podemos simplesmente inventar outra e esperar recuperar o conteúdo antigo.

Os registros antigos também precisavam ser tratados com cuidado. A migração não é acionada automaticamente só por abrir o programa. Primeiro existe um modo de diagnóstico, depois a aplicação requer confirmação explícita do banco e uma chave válida. O objetivo é evitar que testes ou uma inicialização acidental façam alterações em dados que não deveriam ser tocados.

Esses mecanismos foram exercitados em testes de regressão, junto com as sessões usadas pelo portal. Um exemplo concreto aparece na API de métricas do D15: sem sessão, a resposta é 401; com sessão sem permissão, 403; com ADMIN autenticado após MFA, 200. O teste HTTP com MariaDB verificou esses casos. Ainda assim, nosso MFA é didático, e a atividade não é uma certificação de segurança ou um atestado jurídico de conformidade integral com a LGPD.

## 7. D15: treinamento, portal e acompanhamento de resultados

Chegando ao D15, o Mauro ficou com o portal de treinamento e eu participei do encaixe dessa solução no ERP. O problema aqui é muito prático: quando um sistema ganha novas telas, as pessoas precisam aprender a usá-las. Então foi criada uma central local com catálogo de conteúdos, orientações e perguntas frequentes. Ela pode ser acessada pelo atalho da aplicação principal ou diretamente no navegador.

O portal foi implementado com um servidor HTTP em Java e uma interface simples. Ele não está hospedado publicamente na internet; roda na própria máquina, normalmente no endereço localhost e na porta 8085. Isso reduz dependências externas para uma demonstração e mantém o ambiente consistente com o restante do projeto.

Na integração, o backend deixou de depender de dados simulados como fonte principal e passou a usar o TreinamentoService ligado ao banco por um DataSource. Quando alguém abre o portal ou visualiza determinado material, o sistema consegue registrar um evento de uso. O servidor também valida o conteúdo que serve, para que o usuário não peça qualquer arquivo do computador por um endereço malformado.

Um ponto que vale explicar é que o portal não cria um sistema de login paralelo ao ERP. A consulta de métricas administrativas usa a sessão e as verificações de autorização da camada de segurança. Essa ligação entre D14 e D15 foi um dos contratos de integração mais importantes da rodada.

O Higor contribuiu com os materiais de treinamento e com a definição de métricas. O conteúdo inclui guia rápido, FAQ e roteiros escritos para tarefas frequentes. Por exemplo, alguém que esqueceu o procedimento de cadastro pode consultar uma orientação antes de abrir um chamado. Isso é uma proposta de capacitação, e não uma prova de que os usuários já passaram a ter menos dúvidas.

Nós também fizemos uma análise de tickets e pesquisa de satisfação como exercício acadêmico. O documento apresenta uma distribuição fictícia de chamados ao longo de três meses, em categorias como login, clientes, compras e relatórios. Também traz perguntas que poderiam ser aplicadas numa pesquisa real. Esses números são simulados. Nós não entrevistamos vinte usuários reais nem reunimos quarenta e cinco chamados verdadeiros.

As métricas técnicas do portal são diferentes desses exemplos: elas vêm dos eventos registrados quando a aplicação está sendo usada no banco de teste. Isso pode mostrar quantos materiais foram abertos e como o conteúdo foi acessado, mas não prova, por si só, que as pessoas aprenderam mais ou que os chamados caíram. Para demonstrar redução de suporte, precisaríamos comparar períodos equivalentes com dados reais e considerar também a quantidade de usuários ativos.

Nos documentos acadêmicos, construímos um conjunto ilustrativo de quarenta e cinco chamados distribuídos por três meses, com categorias como login, cadastro de clientes, compras e relatórios. Esses quarenta e cinco chamados não vieram de um sistema real de suporte. Servem para demonstrar como organizaríamos um diagnóstico: primeiro separaríamos os problemas por assunto, depois identificaríamos as dúvidas mais frequentes e, por fim, criaríamos material específico para cada uma. A mesma lógica vale para a pesquisa de satisfação fictícia. Uma resposta simulada pode ajudar a desenhar um questionário, mas nunca pode ser apresentada como a opinião de uma pessoa que realmente participou.

Também existe diferença entre medir acesso ao conteúdo e medir resultado do treinamento. Saber que alguém abriu o guia é uma informação útil, mas não prova que aprendeu, nem que deixou de precisar do suporte. Para afirmar uma redução de chamados, precisaríamos comparar períodos equivalentes, ajustar pela quantidade de usuários ativos e considerar outros fatores que poderiam explicar a mudança. Por enquanto, o AutoBotz oferece os registros de acesso necessários para começar essa avaliação, e a documentação explica como ela poderia ser feita posteriormente.

Os testes D15PortalTest, D15PersistenciaTest e D15AuthHttpJdbcTest validaram respectivamente rotas, persistência e autorização. O guia e o FAQ estão disponíveis. Já o vídeo tutorial curto e o webinar não foram gravados, e o FAQ não deve ser confundido com um fórum interativo já existente.

## 8. Integração final, GitHub e validações

Na etapa final, eu coordenei a consolidação técnica e a revisão das contribuições na mesma base. As pessoas trabalharam em diferentes frentes, mas o resultado é um único repositório executável, com DAOs, serviços, interface, banco e testes. O histórico Git mantém os commits dos integrantes; os ajustes de integração não apagam a autoria de quem desenvolveu cada módulo.

No GitHub, a PR trinta e sete reuniu D11 até D15 e preservou o histórico de commits. Depois, a trinta e oito retirou arquivos compilados e uma cópia antiga do projeto que estavam no versionamento. A trinta e nove acrescentou o simulador de planos na interface e completou a documentação acadêmica. A branch principal terminou no commit fa580cde, e as PRs antigas foram reconciliadas. Isso mantém o trabalho rastreável.

Na validação usamos Java 21. O ZIP final foi extraído, recompilado e passou em onze classes de teste sem banco. Antes disso, oito classes de teste JDBC haviam passado num banco MariaDB descartável, incluindo a regressão dos módulos anteriores e os desafios novos. O pipeline integrado do GitHub também concluiu com sucesso depois das últimas correções. Esses são resultados registrados, mas não significam que qualquer uso do sistema esteja livre de defeitos.

O resultado da validação precisa ser entendido por camadas. Os testes sem banco verificam cálculos, regras, conteúdo e partes do portal. Os testes JDBC exercitam operações com o MariaDB e ajudam a identificar problemas que não aparecem só compilando. Já o GitHub Actions repete uma regressão num ambiente de integração. E o pacote final foi testado após ser extraído, para evitar a situação de um projeto funcionar numa pasta de desenvolvimento, mas falhar quando outra pessoa tenta utilizá-lo. Esses níveis de verificação se complementam; nenhum deles é garantia de ausência absoluta de defeitos.

## 9. Limitações, aprendizados e encerramento

Antes de encerrar, eu quero deixar bem claros os limites do trabalho. Os números de planos, churn e pesquisas são fictícios. Nós não medimos redução real de chamados, consumo de energia ou tempo manual em condições comparáveis. O D13 usa um ledger local, e o MFA é uma demonstração acadêmica. E o vídeo tutorial curto do D15 é um roteiro, não uma gravação que já esteja publicada.

Com essas ressalvas, o resultado principal foi integrar compras, assinaturas, contratos, proteção de dados e capacitação à base que já existia. Conseguimos organizar o código, preservar a autoria dos integrantes, executar regressões e documentar como as partes se relacionam. Mais do que adicionar novas telas, nosso objetivo foi mostrar que decisões administrativas, persistência e segurança precisam funcionar em conjunto.

Esse é o resultado da segunda rodada do AutoBotz Motors: uma base que recebeu novas funcionalidades e foi verificada com testes reais. Como responsável pela apresentação, eu agradeço ao professor pela atenção e também aos integrantes da Equipe B, que desenvolveram suas frentes. Fico à disposição para explicar qualquer decisão técnica, os testes e os limites de cada módulo.
