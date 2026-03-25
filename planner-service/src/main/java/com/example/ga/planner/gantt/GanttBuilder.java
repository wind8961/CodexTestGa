package com.example.ga.planner.gantt;

import com.example.ga.planner.api.GanttTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GanttBuilder {
    private static final DateTimeFormatter MERMAID_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public String toMermaid(List<GanttTask> tasks) {
        StringBuilder sb = new StringBuilder("gantt\n    title Satellite Multi-Plan\n    dateFormat  YYYY-MM-DD HH:mm:ss\n");
        tasks.forEach(task -> sb.append("    section ").append(task.lane()).append("\n")
                .append("    ").append(task.label()).append(" : ")
                .append(MERMAID_TIME.format(Instant.ofEpochSecond(task.startEpochSecond()))).append(", ")
                .append(MERMAID_TIME.format(Instant.ofEpochSecond(task.endEpochSecond()))).append("\n"));
        return sb.toString();
    }
}
