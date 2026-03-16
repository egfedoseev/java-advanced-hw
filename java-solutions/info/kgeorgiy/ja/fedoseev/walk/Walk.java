package info.kgeorgiy.ja.fedoseev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Walk {
    private static int hashFile(Path path) {
        int hash = 0;
        try (BufferedInputStream fileInputStream = new BufferedInputStream(Files.newInputStream(path))) {
            hash = FNWHasher.FNV1Hash(fileInputStream);
        } catch (FileNotFoundException e) {
            System.err.println("File " + path + " not found: " + e.getMessage());
        } catch (IOException e) {
            hash = 0;
        }
        return hash;
    }

    static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: INPUT OUTPUT");
            return;
        }
        Path inputPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

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
        String pathString = inputReader.readLine();
        while (pathString != null) {
            try {
                outputWriter.write(String.format("%08x %s", hashFile(Path.of(pathString)), pathString));
                outputWriter.newLine();
            } catch (IOException e) {
                System.err.println("Can't write to output file: " + e.getMessage());
                return;
            }
            pathString = inputReader.readLine();
        }
    }
}
