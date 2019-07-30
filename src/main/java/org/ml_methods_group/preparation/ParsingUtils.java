package org.ml_methods_group.preparation;

import astminer.parse.antlr.AntlrUtilKt;
import astminer.parse.antlr.SimpleNode;
import com.google.gson.Gson;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ml_methods_group.parser.CPP14Lexer;
import org.ml_methods_group.parser.CPP14Parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ParsingUtils {
    public static void parseTokens(Path src, Path dst, Path index) throws IOException {
        final Map<Path, Path> files = new HashMap<>();
        if (!Files.exists(dst)) {
            Files.createDirectories(dst);
        }
        CommonUtils.processIndex(index,
                (handle, participantId, contestId, problemId, problemLiteral, solutionId, path) -> {
                    final Path srcPath = src.resolve(contestId).resolve(problemLiteral).resolve("submissions")
                            .resolve(solutionId + ".program.cpp");
                    final String filename = contestId + "." + problemLiteral + "."
                            + solutionId + ".program.cpp&tokens.csv";
                    final Path targetPath = dst.resolve(contestId).resolve(filename);
                    files.put(srcPath, targetPath);
                });
        final ProcessObserver state = new ProcessObserver("Solutions", files.size());
        final List<Map.Entry<Path, Path>> buffer = new ArrayList<>(files.entrySet());
        Collections.shuffle(buffer, new Random(239566));
        System.out.println("Index loaded: " + buffer.size() + " solutions found");
        buffer.parallelStream()
                .map(entry -> {
                    try {
                        final String code = Files.readString(entry.getKey(), UTF_8);
                        state.unitLoaded();
                        return new CodeWrapper(code, entry.getValue());
                    } catch (IOException e) {
                        System.out.println("Failed to load: " + entry.getKey()
                                + ", cause: " + e.getClass() + " " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(wrapper -> {
                    final CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString(wrapper.code));
                    lexer.removeErrorListeners();
                    final int identifierType = lexer.getTokenType("Identifier");
                    final Map<String, Long> identifiers = lexer.getAllTokens().stream()
                            .filter(token -> token.getType() == identifierType)
                            .collect(Collectors.groupingBy(Token::getText, Collectors.counting()));
                    state.unitProcessed();
                    return new TokensWrapper(identifiers, wrapper.dst);
                })
                .forEach(result -> {
                    if (!Files.exists(result.dst.getParent())) {
                        try {
                            Files.createDirectory(result.dst.getParent());
                        } catch (IOException e) {
                            System.out.println("Failed to create directory: " + result.dst.getParent()
                                    + ", cause: " + e.getClass() + " " + e.getMessage());
                            return;
                        }
                    }
                    try (PrintWriter writer = new PrintWriter(result.dst.toFile(), UTF_8)) {
                        writer.println("token,count");
                        result.counters.forEach((token, count) -> writer.println(token + "," + count));
                    } catch (Exception e) {
                        System.out.println("Failed to write: " + result.dst
                                + ", cause: " + e.getClass() + " " + e.getMessage());
                    }
                    state.unitWritten();
                });
    }

    private static class CodeWrapper {
        private final String code;
        private final Path dst;

        private CodeWrapper(String code, Path dst) {
            this.code = code;
            this.dst = dst;
        }
    }

    private static class TokensWrapper {
        private final Map<String, Long> counters;
        private final Path dst;

        private TokensWrapper(Map<String, Long> counters, Path dst) {
            this.counters = counters;
            this.dst = dst;
        }
    }
}
