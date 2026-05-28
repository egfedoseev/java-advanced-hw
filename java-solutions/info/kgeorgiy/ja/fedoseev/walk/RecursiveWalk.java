package info.kgeorgiy.ja.fedoseev.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import static info.kgeorgiy.ja.fedoseev.walk.Walk.*;

public class RecursiveWalk {

    public static void main(String[] args) {
        runWalk(args, RecursiveWalk::processFiles);
    }

    private static void processFiles(BufferedReader inputReader, Writer outputWriter) throws IOException {
        String pathString;
        while ((pathString = inputReader.readLine()) != null) {
            try {
                Optional<Path> optionalPath = Walk.tryParsePath(pathString);
                if (optionalPath.isEmpty()) {
                    writeFailedFileResult(outputWriter, pathString);
                    continue;
                }
                Path path = optionalPath.get();
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        processRegularFile(outputWriter, file, file.toString());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        writeFailedFileResult(outputWriter, file.toString());
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
