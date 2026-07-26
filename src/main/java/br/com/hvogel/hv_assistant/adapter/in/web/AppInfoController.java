package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.adapter.in.web.dto.AboutInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppInfoController {

    private final AboutInfoProvider aboutInfoProvider;

    public AppInfoController(AboutInfoProvider aboutInfoProvider) {
        this.aboutInfoProvider = aboutInfoProvider;
    }

    /**
     * Metadados ricos para o modal "Sobre" (mesmo formato do FilePack).
     */
    @GetMapping("/api/sobre")
    public AboutInfo sobre() {
        return aboutInfoProvider.getAbout();
    }

    /**
     * Resumo leve usado pelo footer da UI (mesma fonte de versão do Sobre).
     */
    @GetMapping("/api/info")
    public Map<String, String> info() {
        AboutInfo about = aboutInfoProvider.getAbout();
        return Map.of(
                "version", about.versao(),
                "developer", about.autor(),
                "product", "Assistente de RH");
    }
}
