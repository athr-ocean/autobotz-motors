import autobotz.model.ContratoFornecedor;
import autobotz.model.RegistroLedgerContrato;
import autobotz.model.ResultadoIntegridadeLedger;
import autobotz.model.StatusContrato;
import autobotz.model.TipoEventoContrato;
import autobotz.service.ContratoService;
import autobotz.util.LedgerContratoHash;
import autobotz.util.I18nUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Testes determinísticos do ledger e das condições de vencimento do D13, sem banco externo. */
public final class D13LedgerIntegrityTest {
    private static int passed;

    private D13LedgerIntegrityTest() { }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("FAIL " + name);
        passed++;
        System.out.println("PASS " + name);
    }

    public static void main(String[] args) {
        LocalDateTime t = LocalDateTime.of(2026, 10, 27, 12, 0);
        ContratoFornecedor rascunho = new ContratoFornecedor(42, 7, new BigDecimal("2500.00"),
                LocalDate.of(2026, 11, 6), StatusContrato.RASCUNHO, false, false, 0,
                ContratoFornecedor.HASH_GENESIS, t);
        RegistroLedgerContrato um = RegistroLedgerContrato.novo(42, 1, TipoEventoContrato.CONTRATO_CRIADO,
                LedgerContratoHash.retratoContrato(rascunho) + ";detalhe=criado",
                ContratoFornecedor.HASH_GENESIS, t);
        ContratoFornecedor negociacao = new ContratoFornecedor(42, 7, new BigDecimal("2500.00"),
                LocalDate.of(2026, 11, 6), StatusContrato.EM_NEGOCIACAO, false, false, 1,
                um.getHashRegistro(), t);
        RegistroLedgerContrato dois = RegistroLedgerContrato.novo(42, 2, TipoEventoContrato.ESTADO_ALTERADO,
                LedgerContratoHash.retratoContrato(negociacao) + ";detalhe=de=RASCUNHO;para=EM_NEGOCIACAO",
                um.getHashRegistro(), t.plusSeconds(1));
        ContratoFornecedor negociacaoAncorada = new ContratoFornecedor(42, 7, new BigDecimal("2500.00"),
                LocalDate.of(2026, 11, 6), StatusContrato.EM_NEGOCIACAO, false, false, 2,
                dois.getHashRegistro(), t);
        List<RegistroLedgerContrato> cadeia = List.of(um, dois);

        ResultadoIntegridadeLedger valida = LedgerContratoHash.verificar(cadeia, 2, dois.getHashRegistro());
        check(valida.isValido(), "CADEIA_INTACTA");
        check(LedgerContratoHash.retratoCorrespondeAoContrato(negociacaoAncorada, dois),
                "DADOS_ATUAIS_ANcorados_NO_ULTIMO_EVENTO");
        check(!LedgerContratoHash.verificar(cadeia, 2, um.getHashRegistro()).isValido(), "ANCORA_DETECTA_EVENTO_REMOVIDO");

        ContratoFornecedor alterado = new ContratoFornecedor(42, 7, new BigDecimal("3000.00"),
                LocalDate.of(2026, 11, 6), StatusContrato.EM_NEGOCIACAO, false, false, 2,
                dois.getHashRegistro(), t);
        check(!LedgerContratoHash.retratoCorrespondeAoContrato(alterado, dois),
                "VALOR_DO_CONTRATO_ALTERADO_DETECTADO");

        RegistroLedgerContrato payloadAlterado = new RegistroLedgerContrato(dois.getId(), dois.getContratoId(),
                dois.getSequencia(), dois.getTipo(), LedgerContratoHash.retratoContrato(negociacaoAncorada)
                        + ";detalhe=de=RASCUNHO;para=CANCELADO", dois.getHashConteudo(),
                dois.getHashAnterior(), dois.getHashRegistro(), dois.getDataHora());
        check(!LedgerContratoHash.verificar(List.of(um, payloadAlterado), 2, dois.getHashRegistro()).isValido(),
                "PAYLOAD_ALTERADO_DETECTADO");

        RegistroLedgerContrato anteriorAlterado = new RegistroLedgerContrato(dois.getId(), dois.getContratoId(),
                dois.getSequencia(), dois.getTipo(), dois.getPayload(), dois.getHashConteudo(),
                ContratoFornecedor.HASH_GENESIS, dois.getHashRegistro(), dois.getDataHora());
        check(!LedgerContratoHash.verificar(List.of(um, anteriorAlterado), 2, dois.getHashRegistro()).isValido(),
                "HASH_ANTERIOR_ALTERADO_DETECTADO");
        check(!LedgerContratoHash.verificar(List.of(um), 2, dois.getHashRegistro()).isValido(),
                "REGISTRO_REMOVIDO_DETECTADO_PELA_ANCORA");

        LocalDate hoje = LocalDate.of(2026, 10, 27);
        check(ContratoService.estaNaJanelaDeAlerta(hoje.plusDays(10), hoje), "ALERTA_NO_LIMITE_DE_10_DIAS");
        check(!ContratoService.estaNaJanelaDeAlerta(hoje.plusDays(11), hoje), "ALERTA_ANTES_DA_JANELA_RECUSADO");
        check(!ContratoService.estaNaJanelaDeAlerta(hoje.minusDays(1), hoje), "ALERTA_DE_CONTRATO_VENCIDO_RECUSADO");

        ContratoFornecedor contrato = new ContratoFornecedor(1, 7, new BigDecimal("2500.00"), hoje,
                StatusContrato.EM_EXECUCAO, true, false, 1, um.getHashRegistro(), t);
        check(ContratoService.podeLiberarPagamento(contrato, hoje), "PAGAMENTO_SIMULADO_EXIGE_NF_E_VENCIMENTO");
        ContratoFornecedor nfPendente = new ContratoFornecedor(1, 7, new BigDecimal("2500.00"), hoje,
                StatusContrato.EM_EXECUCAO, false, false, 1, um.getHashRegistro(), t);
        check(!ContratoService.podeLiberarPagamento(nfPendente, hoje), "PAGAMENTO_BLOQUEADO_SEM_NF_VALIDADA");
        ContratoFornecedor futuro = new ContratoFornecedor(1, 7, new BigDecimal("2500.00"), hoje.plusDays(1),
                StatusContrato.EM_EXECUCAO, true, false, 1, um.getHashRegistro(), t);
        check(!ContratoService.podeLiberarPagamento(futuro, hoje), "PAGAMENTO_BLOQUEADO_ANTES_DO_VENCIMENTO");
        ContratoFornecedor cancelado = new ContratoFornecedor(1, 7, new BigDecimal("2500.00"), hoje,
                StatusContrato.CANCELADO, true, false, 1, um.getHashRegistro(), t);
        check(!ContratoService.podeLiberarPagamento(cancelado, hoje), "PAGAMENTO_BLOQUEADO_EM_CONTRATO_CANCELADO");

        check(StatusContrato.RASCUNHO.podeIrPara(StatusContrato.EM_NEGOCIACAO), "TRANSICAO_RASCUNHO_NEGOCIACAO");
        check(!StatusContrato.CANCELADO.podeIrPara(StatusContrato.ASSINADO), "ESTADO_TERMINAL_NAO_REABRE");
        String[] chaves = {"contrato.titulo", "contrato.evento.contrato_criado", "contrato.ledger.valido",
                "contrato.pagamento.simulado", "contrato.erro.integridade_comprometida"};
        var pt = I18nUtils.getBundle(Locale.forLanguageTag("pt-BR"));
        var en = I18nUtils.getBundle(Locale.US);
        for (String chave : chaves) check(pt.containsKey(chave) && en.containsKey(chave), "I18N_" + chave);
        System.out.println("D13_LEDGER_OK checks=" + passed);
    }
}
