package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {


    @FXML
    private TextField dniField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private final String DB_USER = "test";
    private final String DB_PASSWORD = "contraseña_segura_patata_12112";

    @FXML
    private ImageView logoImage;

    @FXML
    public void initialize() {
        // Intenta cargar la imagen desde la ruta absoluta en el classpath.
        InputStream is = getClass().getResourceAsStream("/Imagenes/LogoBiancamar.png");
        if (is == null) {
            System.out.println("No se encontró la imagen en la ruta: /Imagenes/LogoBiancamar.png");
        } else {
            logoImage.setImage(new Image(is));
        }
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) dniField.getScene().getWindow();
            stage.setWidth(350);
            stage.setHeight(500);
        });
    }



    @FXML
    protected void onLogin() {
        String dni = dniField.getText().trim();
        String password = passwordField.getText().trim();

        if (dni.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Por favor, completa todos los campos.");
            return;
        }

        if (validarCredenciales(dni, password)) {
            errorLabel.setText("Inicio de sesión exitoso.");
            abrirVentanaPrincipal(dni); // Pasa el DNI al abrir la ventana principal
        } else {
            errorLabel.setText("Usuario o contraseña incorrectos.");
        }
    }

    @FXML
    protected void onRegister() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("register-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) dniField.getScene().getWindow(); // Obtiene la ventana actual
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle("Registro de Usuario");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validarCredenciales(String dni, String password) {
        String query = "SELECT contraseña FROM pacientes WHERE dni = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, dni);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("contraseña");
                return storedPassword.equals(password); // Cambia esto si usas contraseñas encriptadas
            }

        } catch (Exception e) {
            System.out.println("Error al validar credenciales:");
            e.printStackTrace();
        }

        return false;
    }

    private void abrirVentanaPrincipal(String dni) { // Recibe el DNI
        try {
            // Cargar el archivo main-view.fxml
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            // Obtener el controlador de la vista principal
            MainController mainController = fxmlLoader.getController();

            // Pasar el DNI al MainController
            mainController.setDni(dni);

            // Obtener el Stage actual
            Stage stage = (Stage) dniField.getScene().getWindow();

            // Establecer la escena y configurar el Stage en pantalla completa
            stage.setScene(scene);
            stage.setTitle("Ventana Principal");
            stage.setMaximized(true);  // Esto hace que se abra en pantalla completa (maximizada)
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}