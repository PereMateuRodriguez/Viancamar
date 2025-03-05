package org.example.biancamar;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DetalleCitaController {

    @FXML
    private Label doctorLabel;

    @FXML
    private Label hospitalLabel;

    @FXML
    private Label especialidadLabel;

    private static final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "contraseña_segura_patata_12112";

    @FXML
    private Button crearCitaButton;

    @FXML
    private ScrollPane calendarContainer;

    @FXML
    private Label fechaLabel;

    @FXML
    private ComboBox<String> horariosComboBox;

    private static final DateTimeFormatter FECHA_FORMATTER =
            DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES"));

    private String dniPaciente;
    private int idMedico;
    private Calendar doctorSchedule;

    // Agregar un campo adicional para el ID de la cita
    private int idCita;

    public void setDatos(String dniPaciente, int idMedico, int idCita) {
        this.dniPaciente = dniPaciente;
        this.idMedico = idMedico;
        this.idCita = idCita; // Asignar el ID de la cita

        System.out.println("DNI recibido: " + dniPaciente);
        System.out.println("ID del médico recibido: " + idMedico);
        System.out.println("ID de la cita recibido: " + idCita);

        cargarInformacionMedico(idMedico); // Cargar doctor, hospital y especialidad
        cargarCalendario(idMedico);       // Cargar calendario del doctor
    }
    @FXML
    private void eliminarCita() {
        if (idCita == 0) {
            mostrarAlerta("Error", "No hay una cita seleccionada para eliminar.");
            return;
        }

        // Confirmar la eliminación (Opcional, para evitar eliminaciones accidentales)
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Estás seguro de que deseas eliminar esta cita?");
        if (confirmacion.showAndWait().orElseThrow().getButtonData().isCancelButton()) {
            return; // Salir si el usuario canceló
        }

        // Consulta SQL para borrar la cita
        String eliminarCitaSQL = """
        DELETE FROM citas
        WHERE id_cita = ?;
    """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(eliminarCitaSQL)) {

            stmt.setInt(1, idCita); // ID de la cita a eliminar

            int filasEliminadas = stmt.executeUpdate();
            if (filasEliminadas > 0) {
                mostrarAlerta("Cita Eliminada", "La cita ha sido eliminada exitosamente.");
                idCita = 0; // Reiniciar el valor de idCita después de eliminarla

                // Volver al MainController después de eliminar la cita
                volver();
            }else {
                mostrarAlerta("Error al eliminar cita", "No se pudo eliminar la cita.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error en la base de datos", "Hubo un problema al intentar eliminar la cita.");
        }
    }
    @FXML
    private void initialize() {
        System.out.println("DetalleCita inicializada.");

        // Configurar texto inicial para el botón de crear/modificar cita
        crearCitaButton.setText("Modificar Cita");


        // Configurar función del botón "Crear Cita" para modificar una cita existente
        crearCitaButton.setOnAction(event -> modificarCita(String.valueOf(idCita)));

    }

    private void modificarCita(String idCita) {
        // Obtener el horario seleccionado desde el ComboBox
        String horarioSeleccionado = horariosComboBox.getValue();
        if (horarioSeleccionado == null || horarioSeleccionado.isEmpty()) {
            mostrarAlerta("Horario no seleccionado", "Por favor, selecciona un horario.");
            return;
        }

        // Validar el formato del horario
        String[] horas = horarioSeleccionado.split(" - ");
        if (horas.length != 2) {
            mostrarAlerta("Formato de horario inválido", "El formato del horario seleccionado no es válido.");
            return;
        }

        // Parsear las horas seleccionadas
        LocalTime horaInicio;
        LocalTime horaFin;
        try {
            horaInicio = LocalTime.parse(horas[0]); // Hora de inicio
            horaFin = LocalTime.parse(horas[1]);   // Hora de fin
        } catch (Exception e) {
            mostrarAlerta("Error en el horario", "No se pudo interpretar el horario seleccionado.");
            return;
        }

        // Validar la fecha seleccionada en el Label
        String textoFecha = fechaLabel.getText().replace("Fecha seleccionada: ", "").trim();
        LocalDate fechaCita;
        try {
            // Añadir el año actual al texto de la fecha
            int anioActual = Year.now().getValue();
            String textoFechaConAnio = textoFecha + " de " + anioActual;

            // Usar un patrón que incluya el año
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            fechaCita = LocalDate.parse(textoFechaConAnio, formatter);
        } catch (Exception e) {
            mostrarAlerta("Error en la fecha", "No se pudo interpretar la fecha seleccionada: " + textoFecha);
            return;
        }

        // Consulta SQL para actualizar la cita
        String actualizarCitaSQL = """
    UPDATE citas
    SET fecha = ?, hora_inicio = ?, hora_fin = ?
    WHERE id_cita = ?;
    """;

        // Ejecutar la consulta para actualizar la cita en la base de datos
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(actualizarCitaSQL)) {

            stmt.setDate(1, Date.valueOf(fechaCita));       // Fecha de la cita
            stmt.setTime(2, Time.valueOf(horaInicio));     // Hora de inicio
            stmt.setTime(3, Time.valueOf(horaFin));        // Hora de fin
            stmt.setInt(4, Integer.parseInt(idCita));      // ID de la cita

            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas > 0) {
                mostrarAlerta("Cita Modificada", "La cita ha sido modificada exitosamente.");

                // Volver al MainController después de modificar la cita
                volver();
            }else {
                mostrarAlerta("Error al modificar cita", "No se pudo modificar la cita. Por favor, intenta de nuevo.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error en la base de datos", "Hubo un problema al intentar modificar la cita.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
    private void cargarInformacionMedico(int idMedico) {
        String queryInformacionMedico = """
            SELECT m.nombre AS nombre_doctor, uh.nombre AS hospital, e.nombre AS especialidad
            FROM medicos m
            JOIN ubicaciones_hospitales uh ON uh.id_hospital = m.id_hospital
            JOIN especialidades e ON e.id_especialidad = m.especialidad
            WHERE m.id_medico = ?;
        """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(queryInformacionMedico)) {

            statement.setInt(1, idMedico);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String nombreDoctor = rs.getString("nombre_doctor");
                String hospital = rs.getString("hospital");
                String especialidad = rs.getString("especialidad");

                doctorLabel.setText("Doctor: " + nombreDoctor);
                hospitalLabel.setText("Hospital: " + hospital);
                especialidadLabel.setText("Especialidad: " + especialidad);
            } else {
                doctorLabel.setText("Doctor: No disponible");
                hospitalLabel.setText("Hospital: No disponible");
                especialidadLabel.setText("Especialidad: No disponible");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            doctorLabel.setText("Doctor: No disponible");
            hospitalLabel.setText("Hospital: No disponible");
            especialidadLabel.setText("Especialidad: No disponible");
        }
    }
    private void actualizarFechaLabel(LocalDate fecha) {
        String textoFecha = fecha.format(FECHA_FORMATTER);
        fechaLabel.setText("Fecha seleccionada: " + textoFecha);
    }

    private void cargarCalendario(int idMedico) {
        // Crear la instancia del calendario
        doctorSchedule = new Calendar("Horarios del Médico");

        // Crear el CalendarView
        CalendarView calendarView = new CalendarView();
        calendarView.showDayPage();
        calendarView.setShowSearchField(false);
        calendarView.setShowAddCalendarButton(false);
        calendarView.setShowToolBar(false);
        calendarView.setShowPageToolBarControls(false);
        calendarView.setShowPrintButton(false);
        calendarView.setEntryFactory(param -> null);

        // Configurar la fecha actual y listener para cambios
        LocalDate fechaActual = LocalDate.now();
        actualizarFechaLabel(fechaActual);
        recargarCalendarioPorFecha(fechaActual);

        // Listener para actualizar el calendario según la fecha seleccionada
        calendarView.dateProperty().addListener((observable, oldDate, newDate) -> {
            if (newDate != null) {
                actualizarFechaLabel(newDate);
                recargarCalendarioPorFecha(newDate);
            }
        });

        // Agregar el calendario al CalendarView
        CalendarSource calendarSource = new CalendarSource("Calendarios");
        calendarSource.getCalendars().add(doctorSchedule);
        calendarView.getCalendarSources().add(calendarSource);

        // Mostrar el calendario en el ScrollPane
        calendarContainer.setContent(calendarView);

        System.out.println("Calendario cargado para el idMedico: " + idMedico);
    }
    private void recargarCalendarioPorFecha(LocalDate fechaInicio) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String diaDeLaSemana = fechaInicio.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            diaDeLaSemana = diaDeLaSemana.substring(0, 1).toUpperCase() + diaDeLaSemana.substring(1).toLowerCase();

            String queryCitasPorFecha = """
           SELECT h.hora_inicio AS horario_inicio, h.hora_fin AS horario_fin, 
                  c.fecha AS fecha_cita, c.hora_inicio AS cita_hora_inicio, c.hora_fin AS cita_hora_fin
           FROM horarios_medicos h
           LEFT JOIN citas c
           ON c.id_medico = h.id_medico AND c.fecha = ?
           WHERE h.id_medico = ? AND h.dia_semana = ?
           ORDER BY h.hora_inicio;
           """;

            PreparedStatement statement = connection.prepareStatement(queryCitasPorFecha);
            statement.setDate(1, Date.valueOf(fechaInicio));
            statement.setInt(2, idMedico);
            statement.setString(3, diaDeLaSemana);

            ResultSet rs = statement.executeQuery();

            doctorSchedule.clear();
            horariosComboBox.getItems().clear();
            doctorSchedule.setReadOnly(true);

            Set<String> franjasProcesadas = new HashSet<>();

            while (rs.next()) {
                Time horarioInicio = rs.getTime("horario_inicio");
                Time horarioFin = rs.getTime("horario_fin");
                Time citaHoraInicio = rs.getTime("cita_hora_inicio");
                Time citaHoraFin = rs.getTime("cita_hora_fin");

                if (horarioInicio != null && horarioFin != null) {
                    LocalTime horaInicio = horarioInicio.toLocalTime();
                    LocalTime horaFin = horarioFin.toLocalTime();

                    while (!horaInicio.isAfter(horaFin.minusHours(1))) {
                        LocalTime siguienteHora = horaInicio.plusHours(1);
                        String franjaTexto = horaInicio + " - " + siguienteHora;

                        if (!franjasProcesadas.contains(franjaTexto)) {
                            franjasProcesadas.add(franjaTexto);

                            if (citaHoraInicio != null && !horaInicio.isBefore(citaHoraInicio.toLocalTime())
                                    && !siguienteHora.isAfter(citaHoraFin.toLocalTime())) {
                                // Si ya hay cita en este horario, marcar como "No disponible"
                                horariosComboBox.getItems().add(franjaTexto + " (No disponible)");

                                Entry<String> entradaNoDisponible = new Entry<>("No disponible");
                                entradaNoDisponible.setInterval(
                                        fechaInicio.atTime(horaInicio),
                                        fechaInicio.atTime(siguienteHora)
                                );
                                entradaNoDisponible.getStyleClass().add("entry-no-disponible");
                                doctorSchedule.addEntry(entradaNoDisponible);
                            } else {
                                // Si el horario está libre, marcar como "Disponible"
                                horariosComboBox.getItems().add(franjaTexto);

                                Entry<String> entradaDisponible = new Entry<>("Disponible");
                                entradaDisponible.setInterval(
                                        fechaInicio.atTime(horaInicio),
                                        fechaInicio.atTime(siguienteHora)
                                );
                                entradaDisponible.getStyleClass().add("entry-disponible");
                                doctorSchedule.addEntry(entradaDisponible);
                            }
                        }

                        horaInicio = siguienteHora;
                    }
                }
            }

            if (!horariosComboBox.getItems().isEmpty()) {
                horariosComboBox.getSelectionModel().selectFirst();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }





    @FXML
    private void volver() {
        System.out.println("Volviendo a la pantalla principal con el DNI del paciente: " + dniPaciente);

        try {
            // Cargar el archivo FXML del "main-view" para volver a la vista principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/main-view.fxml"));
            Parent mainView = loader.load();

            // Obtener el controlador del MainController
            MainController mainController = loader.getController();

            // Pasar el DNI al MainController
            mainController.setDni(dniPaciente); // Mandamos el DNI recibido por el controlador

            // Restaurar la vista inicial en el dinámico
            mainController.restoreInitialView();

            // Cambiar la escena actual al main-view
            Scene currentScene = doctorLabel.getScene(); // Obtener la escena actual
            currentScene.setRoot(mainView);

        } catch (IOException e) {
            System.err.println("Error al cargar el main-view.fxml:");
            e.printStackTrace();
        }
    }
}