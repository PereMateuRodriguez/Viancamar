package org.example.biancamar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;



public class DatabaseHandler {
    private static final String URL = "jdbc:postgresql://pm0002@conectabalear.net:5432/Biancamar";
    private static final String USER = "test";
    private static final String PASSWORD = "contraseña_segura_patata_12112";


    public static boolean validarUsuario(String dni, String contraseña) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String query = "SELECT contraseña FROM usuarios WHERE dni = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, dni);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("contraseña");
                return contraseña.equals(storedPassword); // Aquí puedes usar bcrypt
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
