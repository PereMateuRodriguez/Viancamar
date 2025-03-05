package org.example.biancamar;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class ConsultaResultadosController {

    // Conexión a la base de datos
    private static final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String DB_USER = "test";
    private static final String DB_PASS = "contraseña_segura_patata_12112";

    private String dni; // DNI del paciente que consulta

    @FXML
    private TextField textFieldBuscarDoctor;
    @FXML
    private ComboBox<String> comboBoxEspecialidad;
    @FXML
    private ComboBox<String> comboBoxHospital;
    @FXML
    private TableView<ObservableList<String>> tablaCitas;
    @FXML
    private TableColumn<ObservableList<String>, String> columnaFecha;
    @FXML
    private TableColumn<ObservableList<String>, String> columnaHorario;
    @FXML
    private TableColumn<ObservableList<String>, String> columnaMedico;
    @FXML
    private TableColumn<ObservableList<String>, String> columnaEspecialidad;
    @FXML
    private TableColumn<ObservableList<String>, Void> columnaResultado;

    @FXML
    public void initialize() {
        // Configuración inicial de las columnas de la tabla
        columnaFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0))); // Índice 0: Fecha
        columnaHorario.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(1))); // Índice 1: Horario
        columnaMedico.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(3))); // Índice 3: Doctor
        columnaEspecialidad.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(2))); // Índice 2: Especialidad

        configurarColumnaResultado(); // Configurar la columna del botón "Ver resultado"

        // Cargar datos iniciales para los filtros
        cargarEspecialidades();
        cargarHospitales();
    }

    private void configurarColumnaResultado() {
        columnaResultado.setCellFactory(columna -> new TableCell<>() {
            private final Button btnResultado = new Button("Ver Resultado");

            {
                btnResultado.setOnAction(event -> {
                    // Obtener la fila seleccionada
                    ObservableList<String> fila = getTableView().getItems().get(getIndex());
                    mostrarResultado(fila); // Generar el informe PDF para la cita seleccionada
                });
                btnResultado.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null); // No mostrar botón si la celda está vacía
                } else {
                    setGraphic(btnResultado); // Mostrar botón si hay contenido
                }
            }
        });
    }

    private void cargarResultados() {
        // Obtener y mostrar todas las citas pasadas del paciente
        ObservableList<ObservableList<String>> citas = FXCollections.observableArrayList();
        String query = """
            SELECT 
                c.id_cita AS id_cita,
                c.fecha AS fecha_cita,
                c.hora_inicio AS hora_cita_inicio,
                c.hora_fin AS hora_cita_fin,
                m.nombre AS nombre_medico,
                m.apellido AS apellido_medico,
                e.nombre AS especialidad_medico
            FROM 
                citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN medicos m ON c.id_medico = m.id_medico
            INNER JOIN especialidades e ON m.especialidad = e.id_especialidad
            WHERE 
                p.dni = ?
                AND c.fecha < CURRENT_DATE
        """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, dni); // Parámetro de búsqueda (DNI del paciente)

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Representar los datos como una lista
                    ObservableList<String> fila = FXCollections.observableArrayList();
                    fila.add(resultSet.getDate("fecha_cita").toString()); // Índice 0: Fecha
                    fila.add(resultSet.getTime("hora_cita_inicio") + " - " + resultSet.getTime("hora_cita_fin")); // Índice 1: Horario
                    fila.add(resultSet.getString("especialidad_medico")); // Índice 2: Especialidad
                    fila.add(resultSet.getString("nombre_medico") + " " + resultSet.getString("apellido_medico")); // Índice 3: Doctor
                    fila.add(String.valueOf(resultSet.getInt("id_cita"))); // Índice 4: ID de la cita

                    citas.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar las citas: " + e.getMessage());
        }

        // Actualizar la tabla en el hilo principal
        Platform.runLater(() -> tablaCitas.setItems(citas));
    }

    private void mostrarResultado(ObservableList<String> fila) {
        if (fila.size() >= 5) {
            try {
                int idCita = Integer.parseInt(fila.get(4));
                String contenidoInforme = generarContenidoInforme(fila);

                // Configurar el FileChooser para guardar el PDF
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Guardar Informe PDF");
                // Sugerir un nombre sencillo basado en el ID de la cita
                fileChooser.setInitialFileName(idCita + ".pdf");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

                Stage currentStage = (Stage) tablaCitas.getScene().getWindow();
                File file = fileChooser.showSaveDialog(currentStage);
                if (file != null) {
                    // Usar la ruta exacta seleccionada por el usuario sin ningún prefijo adicional
                    GenerarPDF.crearPDF(file.getAbsolutePath(), contenidoInforme);
                    System.out.println("Informe generado y guardado en: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("No se pudo generar el informe para la cita seleccionada.");
                System.err.println("Error: " + e.getMessage());
            }
        } else {
            System.err.println("Información de cita inválida. No se puede generar el informe.");
        }
    }


    private String generarContenidoInforme(ObservableList<String> fila) {
        StringBuilder contenido = new StringBuilder();

        // Agregar los datos de la cita al contenido
        contenido.append("Paciente DNI: ").append(this.dni).append("\n"); // Aquí se incluye el DNI
        contenido.append("Fecha: ").append(fila.get(0)).append("\n"); // Índice 0: Fecha
        contenido.append("Horario: ").append(fila.get(1)).append("\n"); // Índice 1: Horario
        contenido.append("Doctor: ").append(fila.get(3)).append("\n"); // Índice 3: Doctor
        contenido.append("Especialidad: ").append(fila.get(2)).append("\n"); // Índice 2: Especialidad
        contenido.append("\nDetalles: Este es un informe médico para la cita. Para más detalles, consulte al especialista.\n");

        return contenido.toString(); // Regresa el contenido formateado del PDF
    }

    public void setDni(String dni) {
        this.dni = dni; // Asignar el DNI del paciente
        cargarResultados(); // Cargar las citas correspondientes
    }

    private void cargarEspecialidades() {
        String query = "SELECT nombre FROM especialidades";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                comboBoxEspecialidad.getItems().add(resultSet.getString("nombre"));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar especialidades: " + e.getMessage());
        }
    }

    private void cargarHospitales() {
        String query = "SELECT nombre, localidad, provincia FROM ubicaciones_hospitales";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                comboBoxHospital.getItems().add(resultSet.getString("nombre") + " - " +
                        resultSet.getString("localidad") + ", " + resultSet.getString("provincia"));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar hospitales: " + e.getMessage());
        }
    }

    @FXML
    private void buscarCitas() {
        cargarResultados(); // Aquí puedes ajustar para filtrar si fuera necesario
    }

    @FXML
    public void handleVolver(){
        System.out.println("Volviendo a la pantalla principal con el DNI del paciente: " + dni);

        try {
            // Cargar el archivo FXML de main-view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/main-view.fxml"));
            Parent mainView = loader.load();

            // Obtener el controlador del MainController
            MainController mainController = loader.getController();

            // Pasar el DNI actual al MainController
            if (mainController != null) {
                mainController.setDni(dni); // Transmitimos el DNI al controlador
            } else {
                System.err.println("Error: El controlador de MainController no pudo ser inicializado.");
            }

            // Cambiar la escena principal a la vista cargada
            Scene currentScene = tablaCitas.getScene(); // Obtenemos la escena actual desde tablaCitas
            currentScene.setRoot(mainView); // Cambiar el contenido raíz de la escena

            System.out.println("Volviendo a la vista principal.");

        } catch (IOException e) {
            System.err.println("Error al intentar cargar la vista principal (main-view.fxml): " + e.getMessage());
            e.printStackTrace();
        }
    }
}