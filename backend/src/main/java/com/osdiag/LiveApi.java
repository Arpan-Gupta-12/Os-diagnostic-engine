package com.osdiag;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LiveApi {

    private final Monitor monitor;

    public LiveApi(Monitor monitor) {
        this.monitor = monitor;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return monitor.info();
    }

    @GetMapping("/live")
    public List<Sample> live() {
        return monitor.history();
    }

    @GetMapping("/procs")
    public List<Proc> procs(@RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "mem") String sort) {
        Comparator<Proc> order = sort.equals("cpu")
                ? Comparator.comparing((Proc p) -> p.cpu() == null ? -1 : p.cpu()).reversed()
                : Comparator.comparing(Proc::mem).reversed();
        String text = search.toLowerCase(Locale.ROOT);
        return monitor.procs().stream()
                .filter(p -> p.name() != null && p.name().toLowerCase(Locale.ROOT).contains(text))
                .sorted(order).limit(100).toList();
    }
}
