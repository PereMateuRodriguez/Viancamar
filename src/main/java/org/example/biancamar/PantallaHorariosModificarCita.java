package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import com.calendarfx.model.Entry;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.view.CalendarView;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PantallaHorariosModificarCita {
    @FXML
    private Label doctorLabel, hospitalLabel, especialidadLabel, fechaLabel;
    @FXML
    private ComboBox<String> horariosComboBox;
    @FXML
    private Button modificarCitaButton;
    @FXML
    private ScrollPane calendarContainer;

    private int idCita; // ID de la cita a modificar
    private int idMedico; // ID del médico
    private String dniPaciente; // DNI del paciente
    private LocalDate fechaCita; // Fecha actual de la cita
    private LocalTime horaInicioCita; // Hora inicial actual de la cita
    private LocalTime horaFinCita; // Hora final actual de la cita

    private Calendar doctorSchedule; // Calendario del médico

    private static final String DB_URL = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "contraseña_segura_patata_12112";

    @FXML
    private void initialize() {
        System.out.println("Pantalla de modificación inicializada.");

        horariosComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Horario seleccionado: " + newValue);
            }
        });

        modificarCitaButton.setOnAction(event -> modificarCita());
    }

    public void cargarDatosCita(int idCita, String dniPaciente, int idMedico, LocalDate fechaCita, LocalTime horaInicio, LocalTime horaFin) {
        this.idCita = idCita;
        this.dniPaciente = dniPaciente;
        this.idMedico = idMedico;
        this.fechaCita = fechaCita;
        this.horaInicioCita = horaInicio;
        this.horaFinCita = horaFin;

        System.out.println("Cargando la cita con ID: " + idCita);

        cargarInformacionMedico(idMedico);
        cargarCalendario(idMedico);

        // Establece la fecha y el horario seleccionados previamente
        fechaLabel.setText("Fecha seleccionada: " + fechaCita.format(DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES"))));
        horariosComboBox.setValue(horaInicio + " - " + horaFin); // Seleccionar el rango actual de la cita
    }

    private void cargarInformacionMedico(int idMedico) {
        // Idéntico al método en tu código original
        // Carga el nombre del médico, hospital y especialidad
    }

    private void cargarCalendario(int idMedico) {
        // Configuración idéntica del calendario
        doctorSchedule = new Calendar("Horarios del Médico");

        CalendarView calendarView = new CalendarView();
        calendarView.showDayPage();
        calendarView.setShowSearchField(false);

        LocalDate fechaActual = fechaCita; // Use la fecha actual de la cita
        actualizarFechaLabel(fechaActual);
        recargarCalendarioPorFecha(fechaActual);

        calendarView.dateProperty().addListener((observable, oldDate, newDate) -> {
            if (newDate != null) {
                actualizarFechaLabel(newDate);
                recargarCalendarioPorFecha(newDate);
            }
        });

        CalendarSource calendarSource = new CalendarSource("Calendarios");
        calendarSource.getCalendars().add(doctorSchedule);
        calendarView.getCalendarSources().add(calendarSource);

        calendarContainer.setContent(calendarView);
    }

    private void recargarCalendarioPorFecha(LocalDate fechaInicio) {
        // Similar a tu método original, pero marca la hora actual de la cita como seleccionada
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String diaDeLaSemana = fechaInicio.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            diaDeLaSemana = diaDeLaSemana.substring(0, 1).toUpperCase() + diaDeLaSemana.substring(1).toLowerCase();

            String queryCitasPorFecha = """
            SELECT h.hora_inicio AS horario_inicio, h.hora_fin AS horario_fin, 
                   c.hora_inicio AS cita_hora_inicio, c.hora_fin AS cita_hora_fin
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

                if (horarioInicio != null && horarioFin != null) {
                    LocalTime horaInicio = horarioInicio.toLocalTime();
                    LocalTime horaFin = horarioFin.toLocalTime();

                    while (!horaInicio.isAfter(horaFin.minusHours(1))) {
                        LocalTime siguienteHora = horaInicio.plusHours(1);
                        String franjaTexto = horaInicio.toString() + " - " + siguienteHora.toString();

                        if (!franjasProcesadas.contains(franjaTexto)) {
                            franjasProcesadas.add(franjaTexto);

                            if (fechaInicio.equals(fechaCita) &&
                                    horaInicio.equals(horaInicioCita) &&
                                    siguienteHora.equals(horaFinCita)) {
                                horariosComboBox.getItems().add(franjaTexto + " (Actual)");
                            } else {
                                horariosComboBox.getItems().add(franjaTexto + " (Disponible)");
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

    private void modificarCita() {
        // Código para realizar un UPDATE de la cita en la base de datos
        String horarioSeleccionado = horariosComboBox.getValue();
        if (horarioSeleccionado == null || horarioSeleccionado.isEmpty()) {
            mostrarAlerta(AlertType.WARNING, "Horario no seleccionado", "Por favor, seleccione un horario antes de modificar la cita.");
            return;
        }

        // Similar al método `crearCita`, pero con un `UPDATE` en lugar de un `INSERT`
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }

    private void actualizarFechaLabel(LocalDate fecha) {
        String textoFecha = fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES")));
        fechaLabel.setText("Fecha seleccionada: " + textoFecha);
    }
}