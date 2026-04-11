package info.kgeorgiy.ja.fedoseev.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.stream.Collectors;

public class StudentDB implements GroupQuery {

    public static final Comparator<Student> ORDER_BY_NAME = Comparator.comparing(Student::lastName)
            .thenComparing(Student::firstName).reversed()
            .thenComparing(Student::id);

    private List<Group> getGroups(Collection<Student> collection) {
        return collection.stream().collect(Collectors.groupingBy(Student::groupName))
                .entrySet().stream().map(entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::name))
                .toList();
    }

    private List<Group> getGroupsBy(Collection<Student> collection, Comparator<Student> comparator) {
        return getGroups(collection).stream()
                .map(group -> new Group(group.name(),
                        group.students().stream().sorted(comparator).toList()))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupsBy(collection, ORDER_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupsBy(collection, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> collection) {
        return getGroups(collection).stream()
                .max(Comparator.comparing((Group group) -> group.students().size())
                        .thenComparing(Group::name)).map(Group::name).orElse(null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> collection) {
        return getGroups(collection).stream()
                .min(Comparator.comparing((Group group) -> -group.students().stream()
                                .map(Student::firstName).distinct().count())
                        .thenComparing(Group::name))
                .map(Group::name).orElse(null);
    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return list.stream().map(Student::firstName).toList();
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return list.stream().map(Student::lastName).toList();
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> list) {
        return list.stream().map(Student::groupName).toList();
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return list.stream().map(student -> student.firstName() + " " + student.lastName()).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return list.stream().map(Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> list) {
        return list.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return collection.stream().sorted().toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return collection.stream().sorted(ORDER_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return collection.stream().filter(student -> student.firstName().equals(s)).sorted(ORDER_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return collection.stream().filter(student -> student.lastName().equals(s)).sorted(ORDER_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, GroupName groupName) {
        return collection.stream().filter(student -> student.groupName().equals(groupName)).
                sorted(ORDER_BY_NAME).toList();
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, GroupName groupName) {
        return collection.stream().filter(student -> student.groupName().equals(groupName)).
                collect(Collectors.groupingBy(
                        Student::lastName,
                        Collectors.mapping(
                                Student::firstName,
                                Collectors.reducing(String.valueOf(Character.MAX_VALUE), (a, b) -> a.compareTo(b) <= 0 ? a : b))));
    }
}
