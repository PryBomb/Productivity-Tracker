import java.math.BigDecimal;

/**
 * Tracker.java
 * Represents ONE improvement goal (one row of the "goals" table).
 */
public class Tracker {

    private int goalId;
    private String title;
    private String category;
    private String targetDate; // can be null if no deadline
    private int progress;      // 0 to 100
    private String status;     // ACTIVE or COMPLETED
    private int logCount;      // how many daily entries this goal has
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String metricUnit;

    public Tracker(int goalId, String title, String category, String targetDate,
                int progress, String status, int logCount, BigDecimal targetValue,
                BigDecimal currentValue, String metricUnit) {
        this.goalId = goalId;
        this.title = title;
        this.category = category;
        this.targetDate = targetDate;
        this.progress = progress;
        this.status = status;
        this.logCount = logCount;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.metricUnit = metricUnit;
    }

    public int getGoalId() {
        return goalId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public int getProgress() {
        return progress;
    }

    public String getStatus() {
        return status;
    }

    public int getLogCount() {
        return logCount;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public String getMetricUnit() {
        return metricUnit;
    }

    // Draws a bar like [#####-----] for the progress
    private String progressBar() {
        int filled = progress / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "#" : "-");
        }
        bar.append("]");
        return bar.toString();
    }

    @Override
    public String toString() {
        String due = (targetDate == null) ? "No deadline" : targetDate;
        return String.format("#%-3d %-24s %-12s %s %3d%%  %-9s %s / %s %s  Due: %-11s Logs: %d",
                goalId, title, category, progressBar(), progress, status,
                currentValue.stripTrailingZeros().toPlainString(),
                targetValue.stripTrailingZeros().toPlainString(), metricUnit, due, logCount);
    }
}