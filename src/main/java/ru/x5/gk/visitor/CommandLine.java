package ru.x5.gk.visitor;

import ru.x5.gk.visitor.jms.JmsVisitor;
import ru.x5.gk.visitor.sql.SqlVisitor;
import ru.x5.gk.visitor.ssh.SshVisitor;

public class CommandLine {
    public static void main(String[] args) {
        switch (args[0]) {
            case "sql" :
                SqlVisitor.main(args);
                break;
            case "ssh":
                SshVisitor.main(args);
                break;
            case "jms":
                JmsVisitor.main(args);
                break;
            default:
                throw new IllegalStateException("Please, input valid program arg");
        }
    }
}
