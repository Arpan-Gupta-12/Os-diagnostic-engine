package com.osdiag;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** The limits used by the rules, and the demo switch. */
@RestController
@RequestMapping("/api")
public class SettingsApi {

    private final LimitsRepo limits;
    private final Demo demo;
    private final Monitor monitor;

    public SettingsApi(LimitsRepo limits, Demo demo, Monitor monitor) {
        this.limits = limits;
        this.demo = demo;
        this.monitor = monitor;
    }

    @GetMapping("/limits")
    public Limits limits() {
        return limits.get();
    }

    @PutMapping("/limits")
    public Limits save(@Valid @RequestBody Limits changed) {
        changed.setId(1L);
        return limits.save(changed);
    }

    @DeleteMapping("/limits")
    public Limits reset() {
        limits.deleteAll();
        return new Limits();
    }

    @PostMapping("/demo")
    public Map<String, String> demo(@RequestParam String mode) {
        if (!List.of("off", "healthy", "slow").contains(mode)) {
            throw new IllegalArgumentException("Demo mode must be off, healthy or slow.");
        }
        demo.set(mode);
        monitor.clearHistory();
        return Map.of("mode", mode);
    }
}
