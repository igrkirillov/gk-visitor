package ru.x5.gk.visitor;

import ru.x5.gk.visitor.jms.JmsVisitor;
import ru.x5.gk.visitor.sql.SqlVisitor;
import ru.x5.gk.visitor.ssh.SshVisitor;

public class CommandLine {
    public static void main(String[] args) {
        System.out.println("1 - sql visitor");
        System.out.println("2 - ssh visitor");
        System.out.println("3 - jms visitor");
        System.out.println("Выберите номер команды:");
        final int input = Integer.parseInt(System.console().readLine());
        switch (input) {
            case 1 :
                SqlVisitor.main(args);
                return;
            case 2:
                SshVisitor.main(args);
                return;
            case 3:
                JmsVisitor.main(args);
        }
    }
}
