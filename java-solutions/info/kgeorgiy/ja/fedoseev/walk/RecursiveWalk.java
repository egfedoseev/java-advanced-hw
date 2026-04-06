package info.kgeorgiy.ja.fedoseev.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import static info.kgeorgiy.ja.fedoseev.walk.Walk.tryParsePath;

public class RecursiveWalk {

    public static final String FORMAT = "%08x %s";

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: INPUT OUTPUT");
            return;
        }
        Optional<Path> parsedInput = tryParsePath(args[0]);
        if (parsedInput.isEmpty()) {
            System.err.println("Invalid input path: " + args[0]);
            return;
        }
        Path inputPath = parsedInput.get();

        Optional<Path> parsedOutput = tryParsePath(args[1]);
        if (parsedOutput.isEmpty()) {
            System.err.println("Invalid output path: " + args[1]);
            return;
        }
        Path outputPath = parsedOutput.get();

        try {
            final Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Can't create parent directories for output file: " + e.getMessage());
            return;
        }

        try (BufferedReader inputReader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            try (BufferedWriter outputWriter = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                processFiles(inputReader, outputWriter);
            } catch (IOException e) {
                System.err.println("Can't read from input file: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + args[0] + " not found");
        } catch (IOException e) {
            System.err.println("Something went wrong while trying to access input file: " + e.getMessage());
        }
    }

    private static void processFiles(BufferedReader inputReader, BufferedWriter outputWriter) throws IOException {
        while (true) {
            final String pathString = inputReader.readLine();
            if (pathString == null) {
                break;
            }
            try {
                Optional<Path> path = Walk.tryParsePath(pathString);
                if (path.isEmpty() || !Files.isDirectory(path.get())) {
                    Walk.processRegularFile(outputWriter, path, pathString);
                    continue;
                }
                Files.walkFileTree(path.get(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        outputWriter.write(String.format(FORMAT, Walk.hashFile(file), file));
                        outputWriter.newLine();
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.err.println("Can't write to output file: " + e.getMessage());
                return;
            }
        }
    }
}
