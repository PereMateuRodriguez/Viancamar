package org.example.biancamar;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class CrearCitaController {

    @FXML
    private TextField txtBuscarDoctor;

    @FXML
    private ListView<String> listResultadosDoctores;

    @FXML
    private Label lblDoctorSeleccionado;

    @FXML
    private DatePicker datePickerFecha;

    @FXML
    private TextField txtHora;

    @FXML
    private Button btnGuardarCita;

    private String doctorSeleccionado;

    private static final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "contraseña_segura_patata_12112";

    /**
     * Método que se ejecuta cuando el usuario escribe en el campo de búsqueda (TextField).
     */
    @FXML
    private void buscarDoctor() {
        String nombreBusqueda = txtBuscarDoctor.getText().trim();

        if (nombreBusqueda.isEmpty()) {
            listResultadosDoctores.setItems(FXCollections.observableArrayList());
            return;
        }

        String query = """
                SELECT 
                    CONCAT(m.nombre, ' ', m.apellido, ' - Especialidad: ', e.nombre, ' - Hospital: ', h.nombre) AS doctor_info
                FROM 
                    medicos m
                JOIN 
                    ubicaciones_hospitales h ON m.id_hospital = h.id_hospital
                JOIN 
                    especialidades e ON m.especialidad = e.id_especialidad
                WHERE
                    LOWER(m.nombre) LIKE LOWER(?) OR LOWER(m.apellido) LIKE LOWER(?);
                """;

        ObservableList<String> resultados = FXCollections.observableArrayList();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, "%" + nombreBusqueda + "%");
            stmt.setString(2, "%" + nombreBusqueda + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                resultados.add(rs.getString("doctor_info"));
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar doctores: " + e.getMessage());
            e.printStackTrace();
        }

        listResultadosDoctores.setItems(resultados);
    }

    /**
     * Método para seleccionar un doctor de la lista.
     */
    @FXML
    private void seleccionarDoctor(MouseEvent event) {
        doctorSeleccionado = listResultadosDoctores.getSelectionModel().getSelectedItem();

        if (doctorSeleccionado != null) {
            lblDoctorSeleccionado.setText("Doctor seleccionado: " + doctorSeleccionado);

            // Habilitar DatePicker y campos relacionados
            datePickerFecha.setDisable(false);
            txtHora.setDisable(false);
            btnGuardarCita.setDisable(false);
        }
    }

    /**
     * Método para guardar la cita después de completar todos los datos.
     */
    @FXML
    private void guardarCita() {
        if (doctorSeleccionado == null) {
            System.out.println("Error: No se ha seleccionado un doctor.");
            return;
        }

        LocalDate fechaSeleccionada = datePickerFecha.getValue();
        String hora = txtHora.getText();

        if (fechaSeleccionada == null || hora == null || hora.isEmpty()) {
            System.out.println("Error: Debe seleccionar una fecha y una hora.");
            return;
        }

        // Guardar Cita en la base de datos
        String query = """
                INSERT INTO citas (fecha, hora, doctor) 
                VALUES (?, ?, ?);
                """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, fechaSeleccionada.toString());
            stmt.setString(2, hora);
            stmt.setString(3, doctorSeleccionado);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Cita guardada correctamente.");
            }

        } catch (SQLException e) {
            System.out.println("Error al guardar la cita: " + e.getMessage());
            e.printStackTrace();
        }
    }
}