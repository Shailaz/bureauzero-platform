package com.bureauzero.core.controller;

import com.bureauzero.core.entity.CitizenApplication;
import com.bureauzero.core.repository.ApplicationRepository;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class MissionControlController {

    private final ApplicationRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String pythonAiUrl;

    public MissionControlController(
            ApplicationRepository repository,
            @Value("${bureauzero.ai.base-url:http://127.0.0.1:8001}") String pythonAiUrl) {
        this.repository = repository;
        this.pythonAiUrl = pythonAiUrl;
    }

    @PostMapping("/ingest-document")
    public ResponseEntity<?> ingestCitizenDocument(@RequestParam("file") MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity response = restTemplate.postForEntity(
                pythonAiUrl + "/ai/extract-profile", requestEntity, Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/dispatch-agent")
    public ResponseEntity<?> dispatchBrowserAgent(@RequestBody Map<String, Object> payload) {
        ResponseEntity response = restTemplate.postForEntity(
                pythonAiUrl + "/ai/execute-workflow", payload, Map.class);
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        if (result != null && "COMPLETED".equals(result.get("status"))) {
            String summary = String.valueOf(result.getOrDefault("final_receipt_summary", ""));
            if (summary.contains("Official Receipt ID:")) {
                String receiptId = summary.lines()
                        .filter(line -> line.startsWith("Official Receipt ID:"))
                        .map(line -> line.substring("Official Receipt ID:".length()).trim())
                        .findFirst().orElse(null);
                if (receiptId != null) {
                    return repository.findById(receiptId)
                            .<ResponseEntity<?>>map(application -> ResponseEntity.ok(Map.of(
                                    "status", result.get("status"),
                                    "final_receipt_summary", summary,
                                    "receipt", application,
                                    "agent_reasoning_trace", result.get("agent_reasoning_trace")
                            )))
                            .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                    .body(Map.of("error", "The portal reported success but its receipt was not persisted.")));
                }
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/registry-ledger")
    public List<CitizenApplication> getLiveRegistryLedger() {
        return repository.findAll();
    }
}
