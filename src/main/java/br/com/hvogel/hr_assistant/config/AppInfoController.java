package br.com.hvogel.hr_assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppInfoController {

    @Value("${app.info.version}")
    private String version;

    @Value("${app.info.developer}")
    private String developer;

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "version", version,
                "developer", developer,
                "product", "Assistente de RH");
    }
}
