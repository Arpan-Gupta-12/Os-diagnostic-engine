package com.osdiag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Startup programs (read only) and the power plan, which is only switched when the user asks for it. */
@RestController
@RequestMapping("/api")
public class StartupApi {

    private final Demo demo;
    private final OsAdapter os = OsAdapter.current();

    public StartupApi(Demo demo) {
        this.demo = demo;
    }

    @GetMapping("/startup")
    public Map<String, Object> startup() {
        List<OsAdapter.Startup> items = demo.on() ? demo.startup() : os.startupItems();
        String power = demo.on() ? demo.power() : os.powerPlan();
        Map<String, Object> m = new HashMap<>();
        m.put("items", items);
        m.put("power", power);
        m.put("tips", tips(items, power));
        return m;
    }

    @PostMapping("/power")
    public Map<String, Object> power(@RequestParam String plan) {
        if (!List.of("balanced", "performance").contains(plan)) {
            throw new IllegalArgumentException("Power plan must be balanced or performance.");
        }
        if (demo.on()) {
            return Map.of("ok", true, "message", "Demo: the power plan would be switched to " + plan + ". Nothing was changed.");
        }
        String before = os.powerPlan();
        boolean done = os.setPowerPlan(plan);
        String after = os.powerPlan();
        boolean changed = done && after != null && !after.equals(before);
        return Map.of("ok", changed, "message", changed
                ? "The power plan changed from " + before + " to " + after + ". You can change it back in the system power settings."
                : "The power plan was not changed" + (after == null ? "." : " (it is still " + after + ")."));
    }

    static List<String> tips(List<OsAdapter.Startup> items, String power) {
        List<String> tips = new ArrayList<>();
        if (items.size() > 8) {
            tips.add(items.size() + " programs start together with your PC. Each one makes the start slower and uses "
                    + "memory. Open Task Manager (Startup apps) on Windows, or Startup Applications on Linux, and turn "
                    + "off the ones you do not need.");
        }
        String plan = power == null ? "" : power.toLowerCase();
        if (plan.contains("saver")) {
            tips.add("Power saver is on. It slows the PC down to save battery. Switch to Balanced when you want full speed.");
        } else if (plan.contains("performance")) {
            tips.add("High performance keeps the PC fast but uses more power and makes it hotter. Balanced is enough for most work.");
        }
        return tips;
    }
}
