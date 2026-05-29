module java.solutions {
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.implementor.tools;
    requires info.kgeorgiy.java.advanced.streams;
    requires info.kgeorgiy.java.advanced.student;
    requires java.compiler;
    requires info.kgeorgiy.java.advanced.iterative;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires java.rmi;
    requires jdk.httpserver;

    exports info.kgeorgiy.ja.fedoseev.bank to java.rmi;
    exports info.kgeorgiy.ja.fedoseev.bank.account to java.rmi;
    exports info.kgeorgiy.ja.fedoseev.bank.person to java.rmi;
}