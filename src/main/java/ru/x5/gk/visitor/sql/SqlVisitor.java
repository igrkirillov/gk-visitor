package ru.x5.gk.visitor.sql;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;

public class SqlVisitor {
    public static void main(String[] args) {
        try {
            String shops = new String(Files.readAllBytes(Paths.get("./shops.txt")));
            String sql = new String(Files.readAllBytes(Paths.get("./sql.txt")));
            String[] var4 = shops.split(",");
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String shop = var4[var6];
                shop = shop.trim();
                String result = "\n" + shop;

                try {
                    Connection con = DriverManager.getConnection("jdbc:postgresql://BO-" + shop + ":5432/postgres?currentSchema=gkretail", "gkretail", "gkretail");
                    Throwable var9 = null;

                    try {
                        Statement statement = con.createStatement();
                        ResultSet rs = statement.executeQuery(sql);
                        ResultSetMetaData md = rs.getMetaData();

                        while(rs.next()) {
                            result = result + "\n";

                            for(int col = 1; col <= md.getColumnCount(); ++col) {
                                result = result + rs.getString(col) + "  ";
                            }
                        }

                        System.out.println(result);
                        Files.write(Paths.get("./result.txt"), Arrays.asList(result), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (Throwable var23) {
                        var9 = var23;
                        throw var23;
                    } finally {
                        if (con != null) {
                            if (var9 != null) {
                                try {
                                    con.close();
                                } catch (Throwable var22) {
                                    var9.addSuppressed(var22);
                                }
                            } else {
                                con.close();
                            }
                        }

                    }
                } catch (Exception var25) {
                    var25.printStackTrace();
                }
            }
        } catch (Exception var26) {
            var26.printStackTrace();
        }

    }
}
