package br.com.hvogel.hv_assistant.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class AboutInfoProviderTest {

    @Test
    void usesBuildPropertiesWhenPresent() {
        BuildProperties build = org.mockito.Mockito.mock(BuildProperties.class);
        org.mockito.Mockito.when(build.getVersion()).thenReturn("0.2.0");
        org.mockito.Mockito.when(build.getTime()).thenReturn(Instant.parse("2026-07-26T12:00:00Z"));

        AboutInfoProvider provider = new AboutInfoProvider(
                Optional.of(build),
                "Hvogel Tecnologia Ltda.",
                "HV Assistant",
                "Assistente virtual de RH com RAG",
                "0.2.0");

        var about = provider.getAbout();

        assertThat(about.autor()).isEqualTo("Hvogel Tecnologia Ltda.");
        assertThat(about.versao()).isEqualTo("0.2.0");
        assertThat(about.aplicacao()).isEqualTo("HV Assistant");
        assertThat(about.descricao()).contains("RAG");
        assertThat(about.dataHoraAlteracao()).isEqualTo(Instant.parse("2026-07-26T12:00:00Z"));
        assertThat(about.consultadoEm()).isBeforeOrEqualTo(Instant.now());
        assertThat(about.javaVersion()).isNotBlank();
        assertThat(about.springBootVersion()).isNotBlank();
        assertThat(about.formatosSuportados()).contains("RAG", "SSE", "PDF");
        assertThat(about.endpoints()).anyMatch(e -> e.contains("/api/sobre"));
        assertThat(about.endpoints()).anyMatch(e -> e.contains("/chat/stream"));
    }

    @Test
    void stripsSnapshotSuffixForDisplay() {
        BuildProperties build = org.mockito.Mockito.mock(BuildProperties.class);
        org.mockito.Mockito.when(build.getVersion()).thenReturn("0.0.1-SNAPSHOT");
        org.mockito.Mockito.when(build.getTime()).thenReturn(Instant.parse("2026-07-26T12:00:00Z"));

        AboutInfoProvider provider = new AboutInfoProvider(
                Optional.of(build),
                "Hvogel Tecnologia Ltda.",
                "HV Assistant",
                "Assistente virtual de RH com RAG",
                "0.2.0");

        assertThat(provider.getAbout().versao()).isEqualTo("0.0.1");
    }

    @Test
    void toDisplayVersion_handlesVariants() {
        assertThat(AboutInfoProvider.toDisplayVersion("0.0.1-SNAPSHOT")).isEqualTo("0.0.1");
        assertThat(AboutInfoProvider.toDisplayVersion("0.0.1-snapshot")).isEqualTo("0.0.1");
        assertThat(AboutInfoProvider.toDisplayVersion("1.2.3")).isEqualTo("1.2.3");
        assertThat(AboutInfoProvider.toDisplayVersion("  ")).isEqualTo("0.2.0");
        assertThat(AboutInfoProvider.toDisplayVersion(null)).isEqualTo("0.2.0");
    }

    @Test
    void usesFallbackWhenBuildPropertiesMissing() {
        AboutInfoProvider provider = new AboutInfoProvider(
                Optional.empty(),
                "Hvogel Tecnologia Ltda.",
                "HV Assistant",
                "Assistente virtual de RH com RAG",
                "0.2.0");

        var about = provider.getAbout();

        assertThat(about.versao()).isEqualTo("0.2.0");
        assertThat(about.dataHoraAlteracao()).isEqualTo(Instant.parse("2026-07-25T00:00:00Z"));
    }
}
