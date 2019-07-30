package org.ml_methods_group.preparation;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class ProcessObserver {
    private final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    private final AtomicInteger loadedCount = new AtomicInteger(0);
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger writtenCount = new AtomicInteger(0);
    private final String unitName;
    private final int total;
    private final int outputFrequency;
    private final PrintStream output;

    public ProcessObserver(String unitName, int total) {
        this(unitName, total, total / 100);
    }

    public ProcessObserver(String unitName, int total, int outputFrequency) {
        this(unitName, total, outputFrequency, System.out);
    }

    public ProcessObserver(String unitName, int total, int outputFrequency, PrintStream output) {
        this.unitName = unitName;
        this.total = total;
        this.outputFrequency = outputFrequency;
        this.output = output;
    }

    public void unitLoaded() {
        final int value = loadedCount.incrementAndGet();
        if (value == total || value % outputFrequency == 0) {
            output.printf("%s: %s loaded: %d/%d ~ %d%%\n",
                    formatter.format(new Date()),
                    unitName,
                    value,
                    total,
                    value * 100 / total);
        }
    }

    public void unitProcessed() {
        final int value = processedCount.incrementAndGet();
        if (value == total || value % outputFrequency == 0) {
            output.printf("%s: %s processed: %d/%d ~ %d%%\n",
                    formatter.format(new Date()),
                    unitName,
                    value,
                    total,
                    value * 100 / total);
        }
    }

    public void unitWritten() {
        final int value = writtenCount.incrementAndGet();
        if (value == total || value % outputFrequency == 0) {
            output.printf("%s: %s written: %d/%d ~ %d%%\n",
                    formatter.format(new Date()),
                    unitName,
                    value,
                    total,
                    value * 100 / total);
        }
    }
}
