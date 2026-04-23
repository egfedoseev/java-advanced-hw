package info.kgeorgiy.ja.fedoseev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassImplementor {
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

    private boolean isImplemented = false;
    private final Class<?> token;

    private final StringBuilder code = new StringBuilder();

    private final Package tokenPackage;
    private final String simpleName;

    public ClassImplementor(Class<?> token) throws ImplerException {
        validate(token);
        this.token = token;
        tokenPackage = token.getPackage();
        simpleName = token.getSimpleName() + "Impl";
    }

    public String implement() throws ImplerException {
        if (isImplemented) {
            return code.toString();
        }

        code.append("package ").append(tokenPackage.getName()).append(';').append(System.lineSeparator());
        code.append("public class ").append(simpleName).append(' ');
        if (token.isInterface()) {
            code.append("implements ");
        } else {
            code.append("extends ");
        }
        code.append(token.getCanonicalName()).append(" {").append(System.lineSeparator());

        if (!token.isInterface()) {
            int validConstructors = 0;
            List<Constructor<?>> constructors = getConstructors();
            for (Constructor<?> constructor : constructors) {
                validConstructors += implementConstructor(constructor) ? 1 : 0;
            }
            if (validConstructors == 0) {
                throw new ImplerException("No available constructors for class " + token.getCanonicalName() + "Impl");
            }
        }

        Set<MethodSignature> abstractMethods = new HashSet<>();
        Set<MethodSignature> implemented = new HashSet<>();
        walkClassTree(token, abstractMethods, implemented);

        for (MethodSignature signature : abstractMethods) {
            implementMethod(signature);
        }
        code.append('}');
        isImplemented = true;
        return code.toString();
    }

    private List<Constructor<?>> getConstructors() {
        List<Constructor<?>> constructors = new ArrayList<>();
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                continue;
            }
            constructors.add(constructor);
        }
        return constructors;
    }

    private boolean implementConstructor(Constructor<?> constructor) {
        final int modifiers = constructor.getModifiers();
        if (!checkArgTypes(constructor.getParameterTypes())) {
            return false;
        }
        code.append(FOUR_SPACES).append(getAccessModifierString(modifiers)).append(simpleName);
        buildArgs(constructor.getParameterTypes());

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

    private static String getAccessModifierString(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "public ";
        } else if (Modifier.isProtected(modifiers)) {
            return "protected ";
        }
        return "";
    }

    private boolean buildArgs(Class<?>[] argTypes) {
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

    private boolean checkArgTypes(Class<?>[] argTypes) {
        for (Class<?> type : argTypes) {
            if (!checkTypeAccessibility(type)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkTypeAccessibility(Class<?> type) {
        while (type.isArray()) {
            type = type.getComponentType();
        }
        final int modifiers = type.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            return true;
        }
        if (Modifier.isPrivate(modifiers)) {
            return false;
        }
        if (Modifier.isProtected(modifiers)) {
            return isSamePackage(type) || isMemberOfParent(type);
        }
        return isSamePackage(type);
    }

    private boolean isSamePackage(Class<?> other) {
        return token.getPackageName().equals(other.getPackageName());
    }

    private boolean isMemberOfParent(Class<?> other) {
        Class<?> declaringOther = other.getDeclaringClass();
        return declaringOther != null && declaringOther.isAssignableFrom(token);
    }

    private static void walkClassTree(Class<?> token,
                               Set<MethodSignature> abstractMethods,
                               Set<MethodSignature> implemented) {
        processMethods(token.getDeclaredMethods(), abstractMethods, implemented);
        Class<?> superclass = token.getSuperclass();
        if (superclass != null) {
            walkClassTree(superclass, abstractMethods, implemented);
        }
        for (Class<?> iface : token.getInterfaces()) {
            walkClassTree(iface, abstractMethods, implemented);
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

    private void implementMethod(MethodSignature signature) throws ImplerException {
        code.append(FOUR_SPACES).append("@Override").append(System.lineSeparator());
        int modifiers = signature.modifiers;
        code.append(FOUR_SPACES).append(getAccessModifierString(modifiers));

        Class<?> returnType = signature.returnType;
        if (!checkTypeAccessibility(returnType)) {
            throw new ImplerException("Method returns inaccessible type " + signature);
        }
        code.append(returnType.getCanonicalName()).append(' ');

        code.append(signature.name);
        if (!buildArgs(signature.argTypes)) {
            throw new ImplerException("Inaccessible type in arguments of method " + signature);
        }

        code.append(" {").append(System.lineSeparator());
        code.append(EIGHT_SPACES).append("return").append(getReturnValue(returnType)).append(';').append(System.lineSeparator());
        code.append(FOUR_SPACES).append('}').append(System.lineSeparator());
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
}
