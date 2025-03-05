package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GestionUsuariosController {
    private String dniPaciente;
    @FXML
    private VBox vBoxResultados;
    // Campos de datos personales
    @FXML
    private TextField txtNombre, txtApellido, txtDNI, txtTelefono, txtCorreo, txtDireccion,
            txtLocalidad, txtProvincia, txtCodigoPostal;

    // Contenedor para los seguros
    @FXML
    private VBox vboxSeguros;

    // Botones
    @FXML
    private Button btnGuardarInfo, btnGuardarSeguros;

    // Conexión a la base de datos
    private static final String URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String USER = "test";
    private static final String PASSWORD = "contraseña_segura_patata_12112";

    // Lista de CheckBox para los seguros con sus respectivos IDs
    private List<Pair<CheckBox, Integer>> checkboxSeguros = new ArrayList<>();
    private MainController mainController;

    // Inicialización del controlador
    @FXML
    public void initialize() {
        btnGuardarInfo.setOnAction(event -> guardarDatosPersonales());
        btnGuardarSeguros.setOnAction(event -> guardarSeguros());
        txtDNI.setEditable(false); // Evitar edición manual del DNI
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    @FXML
    private void volver() {
        System.out.println("Volviendo al menú principal...");
        try {
            // Cargar la vista del menú principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/main-view.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del menú principal (si necesitas pasar datos, como el DNI)
            MainController mainController = loader.getController();
            mainController.setDni(this.dniPaciente);

            // Crear una nueva escena para la ventana del menú principal
            Scene mainScene = new Scene(root);

            // Obtener la ventana actual (Stage)
            Stage currentStage = (Stage) btnGuardarInfo.getScene().getWindow();

            // Si el nodo raíz es una instancia de Region (por ejemplo, AnchorPane), podemos enlazar sus propiedades de tamaño
            if (root instanceof Region) {
                Region region = (Region) root;
                region.prefWidthProperty().bind(currentStage.widthProperty());
                region.prefHeightProperty().bind(currentStage.heightProperty());
            }

            // Reemplazar la escena actual por la nueva
            currentStage.setScene(mainScene);
            currentStage.setTitle("Menú Principal");
            currentStage.show();

            System.out.println("Regresaste al menú principal con éxito.");
        } catch (IOException e) {
            System.err.println("Error al cargar la vista del menú principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para definir el DNI del cliente
    public void setDni(String dni) {
        txtDNI.setText(dni);
        this.dniPaciente = dni;
        cargarDatosDesdeBaseDeDatos(dni);

    }

    // Cargar datos personales y seguros desde la base de datos
    private void cargarDatosDesdeBaseDeDatos(String dni) {
        String query = """
            SELECT p.id_paciente, p.nombre AS paciente_nombre, p.apellido, p.telefono, p.correo, p.direccion, 
                   p.localidad, p.provincia, p.codigo_postal,
                   array_agg(ps.id_seguro) as seguros_cliente
            FROM pacientes p
            LEFT JOIN paciente_seguros ps ON p.id_paciente = ps.id_paciente
            WHERE p.dni = ?
            GROUP BY p.id_paciente;
            """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, dni);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Datos personales
                txtNombre.setText(resultSet.getString("paciente_nombre"));
                txtApellido.setText(resultSet.getString("apellido"));
                txtTelefono.setText(resultSet.getString("telefono"));
                txtCorreo.setText(resultSet.getString("correo"));
                txtDireccion.setText(resultSet.getString("direccion"));
                txtLocalidad.setText(resultSet.getString("localidad"));
                txtProvincia.setText(resultSet.getString("provincia"));
                txtCodigoPostal.setText(resultSet.getString("codigo_postal"));

                // Obtener ID paciente
                int idPaciente = resultSet.getInt("id_paciente");

                // Seguros del cliente (IDs)
                Integer[] segurosCliente = (Integer[]) resultSet.getArray("seguros_cliente").getArray();

                // Cargar seguros relacionados al cliente
                cargarSegurosDesdeBaseDeDatos(idPaciente, segurosCliente);
            } else {
                mostrarAlerta("Información", "No se encontraron datos para el DNI: " + dni);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los datos personales: " + e.getMessage());
        }
    }

    // Cargar seguros disponibles y marcar los asociados al cliente
    // Cargar seguros disponibles y marcar los asociados al cliente
    private void cargarSegurosDesdeBaseDeDatos(int idPaciente, Integer[] segurosCliente) {
        String queryTodosLosSeguros = "SELECT id_seguro, nombre, descripcion FROM seguros";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(queryTodosLosSeguros);
             ResultSet resultSet = statement.executeQuery()) {

            // Convertir la lista de seguros del cliente a un Set (evitando nulos)
            Set<Integer> segurosClienteSet = new HashSet<>();
            if (segurosCliente != null) {
                for (Integer seguro : segurosCliente) {
                    if (seguro != null) { // Asegurarse de no incluir valores nulos
                        segurosClienteSet.add(seguro);
                    }
                }
            }

            // Limpiar el VBox de seguros previo a la carga
            vboxSeguros.getChildren().clear();
            checkboxSeguros.clear();

            while (resultSet.next()) {
                int idSeguro = resultSet.getInt("id_seguro");
                String nombreSeguro = resultSet.getString("nombre");
                String descripcionSeguro = resultSet.getString("descripcion");

                // Contenedor de cada seguro
                VBox seguroItem = new VBox(5); // Espaciado interno
                seguroItem.getStyleClass().add("seguro-item"); // Clase CSS para estilos

                // CheckBox para seleccionar/deseleccionar seguro
                CheckBox checkBox = new CheckBox(nombreSeguro);
                checkBox.setUserData(idSeguro); // Asignar el ID del seguro al CheckBox
                checkBox.setSelected(segurosClienteSet.contains(idSeguro)); // Marcar si el cliente tiene el seguro
                checkBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

                // Label para la descripción del seguro
                Label descripcionLabel = new Label(descripcionSeguro);
                descripcionLabel.setWrapText(true); // Permitir salto de línea
                descripcionLabel.setMaxWidth(350); // Limitar ancho
                descripcionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

                // Agregar el CheckBox y la descripción al contenedor
                seguroItem.getChildren().addAll(checkBox, descripcionLabel);
                vboxSeguros.getChildren().add(seguroItem);

                // Guardar el CheckBox en la lista para futuras referencias
                checkboxSeguros.add(new Pair<>(checkBox, idSeguro));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los seguros: " + e.getMessage());
        }
    }

    // Guardar datos personales
    private void guardarDatosPersonales() {
        String query = """
            UPDATE pacientes SET nombre = ?, apellido = ?, telefono = ?, correo = ?, direccion = ?, 
                                 localidad = ?, provincia = ?, codigo_postal = ? 
            WHERE dni = ?
        """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, txtNombre.getText());
            statement.setString(2, txtApellido.getText());
            statement.setString(3, txtTelefono.getText());
            statement.setString(4, txtCorreo.getText());
            statement.setString(5, txtDireccion.getText());
            statement.setString(6, txtLocalidad.getText());
            statement.setString(7, txtProvincia.getText());
            statement.setString(8, txtCodigoPostal.getText());
            statement.setString(9, txtDNI.getText());

            statement.executeUpdate();
            mostrarAlerta("Éxito", "La información personal ha sido actualizada.");

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron guardar los datos personales: " + e.getMessage());
        }
    }

    // Guardar los seguros seleccionados en la base de datos
    private void guardarSeguros() {
        String dni = txtDNI.getText();
        int idPaciente = obtenerIdPacientePorDni(dni);

        if (idPaciente == 0) {
            mostrarAlerta("Error", "No se encontró el paciente con el DNI: " + dni);
            return;
        }

        String queryEliminar = "DELETE FROM paciente_seguros WHERE id_paciente = ?";
        String queryInsertar = "INSERT INTO paciente_seguros (id_paciente, id_seguro) VALUES (?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Eliminar todos los seguros actuales del cliente
            try (PreparedStatement eliminarStatement = connection.prepareStatement(queryEliminar)) {
                eliminarStatement.setInt(1, idPaciente);
                eliminarStatement.executeUpdate();
            }

            // Insertar los seguros seleccionados
            try (PreparedStatement insertarStatement = connection.prepareStatement(queryInsertar)) {
                for (Pair<CheckBox, Integer> pair : checkboxSeguros) {
                    CheckBox checkBox = pair.getKey();
                    if (checkBox.isSelected()) {
                        insertarStatement.setInt(1, idPaciente);
                        insertarStatement.setInt(2, pair.getValue());
                        insertarStatement.executeUpdate();
                    }
                }
            }

            mostrarAlerta("Éxito", "Los seguros han sido actualizados.");

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron guardar los seguros: " + e.getMessage());
        }
    }

    // Obtener el ID del paciente a partir del DNI
    private int obtenerIdPacientePorDni(String dni) {
        String query = "SELECT id_paciente FROM pacientes WHERE dni = ?";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, dni);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id_paciente");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Mostrar una alerta al usuario
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}