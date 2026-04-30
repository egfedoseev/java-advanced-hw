package info.kgeorgiy.ja.fedoseev.implementor;

import info.kgeorgiy.ja.fedoseev.implementor.exception.CompilerException;
import info.kgeorgiy.ja.fedoseev.implementor.exception.ImplerIOException;
import info.kgeorgiy.ja.fedoseev.implementor.exception.InvalidParentException;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Implementor class is used to implement some classes.
 */
public class Implementor implements JarImpler {
    /**
     * Constructs new {@code Implementor}.
     */
    public Implementor() {
    }

    /**
     * Returns path where code will be stored.
     *
     * @param root        root path.
     * @param packageName package of class.
     * @return path where code will be stored.
     */
    private static Path getCodePath(Path root, String packageName) {
        return root.resolve(Path.of(packageName.replace('.', File.separatorChar)));
    }

    /**
     * Returns code string of implemented class.
     *
     * @param token class to be implemented.
     * @return Code string of implemented class.
     * @throws InvalidParentException If class can't be implemented.
     */
    private static String implement(Class<?> token) throws InvalidParentException {
        return new ClassImplementor(token).implement();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ImplerIOException      If IOException occurred.
     * @throws InvalidParentException If class can't be implemented.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerIOException, InvalidParentException {
        final String code = implement(token);

        Path codePath = getCodePath(root, token.getPackageName());
        try {
            Files.createDirectories(codePath);
        } catch (IOException e) {
            throw new ImplerIOException("Can't create directories for generated class", e);
        }
        Path filePath = codePath.resolve(token.getSimpleName() + "Impl.java");
        try {
            Files.writeString(filePath, code, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ImplerIOException("Can't write to " + filePath, e);
        }
    }

    /**
     * The entry point of the application.
     * <p>
     * This method allows running the code generation or JAR archive creation from the command line.
     * Depending on the provided arguments, the program either generates the implementation source code
     * ({@code .java}) or packages it directly into a {@code .jar} file.
     *
     * @param args command-line arguments. Two or three arguments are expected:
     *             <ul>
     *             <li>For generating a {@code .java} file: {@code <fully_qualified_class_name> <root_directory_path>}</li>
     *             <li>For generating a {@code .jar} file: {@code -jar <fully_qualified_class_name> <jar_file_path>}</li>
     *             </ul>
     */
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        final String className;
        final String output;
        boolean isJar = false;
        if (args.length < 3) {
            className = args[0];
            if (args.length == 2) {
                output = args[1];
            } else {
                output = ".";
            }
        } else if (args.length == 3 && Objects.equals(args[0], "-jar")) {
            className = args[1];
            output = args[2];
            isJar = true;
        } else {
            System.err.println("USAGE: [-jar] class [output]");
            return;
        }

        final Class<?> token;
        try {
            token = ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + className + " not found: " + e.getMessage());
            return;
        }

        final Path outputPath = Path.of(output);
        try {
            if (isJar) {
                implementor.implementJar(token, outputPath);
            } else {
                implementor.implement(token, outputPath);
            }
        } catch (ImplerException e) {
            System.err.println("Can't implement: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidParentException If class can't be implemented.
     * @throws ImplerIOException      If an IOException occurred.
     * @throws CompilerException      If something went wrong while compiling implemented class.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws InvalidParentException, ImplerIOException, CompilerException {
        Path tmp;
        try {
            tmp = Files.createTempDirectory("root_tmp");
        } catch (IOException e) {
            throw new ImplerIOException("Can't create directory", e);
        }

        try {
            implement(token, tmp);
            String implName = token.getSimpleName() + "Impl";
            Path javaFile = getCodePath(tmp, token.getPackageName()).resolve(implName + ".java");
            compile(List.of(javaFile), List.of(token));

            Files.deleteIfExists(jarFile);
            Path classFile = javaFile.resolveSibling(implName + ".class");

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                String entryName = token.getPackageName().replace('.', '/') + "/" + implName + ".class";
                JarEntry jarEntry = new JarEntry(entryName);

                jarOutputStream.putNextEntry(jarEntry);
                Files.copy(classFile, jarOutputStream);
                jarOutputStream.closeEntry();
            } catch (IOException e) {
                throw new ImplerIOException("Can't write to jar file", e);
            }
        } catch (IOException e) {
            throw new ImplerIOException("Can't create jar file", e);
        } finally {
            try {
                deleteRecursive(tmp);
            } catch (IOException _) {
            }
        }
    }

    /**
     * Deletes directory recursively. Equivalent to {@code rm -r} in shell.
     *
     * @param path path of directory of file.
     * @throws IOException if IOException occurs while walking file tree.
     */
    private static void deleteRecursive(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Compiles java files. Class files will be stored in the same directories as source files.
     *
     * @param files        list of source files that will be compiled.
     * @param dependencies list of dependencies.
     * @throws CompilerException if files can't be compiled.
     */
    private static void compile(
            final List<Path> files,
            final List<Class<?>> dependencies
    ) throws CompilerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompilerException("Could not find java compiler, include tools.jar to classpath");
        }
        final String classpath = getClassPath(dependencies).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        final String[] args = Stream.concat(
                Stream.of("-cp", classpath, "-encoding", StandardCharsets.UTF_8.name()),
                files.stream().map(Path::toString)
        ).toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new CompilerException("Compiler exit code");
        }
    }

    /**
     * Returns classpath based on dependencies.
     *
     * @param dependencies list of dependencies.
     * @return classpath.
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
                    } catch (final URISyntaxException e) {
                        throw new AssertionError(e);
                    }
                })
                .toList();
    }
}
