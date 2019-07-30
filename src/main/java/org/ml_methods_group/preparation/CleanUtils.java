package org.ml_methods_group.preparation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Predicate;

public class CleanUtils {

    public static void copyFiles(Path src, Path dst, Predicate<Path> filter) throws IOException {
        Files.walk(src)
                .peek(path -> {
                    if (Files.isDirectory(path)) {
                        final Path folder = dst.resolve(src.relativize(path));
                        if (!Files.exists(folder)) {
                            try {
                                Files.createDirectory(folder);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                })
                .filter(Files::isRegularFile)
                .filter(filter)
                .map(src::relativize)
                .forEach(path -> {
                    try {
                        Files.copy(src.resolve(path), dst.resolve(path));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static void cleanSolutions(Path src, Path dst, Path index) throws IOException {
        final Map<String, List<Path>> paths = new HashMap<>();
        final Map<String, List<Integer>> problems = new HashMap<>();
        if (!Files.exists(dst)) {
            Files.createDirectories(dst);
        }
        CommonUtils.processIndex(index,
                (handle, participantId, contestId, problemId, problemLiteral, solutionId, path) -> {
                    final Path filePath = Paths.get(contestId)
                            .resolve(problemLiteral)
                            .resolve("submissions")
                            .resolve(solutionId + ".program.cpp");
                    paths.computeIfAbsent(handle, x -> new ArrayList<>())
                            .add(filePath);
                    problems.computeIfAbsent(handle, x -> new ArrayList<>())
                            .add(Integer.parseInt(problemId));
                });
        System.out.println("Index loaded: " + paths.size() + " users");
        final ProcessObserver state = new ProcessObserver("Users", paths.size());
        paths.keySet().parallelStream()
                .map(handle -> {
                    try {
                        final SolutionsWrapper solutions = SolutionsWrapper.load(handle, src, paths.get(handle), problems.get(handle));
                        state.unitLoaded();
                        return solutions;
                    } catch (IOException e) {
                        System.out.println("Failed to load: " + handle
                                + ", cause: " + e.getClass() + " " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(solutions -> {
                    try {
                        final ResultWrapper result = detectTemplates(solutions);
                        state.unitProcessed();
                        return result;
                    } catch (CloneNotSupportedException e) {
                        System.out.println("Failed to detectTemplates: " + solutions.handle
                                + ", cause: " + e.getClass() + " " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(result -> {
                    try {
                        write(result, dst);
                        state.unitWritten();
                    } catch (IOException e) {
                        System.out.println("Failed to write: " + result.solutions.handle
                                + ", cause: " + e.getClass() + " " + e.getMessage());
                    }
                });
    }

    private static ResultWrapper detectTemplates(SolutionsWrapper solutions) throws CloneNotSupportedException {
        final int n = solutions.paths.size();
        final int[] lastOk = new int[n];
        final int[] bounds = new int[n];
        final int[] commentsBalance = new int[n];
        final int[] bracketsBalance = new int[n];
        final MessageDigest[] digests = new MessageDigest[n];
        final ByteArrayWrapper[] wrappers = new ByteArrayWrapper[n];
        for (int i = 0; i < n; i++) {
            bounds[i] = -1;
            try {
                digests[i] = MessageDigest.getInstance("SHA-256");
                wrappers[i] = new ByteArrayWrapper();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        int count = n;
        for (int k = 0; count != 0; k++) {
            final HashMap<ByteArrayWrapper, HashSet<Integer>> counters = new HashMap<>();
            for (int i = 0; i < n; i++) {
                if (bounds[i] != -1) {
                    continue;
                }
                final List<String> content = solutions.contents.get(i);
                if (content.size() <= k || content.get(k).contains("main") || content.get(k).contains("solve")) {
                    bounds[i] = lastOk[i];
                    count--;
                    continue;
                }
                digests[i].update(solutions.contents.get(i).get(k).getBytes());
                wrappers[i].update(digests[i]);
                counters.computeIfAbsent(wrappers[i], x -> new HashSet<>()).add(solutions.problems.get(i));
            }
            for (int i = 0; i < n; i++) {
                if (bounds[i] != -1) {
                    continue;
                }
                if (counters.get(wrappers[i]).size() <= 2) {
                    bounds[i] = lastOk[i];
                    count--;
                    continue;
                }
                final String line = solutions.contents.get(i).get(k);
                for (int s = 0; s < line.length(); s++) {
                    if (line.startsWith("/*", s)) {
                        commentsBalance[i]++;
                    } else if (line.startsWith("*/", s)) {
                        commentsBalance[i]--;
                    } else if (line.charAt(s) == '{') {
                        bracketsBalance[i]++;
                    } else if (line.charAt(s) == '}') {
                        bracketsBalance[i]--;
                    }
                }
                if (commentsBalance[i] == 0 && bracketsBalance[i] == 0) {
                    lastOk[i] = k + 1;
                }
            }
        }
        return new ResultWrapper(solutions, bounds);
    }

    private static void write(ResultWrapper results, Path dst) throws IOException {
        final int n = results.solutions.paths.size();
        final SolutionsWrapper solutions = results.solutions;
        for (int i = 0; i < n; i++) {
            final List<String> content = solutions.contents.get(i);
            CommonUtils.writeAllLines(dst.resolve(solutions.paths.get(i)),
                    content.subList(results.bounds[i], content.size()));
            if (results.bounds[i] > 0) {
                CommonUtils.writeAllLines(dst.resolve(solutions.paths.get(i) + "$template.txt"),
                        content.subList(0, results.bounds[i]));
            }
        }
    }

    private static class ByteArrayWrapper {
        private byte[] array;
        private int hash;

        void update(MessageDigest digest) throws CloneNotSupportedException {
            this.array = ((MessageDigest) digest.clone()).digest();
            this.hash = Arrays.hashCode(array);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return ((ByteArrayWrapper) obj).hash == hash && Arrays.equals(((ByteArrayWrapper) obj).array, array);
        }
    }

    private static class ResultWrapper {
        private final SolutionsWrapper solutions;
        private final int[] bounds;

        private ResultWrapper(SolutionsWrapper solutions, int[] bounds) {
            this.solutions = solutions;
            this.bounds = bounds;
        }

    }

    private static class SolutionsWrapper {
        private final String handle;
        private final List<Path> paths;
        private final List<Integer> problems;
        private final List<List<String>> contents;

        private SolutionsWrapper(String handle, List<Path> paths, List<Integer> problems, List<List<String>> contents) {
            this.handle = handle;
            this.paths = paths;
            this.problems = problems;
            this.contents = contents;
        }

        private static SolutionsWrapper load(String handle, Path src,
                                             List<Path> paths, List<Integer> problems) throws IOException {
            final List<List<String>> contents = new ArrayList<>();
            for (Path path : paths) {
                contents.add(CommonUtils.readAllLines(src.resolve(path)));
            }
            return new SolutionsWrapper(handle, paths, problems, contents);
        }
    }
}
