package info.kgeorgiy.ja.fedoseev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Implementor implements Impler {
    private static final String FOUR_SPACES = "    ";
    private static final String EIGHT_SPACES = "        ";

    private record MethodSignature(String name, Class<?> returnType, Class<?>[] argTypes, int modifiers) {
        public MethodSignature(Method method) {
            this(method.getName(), method.getReturnType(), method.getParameterTypes(), method.getModifiers());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) && Objects.deepEquals(argTypes, that.argTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(argTypes));
        }
    }

    private static void walkClassTree(Class<?> token,
                                      Set<MethodSignature> abstractMethods,
                                      Set<MethodSignature> implemented) {
        processMethods(token.getDeclaredMethods(), abstractMethods, implemented);
        Class<?> superclass = token.getSuperclass();
        if (superclass != null) {
            walkClassTree(superclass, abstractMethods, implemented);
        }
        for (Class<?> inter : token.getInterfaces()) {
            walkClassTree(inter, abstractMethods, implemented);
        }
    }

    private static void processMethods(Method[] methods,
                                       Set<MethodSignature> abstractMethods,
                                       Set<MethodSignature> implemented) {
        for (Method method : methods) {
            MethodSignature signature = new MethodSignature(method);
            int modifiers = signature.modifiers;
            if (Modifier.isAbstract(modifiers) && !implemented.contains(signature)) {
                abstractMethods.add(signature);
            } else if (!abstractMethods.contains(signature)) {
                implemented.add(signature);
            }
        }
    }

    private static Path getCodePath(Path root, String packageName) {
        return root.resolve(Path.of(packageName.replace('.', File.separatorChar)));
    }

    private Constructor<?> getConstructor(Class<?> token) throws ImplerException {
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                continue;
            }
            return constructor;
        }
        throw new ImplerException("No available constructors for class " + token);
    }

    private static String getAccessModifierString(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "public ";
        } else if (Modifier.isProtected(modifiers)) {
            return "protected ";
        }
        return "";
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        validate(token);

        StringBuilder code = new StringBuilder();
        String packageName = token.getPackageName();
        code.append("package ").append(packageName).append(';').append(System.lineSeparator());
        String simpleName = token.getSimpleName() + "Impl";
        code.append("public class ").append(simpleName).append(' ');
        if (token.isInterface()) {
            code.append("implements ");
        } else {
            code.append("extends ");
        }
        code.append(token.getCanonicalName()).append(" {").append(System.lineSeparator());

        if (!token.isInterface()) {
            Constructor<?> constructor = getConstructor(token);
            implementConstructor(constructor, simpleName, code);
        }

        Set<MethodSignature> abstractMethods = new HashSet<>();
        Set<MethodSignature> implemented = new HashSet<>();
        walkClassTree(token, abstractMethods, implemented);

        for (MethodSignature signature : abstractMethods) {
            implementMethod(signature, code);
        }
        code.append('}');
//        System.out.println(code);

        Path codePath = getCodePath(root, packageName);
        try {
            Files.createDirectories(codePath);
        } catch (IOException e) {
            throw new ImplerException("Can't create directories for generated class", e);
        }
        Path filePath = codePath.resolve(simpleName + ".java");
        try {
            Files.writeString(filePath, code);
        } catch (IOException e) {
            throw new ImplerException("Can't write to " + filePath, e);
        }
    }

    private static boolean implementConstructor(Constructor<?> constructor, String simpleName, StringBuilder code) {
        final int modifiers = constructor.getModifiers();
        if (!checkArgTypes(constructor.getParameterTypes())) {
            return false;
        }
        code.append(FOUR_SPACES).append(getAccessModifierString(modifiers)).append(simpleName);
        buildArgs(constructor.getParameterTypes(), code);

        if (constructor.getExceptionTypes().length > 0) {
            code.append(" throws Exception");
        }

        code.append("{").append(System.lineSeparator());

        code.append(EIGHT_SPACES).append("super(");
        for (int i = 0; i < constructor.getParameterCount(); ++i) {
            code.append("arg").append(i);
            if (i + 1 < constructor.getParameterCount()) {
                code.append(", ");
            }
        }
        code.append(");").append(System.lineSeparator());
        code.append(FOUR_SPACES).append('}').append(System.lineSeparator());
        return true;
    }

    private static void implementMethod(MethodSignature signature, StringBuilder code) throws ImplerException {
        code.append(FOUR_SPACES).append("@Override").append(System.lineSeparator());
        int modifiers = signature.modifiers;
        code.append(FOUR_SPACES).append(getAccessModifierString(modifiers));

        Class<?> returnType = signature.returnType;
        code.append(returnType.getCanonicalName()).append(' ');

        code.append(signature.name);
        if (!buildArgs(signature.argTypes, code)) {
            throw new ImplerException("Inaccessible class in arguments of method " + signature);
        }

        code.append(" {").append(System.lineSeparator());
        code.append(EIGHT_SPACES).append("return").append(getReturnValue(returnType)).append(';').append(System.lineSeparator());
        code.append(FOUR_SPACES).append('}').append(System.lineSeparator());
    }

    private static boolean checkArgTypes(Class<?>[] argTypes) {

    }

    private static boolean buildArgs(Class<?>[] argTypes, StringBuilder code) {
        if (!checkArgTypes(argTypes)) {
            return false;
        }
        code.append('(');
        for (int i = 0; i < argTypes.length; ++i) {
            Class<?> argType = argTypes[i];
            code.append(argType.getCanonicalName()).append(" arg").append(i);
            if (i + 1 < argTypes.length) {
                code.append(", ");
            }
        }
        code.append(")");
        return true;
    }

    private static void validate(Class<?> token) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Class token is null");
        }
        if (token.isPrimitive()) {
            throw new ImplerException("Class is primitive");
        }
        if (token.isEnum()) {
            throw new ImplerException("Class is enum");
        }
        if (token.isArray()) {
            throw new ImplerException("Class is array");
        }
        if (token.isSealed()) {
            throw new ImplerException("Class is sealed interface");
        }
        Module module = token.getModule();
        if (module.isNamed() && (module.getName().startsWith("java.") || module.getName().startsWith("jdk."))) {
            throw new ImplerException("Class package can't be split");
        }
        int tokenModifiers = token.getModifiers();
        if (Modifier.isPrivate(tokenModifiers)) {
            throw new ImplerException("Class is private");
        }
        if (Modifier.isFinal(tokenModifiers)) {
            throw new ImplerException("Class is final");
        }
        if (token.isMemberClass() && !Modifier.isStatic(tokenModifiers)) {
            throw new ImplerException("Class token is non-static member class");
        }
    }

    private static String getReturnValue(Class<?> returnType) {
        if (returnType != void.class) {
            if (returnType == boolean.class) {
                return " false";
            } else if (returnType.isPrimitive()) {
                return " 0";
            } else {
                return " null";
            }
        }
        return "";
    }

    public static void main() throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(Implementor.class, Path.of("/home/egor/Projects/java-advanced/tmp"));
    }
}
