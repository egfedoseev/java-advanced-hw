package info.kgeorgiy.ja.fedoseev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Implementor implements Impler {

    private static Path getCodePath(Path root, String packageName) {
        return root.resolve(Path.of(packageName.replace('.', File.separatorChar)));
    }


    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        final ClassImplementor implementor = new ClassImplementor(token);
        final String code = implementor.implement();
//        System.out.println(code);

        Path codePath = getCodePath(root, token.getPackageName());
        try {
            Files.createDirectories(codePath);
        } catch (IOException e) {
            throw new ImplerException("Can't create directories for generated class", e);
        }
        Path filePath = codePath.resolve(token.getSimpleName() + "Impl.java");
        try {
            Files.writeString(filePath, code);
        } catch (IOException e) {
            throw new ImplerException("Can't write to " + filePath, e);
        }
    }

    public static void main() throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(Implementor.class, Path.of("/home/egor/Projects/java-advanced/tmp"));
    }
}
