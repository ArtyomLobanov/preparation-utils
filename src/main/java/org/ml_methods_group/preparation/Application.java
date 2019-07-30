package org.ml_methods_group.preparation;

import java.io.IOException;
import java.nio.file.Paths;

public class Application {
    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "init":
                CleanUtils.copyFiles(Paths.get(args[1]), Paths.get(args[2]), path -> !path.getFileName().toString().endsWith(".cpp"));
                break;

            case "clean":
                CleanUtils.cleanSolutions(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
                break;

            case "tokens":
                ParsingUtils.parseTokens(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
                break;

            default:
                throw new IllegalArgumentException(args[0]);
        }
    }
}
