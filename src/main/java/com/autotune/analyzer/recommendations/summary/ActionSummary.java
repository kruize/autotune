package com.autotune.analyzer.recommendations.summary;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ActionSummary {
    public static class Section {
        public int count;
        public List<String> workloadNames;
        public Section() {
            count = 0;
            workloadNames = new ArrayList<>();
        }
    }

    public static class Resource {
        public Section cpu;
        public Section memory;

    }
    public Resource idle = new Resource();
    public Resource optimized = new Resource();
    public Resource critical = new Resource();
    public Resource optimizable = new Resource();
    public Resource total = new Resource();


    public ActionSummary() {
        idle.cpu = new Section();
        optimized.cpu = new Section();
        critical.cpu = new Section();
        optimizable.cpu = new Section();
        total.cpu = new Section();

        idle.memory = new Section();
        optimized.memory = new Section();
        critical.memory = new Section();
        optimizable.memory = new Section();
        total.memory = new Section();
    }

    public static ActionSummary merge(ActionSummary summary1, ActionSummary summary2) {

        ActionSummary merged = new ActionSummary();

        // Merge CPU
        mergeSection(merged.idle.cpu, summary1.idle.cpu);
        mergeSection(merged.idle.cpu, summary2.idle.cpu);

        mergeSection(merged.optimized.cpu, summary1.optimized.cpu);
        mergeSection(merged.optimized.cpu, summary2.optimized.cpu);

        mergeSection(merged.critical.cpu, summary1.critical.cpu);
        mergeSection(merged.critical.cpu, summary2.critical.cpu);

        mergeSection(merged.optimizable.cpu, summary1.optimizable.cpu);
        mergeSection(merged.optimizable.cpu, summary2.optimizable.cpu);

        // Merge Memory
        mergeSection(merged.idle.memory, summary1.idle.memory);
        mergeSection(merged.idle.memory, summary2.idle.memory);

        mergeSection(merged.optimized.memory, summary1.optimized.memory);
        mergeSection(merged.optimized.memory, summary2.optimized.memory);

        mergeSection(merged.critical.memory, summary1.critical.memory);
        mergeSection(merged.critical.memory, summary2.critical.memory);

        mergeSection(merged.optimizable.memory, summary1.optimizable.memory);
        mergeSection(merged.optimizable.memory, summary2.optimizable.memory);

        // Merge Total CPU and Memory
        mergeSection(merged.total.cpu, summary1.total.cpu);
        mergeSection(merged.total.cpu, summary2.total.cpu);

        mergeSection(merged.total.memory, summary1.total.memory);
        mergeSection(merged.total.memory, summary2.total.memory);

        return merged;
    }

    private static void mergeSection(Section target, Section source) {
        target.count += source.count;
        target.workloadNames.addAll(source.workloadNames);
    }

    public Resource getIdle() {
        return idle;
    }

    public void setIdle(Resource idle) {
        this.idle = idle;
    }

    public Resource getCritical() {
        return critical;
    }

    public void setCritical(Resource critical) {
        this.critical = critical;
    }

    public Resource getTotal() {
        return total;
    }

    public void setTotal(Resource total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "ActionSummary{" +
                "idle=" + idle +
                ", optimized=" + optimized +
                ", critical=" + critical +
                ", optimizable=" + optimizable +
                ", total.cpu=" + total.cpu +
                ", total.memory=" + total.memory +
                '}';
    }
}
