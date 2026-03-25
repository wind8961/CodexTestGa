package com.example.ga.planner.gantt;

import com.example.ga.planner.api.GanttTask;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public String toPngBase64(List<GanttTask> tasks) {
        if (tasks.isEmpty()) {
            return "";
        }

        List<GanttTask> sorted = tasks.stream()
                .sorted(Comparator.comparing(GanttTask::lane).thenComparing(GanttTask::startEpochSecond))
                .toList();

        long minStart = sorted.stream().mapToLong(GanttTask::startEpochSecond).min().orElse(0L);
        long maxEnd = sorted.stream().mapToLong(GanttTask::endEpochSecond).max().orElse(minStart + 1);
        long span = Math.max(1, maxEnd - minStart);

        Map<String, Integer> laneIndex = new LinkedHashMap<>();
        for (GanttTask task : sorted) {
            laneIndex.computeIfAbsent(task.lane(), lane -> laneIndex.size());
        }

        int marginLeft = 180;
        int marginTop = 60;
        int rowHeight = 48;
        int barHeight = 22;
        int timelineWidth = 1100;
        int width = marginLeft + timelineWidth + 60;
        int height = marginTop + laneIndex.size() * rowHeight + 80;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(33, 33, 33));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Satellite Joint Plan Gantt", 30, 35);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(120, 120, 120));
        g.drawLine(marginLeft, marginTop, marginLeft + timelineWidth, marginTop);
        g.drawLine(marginLeft, marginTop + laneIndex.size() * rowHeight, marginLeft + timelineWidth, marginTop + laneIndex.size() * rowHeight);

        for (int i = 0; i <= 10; i++) {
            int x = marginLeft + (timelineWidth * i / 10);
            long t = minStart + (span * i / 10);
            g.drawLine(x, marginTop - 5, x, marginTop + laneIndex.size() * rowHeight + 5);
            g.drawString(String.valueOf(t), x - 15, marginTop - 12);
        }

        g.setStroke(new BasicStroke(1.5f));
        for (Map.Entry<String, Integer> entry : laneIndex.entrySet()) {
            int y = marginTop + entry.getValue() * rowHeight;
            g.setColor(new Color(90, 90, 90));
            g.drawString(entry.getKey(), 35, y + barHeight);
            g.setColor(new Color(220, 220, 220));
            g.drawLine(marginLeft, y + rowHeight - 3, marginLeft + timelineWidth, y + rowHeight - 3);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (GanttTask task : sorted) {
            int row = laneIndex.get(task.lane());
            int y = marginTop + row * rowHeight + 8;
            int x1 = marginLeft + (int) (((task.startEpochSecond() - minStart) * 1.0 / span) * timelineWidth);
            int x2 = marginLeft + (int) (((task.endEpochSecond() - minStart) * 1.0 / span) * timelineWidth);
            int w = Math.max(2, x2 - x1);

            g.setColor(parseColor(task.color()));
            g.fillRoundRect(x1, y, w, barHeight, 8, 8);
            g.setColor(new Color(50, 50, 50));
            g.drawRoundRect(x1, y, w, barHeight, 8, 8);
            g.drawString(task.label(), x1 + 4, y + 15);
        }

        g.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception ex) {
            return new Color(31, 119, 180);
        }
    }
}
