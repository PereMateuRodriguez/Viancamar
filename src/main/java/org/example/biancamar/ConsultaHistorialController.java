package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConsultaHistorialController {

    @FXML
    private Label labelFechaCreacion;

    @FXML
    private TextArea textAreaResumen;

    @FXML
    private TextArea textAreaCondiciones;

    @FXML
    private TextArea textAreaNotas;

    @FXML
    private TextArea textAreaMedicamentos;

    @FXML
    private ImageView imageView;

    private String dni; // DNI del paciente a cargar

    public void setDni(String dni) {
        this.dni = dni;
        cargarHistorialMedico();
    }

    @FXML
    public void initialize() {

    }

    private void cargarHistorialMedico() {
        String query = """
            SELECT h.fecha_creacion, h.resumen, h.condiciones_preexistentes,
                   h.notas_adicionales, h.medicamentos_generales
            FROM historial_medico h
            JOIN pacientes p ON h.id_paciente = p.id_paciente
            WHERE p.dni = ?;
        """;

        String url = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
        String user = "test";
        String password = "contraseña_segura_patata_12112";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                labelFechaCreacion.setText(rs.getDate("fecha_creacion").toString());
                textAreaResumen.setText(rs.getString("resumen"));
                textAreaCondiciones.setText(rs.getString("condiciones_preexistentes"));
                textAreaNotas.setText(rs.getString("notas_adicionales"));
                textAreaMedicamentos.setText(rs.getString("medicamentos_generales"));
            } else {
                labelFechaCreacion.setText("No disponible");
                textAreaResumen.setText("No hay información disponible.");
                textAreaCondiciones.setText("No hay condiciones preexistentes registradas.");
                textAreaNotas.setText("No hay notas adicionales disponibles.");
                textAreaMedicamentos.setText("No hay medicamentos registrados.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void descargarPDF() {
        try {
            // Construir el contenido del PDF con la información del historial médico
            StringBuilder contenido = new StringBuilder();

            contenido.append("Fecha de Creación: ").append(labelFechaCreacion.getText()).append("\n\n");
            contenido.append("Resumen:\n").append(textAreaResumen.getText()).append("\n\n");
            contenido.append("Condiciones Preexistentes:\n").append(textAreaCondiciones.getText()).append("\n\n");
            contenido.append("Notas Adicionales:\n").append(textAreaNotas.getText()).append("\n\n");
            contenido.append("Medicamentos Generales:\n").append(textAreaMedicamentos.getText()).append("\n\n");

            // Abrir un cuadro de diálogo para que el usuario elija dónde guardar el archivo
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
            fileChooser.setInitialFileName("HistorialMedico_" + dni + ".pdf");

            Stage stage = (Stage) labelFechaCreacion.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                // Utilizar la clase GenerarPDF para crear el archivo en la ubicación seleccionada
                GenerarPDF.crearPDF(file.getAbsolutePath(), contenido.toString());
                System.out.println("PDF generado: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar el PDF del historial médico.");
        }
    }

    @FXML
    private void volverAlMenuPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/main-view.fxml"));
            Parent menuPrincipalView = loader.load();

            MainController mainController = loader.getController();
            if (mainController != null) {
                mainController.setDni(dni);
            }

            Scene currentScene = labelFechaCreacion.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            if (menuPrincipalView instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) menuPrincipalView;
                region.prefWidthProperty().bind(currentStage.widthProperty());
                region.prefHeightProperty().bind(currentStage.heightProperty());
            }

            currentScene.setRoot(menuPrincipalView);

            System.out.println("Volviendo al menú principal.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar la vista del menú principal.");
        }
    }
}
