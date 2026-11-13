package com.osdiag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The optimization plan. kind "clean" can be run after the user approves it, "advice" only explains,
 * "unsupported" explains why this app does not do it.
 */
public class Plan {

    public record Action(String key, String title, String text, String kind, List<String> details, int files, long bytes,
                         List<String> closable) {
        Action(String key, String title, String text, String kind, List<String> details) {
            this(key, title, text, kind, details, 0, 0, List.of());
        }
    }

    static Action clean(String key, String title, String text, int files, long bytes, String folder) {
        String note = files == 0 ? "Nothing to clean here." : text;
        return new Action(key, title, note, "clean", List.of(folder), files, bytes, List.of());
    }

    /** Advice that needs no approval. sample can be null when nothing has been measured yet. */
    static List<Action> advice(Sample sample, List<Proc> procs, Limits limits, List<OsAdapter.Startup> startup, String power,
                               List<Large.Entry> big) {
        List<Action> list = new ArrayList<>();
        list.add(startupReview(startup));
        if (sample != null && isBusy(sample, limits)) {
            list.add(heavyPrograms(procs));
        }
        list.add(powerReview(power));
        if (!big.isEmpty()) {
            list.add(bigFiles(big));
        }
        if (sample != null && sample.mem() != null && sample.paging() != null
                && sample.mem() >= limits.getMem() && sample.paging() >= limits.getPaging()) {
            list.add(new Action("memory", "Force the system to free memory",
                    "Dropping system caches or \"RAM cleaning\" needs administrator rights and only helps for a moment, "
                            + "because the system fills the cache again. This app does not do it.",
                    "unsupported", List.of("If memory is often full, closing programs or adding memory is the real fix.")));
        }
        if (sample != null) {
            hardware(sample, limits).ifPresent(list::add);
        }
        return list;
    }

    private static boolean isBusy(Sample s, Limits limits) {
        return (s.cpu() != null && s.cpu() >= limits.getCpu()) || (s.mem() != null && s.mem() >= limits.getMem());
    }

    private static Action startupReview(List<OsAdapter.Startup> startup) {
        List<String> names = startup.stream().limit(8).map(s -> s.name() + " (" + s.where() + ")").toList();
        return new Action("startup", "Review startup programs",
                startup.size() + " programs start together with your PC. We never change these for you.",
                "advice", names);
    }

    /** Names the heaviest programs. The ones that may be closed get a button on the screen, which asks first. */
    private static Action heavyPrograms(List<Proc> procs) {
        Map<String, Long> memory = new LinkedHashMap<>();
        procs.forEach(p -> memory.merge(p.name(), p.mem(), Long::sum));
        List<String> details = new ArrayList<>();
        memory.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(3)
                .forEach(e -> details.add(String.format("%s: %.1f GB of memory", e.getKey(), e.getValue() / 1073741824.0)));
        procs.stream().filter(p -> p.cpu() != null).sorted(Comparator.comparing(Proc::cpu).reversed()).limit(3)
                .forEach(p -> details.add(String.format("%s: %.0f%% of the processor", p.name(), p.cpu())));
        List<String> closable = new ArrayList<>(memory.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey).filter(n -> procs.stream().anyMatch(p -> p.name().equals(n) && CloseApi.closable(p)))
                .limit(3).toList());
        return new Action("programs", "Free up the heavy programs",
                "These programs use the most. Closing one gives the PC its memory and processor time back at once. "
                        + "You press the button and confirm, because unsaved work in it is lost.",
                "advice", details, 0, 0, closable);
    }

    private static Action bigFiles(List<Large.Entry> big) {
        List<String> details = big.stream().map(e -> String.format("%.1f GB  %s  (changed %s)", e.bytes() / 1073741824.0,
                e.path(), e.changed().toString().substring(0, 10))).toList();
        return new Action("large", "Big files you may not need",
                "The biggest files in your Downloads and Desktop folders. These are your files, so this app never deletes "
                        + "them. Move or delete the ones you no longer need to free space.", "advice", details);
    }

    private static Action powerReview(String power) {
        if (power == null) {
            return new Action("power", "Review power settings", "The power plan could not be read on this PC.",
                    "unsupported", List.of());
        }
        String plan = power.toLowerCase();
        boolean saving = plan.contains("saver") || plan.contains("powersave");
        String text = saving
                ? "Your PC uses a power-saving plan (" + power + "). It saves battery but makes the PC slower. "
                        + "Switching to Balanced is usually a clear speed-up."
                : "Current power plan: " + power + ". No change suggested.";
        return new Action(saving ? "power-saver" : "power", "Review power settings", text, "advice", List.of());
    }

    private static java.util.Optional<Action> hardware(Sample s, Limits limits) {
        List<String> details = new ArrayList<>();
        if (s.disk() != null && s.disk() >= limits.getDiskBusy()) {
            details.add("The disk is very busy. Check its health with the maker's tool. An SSD is much faster than a hard disk.");
        }
        if (s.temp() != null && s.temp() >= limits.getTemp()) {
            details.add("The processor is hot. Clean dust from fans and vents and keep air flowing around the PC.");
        }
        return details.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(new Action("hardware",
                "Hardware checks", "Some problems come from the hardware, which software cannot fix safely.",
                "advice", details));
    }
}
