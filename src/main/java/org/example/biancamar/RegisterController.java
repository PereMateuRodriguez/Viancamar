package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class RegisterController {

    // Campos del formulario
    @FXML
    private TextField nameField, lastNameField, phoneField, emailField, dniField, addressField, cityField, postalCodeField;

    @FXML
    private PasswordField passwordInput;

    @FXML
    private DatePicker birthDateField;

    @FXML
    private ComboBox<String> insuranceComboBox;

    @FXML
    private ComboBox<String> provinceComboBox; // Nuevo ComboBox para provincias

    @FXML
    private Label errorLabel;

    // Datos de conexión a la base de datos
    private static final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "contraseña_segura_patata_12112";

    @FXML
    public void initialize() {
        // Inicializa datos al cargar la interfaz
        cargarSeguros();
        cargarProvincias();
    }

    /**
     * Carga los seguros en el ComboBox desde la base de datos.
     */
    private void cargarSeguros() {
        String query = "SELECT nombre FROM seguros";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String nombreSeguro = resultSet.getString("nombre");
                insuranceComboBox.getItems().add(nombreSeguro);
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error al cargar los seguros desde la base de datos.");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Carga provincias en el ComboBox.
     */
    private void cargarProvincias() {
        List<String> provincias = Arrays.asList(
                "Álava", "Albacete", "Alicante", "Almería", "Asturias", "Ávila", "Badajoz",
                "Barcelona", "Burgos", "Cáceres", "Cádiz", "Cantabria", "Castellón", "Ciudad Real",
                "Córdoba", "Cuenca", "Gerona", "Granada", "Guadalajara", "Guipúzcoa", "Huelva",
                "Huesca", "Islas Baleares", "Jaén", "La Coruña", "La Rioja", "Las Palmas",
                "León", "Lérida", "Lugo", "Madrid", "Málaga", "Murcia", "Navarra", "Orense",
                "Palencia", "Pontevedra", "Salamanca", "Segovia", "Sevilla", "Soria", "Tarragona",
                "Santa Cruz de Tenerife", "Teruel", "Toledo", "Valencia", "Valladolid", "Vizcaya",
                "Zamora", "Zaragoza", "Ceuta", "Melilla"
        );

        provinceComboBox.getItems().addAll(provincias);
    }

    /**
     * Registra al usuario en la base de datos.
     */
    @FXML
    private void onRegister() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Recuperar datos del formulario
            String name = nameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String dni = dniField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String postalCode = postalCodeField.getText().trim();
            String password = passwordInput.getText().trim();
            LocalDate birthDate = birthDateField.getValue();
            String selectedInsurance = insuranceComboBox.getValue();
            String selectedProvince = provinceComboBox.getValue();

            // Validar que ningún campo obligatorio esté vacío
            if (name.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty() ||
                    dni.isEmpty() || address.isEmpty() || city.isEmpty() || postalCode.isEmpty() ||
                    password.isEmpty() || birthDate == null || selectedInsurance == null || selectedProvince == null) {
                errorLabel.setText("Todos los campos son obligatorios.");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }
            // Validar el formato del DNI
            if (!dni.matches("^\\d{8}[A-Za-z]$")) {
                errorLabel.setText("Formato DNI incorrecto. Ejemplo: 12345678A");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }  // Validar el formato del correo electrónico
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errorLabel.setText("Formato de correo incorrecto. Ejemplo: usuario@dominio.com");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }
            // Validar el formato del número de teléfono
            if (!phone.matches("^[0-9]{9}$")) {
                errorLabel.setText("Formato de teléfono incorrecto. Ejemplo: 612345678");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Validar el formato del código postal
            if (!postalCode.matches("^[0-9]{5}$")) {
                errorLabel.setText("Formato de código postal incorrecto. Ejemplo: 07500");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // 1. Insertar en la tabla "pacientes"
            String insertPacienteQuery = "INSERT INTO pacientes (nombre, apellido, telefono, correo, dni, contraseña, fecha_nacimiento, direccion, localidad, provincia, codigo_postal) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psPaciente = connection.prepareStatement(insertPacienteQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            psPaciente.setString(1, name);
            psPaciente.setString(2, lastName);
            psPaciente.setString(3, phone);
            psPaciente.setString(4, email);
            psPaciente.setString(5, dni);
            psPaciente.setString(6, password);
            psPaciente.setDate(7, java.sql.Date.valueOf(birthDate));
            psPaciente.setString(8, address);
            psPaciente.setString(9, city);
            psPaciente.setString(10, selectedProvince);
            psPaciente.setString(11, postalCode);

            int affectedRows = psPaciente.executeUpdate();
            if (affectedRows == 0) {
                errorLabel.setText("Error al registrar el usuario.");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Recuperar el id_paciente generado
            int idPaciente;
            try (ResultSet generatedKeys = psPaciente.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    idPaciente = generatedKeys.getInt(1);
                } else {
                    errorLabel.setText("Error al registrar el usuario, no se obtuvo id.");
                    errorLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
            }

            // 2. Obtener el id_seguro según el seguro seleccionado
            String selectSeguroQuery = "SELECT id_seguro FROM seguros WHERE nombre = ?";
            PreparedStatement psSeguro = connection.prepareStatement(selectSeguroQuery);
            psSeguro.setString(1, selectedInsurance);
            int idSeguro;
            try (ResultSet rsSeguro = psSeguro.executeQuery()) {
                if (rsSeguro.next()) {
                    idSeguro = rsSeguro.getInt("id_seguro");
                } else {
                    errorLabel.setText("Seguro no encontrado.");
                    errorLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
            }

            // 3. Insertar en la tabla "paciente_seguros"
            String insertPacienteSeguroQuery = "INSERT INTO paciente_seguros (id_paciente, id_seguro) VALUES (?, ?)";
            PreparedStatement psPacienteSeguro = connection.prepareStatement(insertPacienteSeguroQuery);
            psPacienteSeguro.setInt(1, idPaciente);
            psPacienteSeguro.setInt(2, idSeguro);
            psPacienteSeguro.executeUpdate();

            errorLabel.setText("Usuario registrado exitosamente.");
            errorLabel.setStyle("-fx-text-fill: green;");

            // 4. Redirigir al login sin que la ventana se muestre en pantalla completa
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/login-view.fxml"));
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);
            Stage currentStage = (Stage) nameField.getScene().getWindow();

            // Desactivar modo maximizado y pantalla completa
            currentStage.setMaximized(false);
            currentStage.setFullScreen(false);

            currentStage.setScene(loginScene);
            currentStage.setTitle("Iniciar Sesión");
            currentStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error al registrar el usuario.");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }




}