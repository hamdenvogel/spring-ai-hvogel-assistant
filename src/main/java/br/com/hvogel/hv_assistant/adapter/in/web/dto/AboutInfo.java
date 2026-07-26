package br.com.hvogel.hv_assistant.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Payload JSON do endpoint {@code GET /api/sobre} (mesmo formato conceitual do FilePack).
 */
public record AboutInfo(
        String autor,
        String versao,
        String aplicacao,
        String descricao,
        Instant dataHoraAlteracao,
        Instant consultadoEm,
        String javaVersion,
        String springBootVersion,
        List<String> formatosSuportados,
        List<String> endpoints) {
}
