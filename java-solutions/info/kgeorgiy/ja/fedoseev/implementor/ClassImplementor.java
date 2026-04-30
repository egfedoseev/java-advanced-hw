package info.kgeorgiy.ja.fedoseev.implementor;

import info.kgeorgiy.ja.fedoseev.implementor.exception.InvalidParentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This class is used to generate implementation of some class.
 */
public class ClassImplementor {
    /**
     * String of four space characters.
     */
    private static final String FOUR_SPACES = "    ";
    /**
     * String of eight space characters.
     */
    private static final String EIGHT_SPACES = "        ";

    /**
     * This record type represents method signature.
     *
     * @param name       name of method.
     * @param returnType return type of method.
     * @param argTypes   argument types of method.
     * @param modifiers  modifiers of method.
     */
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

    /**
     * This variable contains true, if class were already implemented.
     */
    private boolean isImplemented = false;
    /**
     * Token of class that ClassImplementor implements.
     */
    private final Class<?> token;

    /**
     * This variable contains code of implemented class.
     */
    private final StringBuilder code = new StringBuilder();

    /**
     * Package of implemented class.
     */
    private final Package tokenPackage;
    /**
     * Simple name of implemented class.
     */
    private final String simpleName;

    /**
     * Constructs new ClassImplementor that will implement child of provided class.
     *
     * @param token class token.
     * @throws InvalidParentException if class can't be implemented.
     */
    public ClassImplementor(Class<?> token) throws InvalidParentException {
        validate(token);
        this.token = token;
        tokenPackage = token.getPackage();
        simpleName = token.getSimpleName() + "Impl";
    }

    /**
     * Converts string into escaped Unicode string.
     * @param input string to convert.
     * @return converted string.
     */
    private String toUnicodeEscaped(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Implements class.
     *
     * @return code of implemented class.
     * @throws InvalidParentException if class can't be implemented.
     */
    public String implement() throws InvalidParentException {
        if (isImplemented) {
            return toUnicodeEscaped(code.toString());
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
                throw new InvalidParentException("No available constructors for class " + token.getCanonicalName() + "Impl");
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
        return toUnicodeEscaped(code.toString());
    }

    /**
     * Returns possible constructors of implemented class.
     *
     * @return possible constructors of implemented class.
     */
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

    /**
     * Implements constructor.
     *
     * @param constructor constructor of class.
     * @return true is constructor was implemented, false if constructor can't be implemented.
     */
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

    /**
     * Returns {@link String} that represents access modifier.
     *
     * @param modifiers modifiers
     * @return {@link String} that represents access modifier.
     */
    private static String getAccessModifierString(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "public ";
        } else if (Modifier.isProtected(modifiers)) {
            return "protected ";
        }
        return "";
    }

    /**
     * Builds arguments of function or constructor.
     *
     * @param argTypes Types of arguments
     * @return true if args were built, false if not.
     */
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

    /**
     * Checks if types of arguments are accessible.
     *
     * @param argTypes types of arguments.
     * @return true if accessible, false if not.
     */
    private boolean checkArgTypes(Class<?>[] argTypes) {
        for (Class<?> type : argTypes) {
            if (!checkTypeAccessibility(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if type is accessible from implementation of class.
     *
     * @param type type token.
     * @return true if accessible, false if not.
     */
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

    /**
     * Checks if class is in the same package as the other class.
     *
     * @param other type token.
     * @return true if same package, false if not.
     */
    private boolean isSamePackage(Class<?> other) {
        return token.getPackageName().equals(other.getPackageName());
    }

    /**
     * Checks if other class is member of token.
     *
     * @param other other class.
     * @return true if other class is member of token, false if not.
     */
    private boolean isMemberOfParent(Class<?> other) {
        Class<?> declaringOther = other.getDeclaringClass();
        return declaringOther != null && declaringOther.isAssignableFrom(token);
    }

    /**
     * Walks class tree from class token to Object and finds methods that are abstract and implemented.
     *
     * @param token           class token.
     * @param abstractMethods set where abstract methods will be added.
     * @param implemented     set where implemented methods will be added.
     */
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

    /**
     * Filters methods that are abstract or implemented.
     *
     * @param methods         array of methods.
     * @param abstractMethods set where abstract methods will be added.
     * @param implemented     set where implemented methods will be added.
     */
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

    /**
     * Implements method.
     *
     * @param signature method signature.
     * @throws InvalidParentException if method signature contains inaccessible type.
     */
    private void implementMethod(MethodSignature signature) throws InvalidParentException {
        code.append(FOUR_SPACES).append("@Override").append(System.lineSeparator());
        int modifiers = signature.modifiers;
        code.append(FOUR_SPACES).append(getAccessModifierString(modifiers));

        Class<?> returnType = signature.returnType;
        if (!checkTypeAccessibility(returnType)) {
            throw new InvalidParentException("Method returns inaccessible type " + signature);
        }
        code.append(returnType.getCanonicalName()).append(' ');

        code.append(signature.name);
        if (!buildArgs(signature.argTypes)) {
            throw new InvalidParentException("Inaccessible type in arguments of method " + signature);
        }

        code.append(" {").append(System.lineSeparator());
        code.append(EIGHT_SPACES).append("return").append(getReturnValue(returnType)).append(';').append(System.lineSeparator());
        code.append(FOUR_SPACES).append('}').append(System.lineSeparator());
    }

    /**
     * Returns default value for type.
     *
     * @param returnType type token.
     * @return default value.
     */
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

    /**
     * Checks if class can be implemented.
     *
     * @param token class token.
     * @throws InvalidParentException if class can't be implemented.
     */
    private static void validate(Class<?> token) throws InvalidParentException {
        if (token == null) {
            throw new InvalidParentException("Class token is null");
        }
        if (token.isPrimitive()) {
            throw new InvalidParentException("Class is primitive");
        }
        if (token.isEnum()) {
            throw new InvalidParentException("Class is enum");
        }
        if (token.isArray()) {
            throw new InvalidParentException("Class is array");
        }
        if (token.isSealed()) {
            throw new InvalidParentException("Class is sealed interface");
        }
        Module module = token.getModule();
        if (module.isNamed() && (module.getName().startsWith("java.") || module.getName().startsWith("jdk."))) {
            throw new InvalidParentException("Class package can't be split");
        }
        int tokenModifiers = token.getModifiers();
        if (Modifier.isPrivate(tokenModifiers)) {
            throw new InvalidParentException("Class is private");
        }
        if (Modifier.isFinal(tokenModifiers)) {
            throw new InvalidParentException("Class is final");
        }
        if (token.isMemberClass() && !Modifier.isStatic(tokenModifiers)) {
            throw new InvalidParentException("Class token is non-static member class");
        }
    }
}
