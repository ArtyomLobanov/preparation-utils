package org.ml_methods_group.preparation;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CommonUtils {

    public static void processIndex(Path index, IndexProcessor processor) throws IOException {
        try (Scanner scanner = new Scanner(index)) {
            final String headerLine = scanner.nextLine();
            final List<String> columns = Arrays.asList(headerLine.split(","));
            final int handleIndex = columns.indexOf("handle");
            final int participantIndex = columns.indexOf("participant_id");
            final int contestIndex = columns.indexOf("contest_id");
            final int problemIndex = columns.indexOf("problem_id");
            final int problemLiteralIndex = columns.indexOf("problem_lit");
            final int solutionIndex = columns.indexOf("solution_id");
            final int pathIndex = columns.indexOf("path");
            while (scanner.hasNext()) {
                final String[] values = scanner.nextLine().split(",");
                processor.process(
                        values[handleIndex],
                        values[participantIndex],
                        values[contestIndex],
                        values[problemIndex],
                        values[problemLiteralIndex],
                        values[solutionIndex],
                        values[pathIndex]
                );
            }
        }
    }

    public static List<String> readAllLines(Path path) throws IOException {
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
        }
        return lines;
    }

    public static void writeAllLines(Path path, List<String> lines) throws IOException {
        try (PrintWriter writer = new PrintWriter(path.toFile(), StandardCharsets.UTF_8)) {
            lines.forEach(writer::println);
        }
    }

    public interface IndexProcessor {
        void process(String handle, String participantId, String contestId, String problemId,
                     String problemLiteral, String solutionId, String path);
    }
}
