package info.kgeorgiy.ja.fedoseev.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {

    public static final Comparator<Student> ORDER_BY_NAME = Comparator.comparing(Student::lastName)
            .thenComparing(Student::firstName).reversed()
            .thenComparing(Student::id);

    private static Stream<Group> streamGroups(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::groupName))
                .entrySet().stream()
                .map(entry -> new Group(entry.getKey(), entry.getValue()));
    }

    private static List<Group> getGroupsBy(Collection<Student> collection, Comparator<? super Student> comparator) {
        return streamGroups(collection)
                .map(g -> new Group(g.name(), g.students().stream().sorted(comparator).toList()))
                .sorted(Comparator.comparing(Group::name))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBy(students, ORDER_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBy(students, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return streamGroups(students)
                .max(Comparator.comparing((Group group) -> group.students().size())
                        .thenComparing(Group::name)).map(Group::name).orElse(null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return streamGroups(students)
                .max(Comparator.comparing((Group g) -> g.students().stream().map(Student::firstName).distinct().count())
                        .thenComparing(Group::name, Comparator.reverseOrder()))
                .map(Group::name).orElse(null);
    }

    private static <T> List<T> mapStudents(List<Student> students, Function<Student, T> mapper) {
        return students.stream().map(mapper).toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudents(students, Student::firstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudents(students, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> students) {
        return mapStudents(students, Student::groupName);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return students.stream().map(student -> student.firstName() + " " + student.lastName()).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted().toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(ORDER_BY_NAME).toList();
    }

    private static List<Student> findStudents(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate).sorted(ORDER_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudents(students, student -> student.firstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudents(students, student -> student.lastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudents(students, student -> student.groupName().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream().filter(student -> student.groupName().equals(group))
                .collect(Collectors.toMap(
                        Student::lastName,
                        Student::firstName,
                        BinaryOperator.minBy(Comparator.naturalOrder()) // Выбирает минимальное имя при одинаковых фамилиях
                ));
    }
}
