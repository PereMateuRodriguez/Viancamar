package org.example.biancamar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Prueba {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
        String user = "test";
        String password = "contraseña_segura_patata_12112";

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conexión exitosa a la base de datos.");
        } catch (SQLException e) {
            System.out.println("Error al conectar a la base de datos:");
            e.printStackTrace();
        }
    }
}
