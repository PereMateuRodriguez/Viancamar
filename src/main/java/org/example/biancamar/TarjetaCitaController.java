package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import java.io.File;

public class TarjetaCitaController {

    @FXML
    private Label labelFecha;
    @FXML
    private Label labelHorario;
    @FXML
    private Label labelMedico;
    @FXML
    private Label labelEspecialidad;
    @FXML
    private Button btnVerResultado;

    private int idCita;
    private String fecha, horario, medico, especialidad;

    // Método para asignar datos a la tarjeta
    public void setDatos(String fecha, String horario, String medico, String especialidad, int idCita) {
        this.fecha = fecha;
        this.horario = horario;
        this.medico = medico;
        this.especialidad = especialidad;
        this.idCita = idCita;

        labelFecha.setText(fecha);
        labelHorario.setText(horario);
        labelMedico.setText(medico);
        labelEspecialidad.setText(especialidad);
    }

    @FXML
    private void handleVerResultado() {
        // Lógica similar a la del método mostrarResultado en ConsultaResultadosController.
        // Aquí podrías, por ejemplo, generar el PDF o mostrar otro diálogo con los detalles.
        try {
            // Suponiendo que tienes un método estático para generar el contenido del informe
            String contenidoInforme = "Paciente DNI: ...\n" +
                    "Fecha: " + fecha + "\n" +
                    "Horario: " + horario + "\n" +
                    "Doctor: " + medico + "\n" +
                    "Especialidad: " + especialidad + "\n\n" +
                    "Detalles: Este es un informe médico para la cita.";

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Informe PDF");
            fileChooser.setInitialFileName(String.valueOf(idCita));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

            // Obtener la ventana actual (puedes ajustar esto según tu arquitectura)
            Stage stage = (Stage) btnVerResultado.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                // Llama a tu método para generar el PDF, por ejemplo:
                GenerarPDF.crearPDF(file.getAbsolutePath(), contenidoInforme);
                System.out.println("Informe generado en: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error al generar el informe: " + e.getMessage());
        }
    }
}
