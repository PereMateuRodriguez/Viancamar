package org.example.biancamar;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PantallaHorarios {
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

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES"));

    private String dniPaciente;
    private int idMedico;
    private Calendar doctorSchedule;

    public void setDatos(String dniPaciente, int idMedico) {
        this.dniPaciente = dniPaciente;
        this.idMedico = idMedico;
        System.out.println("DNI recibido: " + dniPaciente);
        System.out.println("ID del médico recibido: " + idMedico);

        cargarInformacionMedico(idMedico); // Carga el doctor, hospital, y especialidad
        cargarCalendario(idMedico);       // Carga el calendario del doctor
    }
    @FXML
    private void initialize() {
        System.out.println("Pantalla inicializada.");
        // Agregar la hoja de estilos


        // Listener para el ComboBox
        horariosComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Horario seleccionado: " + newValue);
            }
        });

        // Listener para el botón de Crear Cita
        crearCitaButton.setOnAction(event -> crearCita());
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

                // Mostrar la información en las etiquetas
                doctorLabel.setText("Doctor: " + nombreDoctor);
                hospitalLabel.setText("Hospital: " + hospital);
                especialidadLabel.setText("Especialidad: " + especialidad);
            } else {
                // Si no se encuentra información del médico
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


    private void crearCita() {
        String horarioSeleccionado = horariosComboBox.getValue(); // Obtener horario seleccionado
        if (horarioSeleccionado == null || horarioSeleccionado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Horario no seleccionado", "Por favor, seleccione un horario antes de crear una cita.");
            return;
        }

        // Extraer la fecha del texto de la etiqueta
        String textoFecha = fechaLabel.getText().replace("Fecha selecionada: ", "").trim();
        LocalDate fechaCita;
        try {
            int anioActual = Year.now().getValue(); // Obtener el año actual
            String textoFechaConAnio = textoFecha + " de " + anioActual;
            // Convertir el texto con el año al objeto LocalDate
            fechaCita = LocalDate.parse(textoFechaConAnio, DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error en la fecha", "No se pudo analizar la fecha seleccionada: " + textoFecha);
            ex.printStackTrace();
            return;
        }

        // Dividir el horario para obtener la hora de inicio y fin
        String[] horas = horarioSeleccionado.split(" - ");
        if (horas.length != 2) {
            mostrarAlerta(Alert.AlertType.ERROR, "Formato de horario inválido", "El formato del horario seleccionado no es válido.");
            return;
        }
        LocalTime horaInicio = LocalTime.parse(horas[0]); // Hora de inicio
        LocalTime horaFin = LocalTime.parse(horas[1]);   // Hora de fin

        // Consulta para verificar si ya existe una cita en ese horario
        String verificarCitaSQL = """
        SELECT COUNT(*) AS total
        FROM citas
        WHERE id_medico = ? AND fecha = ? AND
              ((hora_inicio <= ? AND hora_fin > ?) OR
               (hora_inicio < ? AND hora_fin >= ?));
        """;

        // Consulta para obtener el id_paciente a partir del dniPaciente
        String obtenerIdPacienteSQL = "SELECT id_paciente FROM pacientes WHERE dni = ?";

        // Consulta para insertar la cita
        String insertarCitaSQL = """
        INSERT INTO citas (id_paciente, id_medico, fecha, hora_inicio, hora_fin, estado)
        VALUES (?, ?, ?, ?, ?, 'Pendiente');
        """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Verificar si ya existe una cita con el mismo doctor, fecha y horario
            try (PreparedStatement verificarStmt = connection.prepareStatement(verificarCitaSQL)) {
                verificarStmt.setInt(1, idMedico);
                verificarStmt.setDate(2, Date.valueOf(fechaCita));
                verificarStmt.setTime(3, Time.valueOf(horaInicio));
                verificarStmt.setTime(4, Time.valueOf(horaInicio));
                verificarStmt.setTime(5, Time.valueOf(horaFin));
                verificarStmt.setTime(6, Time.valueOf(horaFin));

                try (ResultSet rs = verificarStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("total") > 0) {
                        mostrarAlerta(Alert.AlertType.WARNING, "Conflicto de cita", "Ya existe una cita con este doctor en el mismo horario. Por favor, seleccione otro horario.");
                        return;
                    }
                }
            }

            // Obtener el ID del paciente
            int idPaciente = -1;
            try (PreparedStatement obtenerIdStmt = connection.prepareStatement(obtenerIdPacienteSQL)) {
                obtenerIdStmt.setString(1, dniPaciente);
                try (ResultSet rs = obtenerIdStmt.executeQuery()) {
                    if (rs.next()) {
                        idPaciente = rs.getInt("id_paciente");
                    } else {
                        mostrarAlerta(Alert.AlertType.ERROR, "Paciente no encontrado", "No se encontró un paciente con el DNI especificado.");
                        return;
                    }
                }
            }

            // Insertar la cita en la base de datos
            try (PreparedStatement insertarCitaStmt = connection.prepareStatement(insertarCitaSQL)) {
                insertarCitaStmt.setInt(1, idPaciente);
                insertarCitaStmt.setInt(2, idMedico);
                insertarCitaStmt.setDate(3, Date.valueOf(fechaCita));
                insertarCitaStmt.setTime(4, Time.valueOf(horaInicio));
                insertarCitaStmt.setTime(5, Time.valueOf(horaFin));

                int filasAfectadas = insertarCitaStmt.executeUpdate();
                if (filasAfectadas > 0) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Cita creada", "Cita creada exitosamente para el día: " + fechaCita);
                    // Llamar al método volver() para regresar al main
                    volver();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al crear cita", "No se pudo crear la cita, intente nuevamente.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al crear cita", "Hubo un error al crear la cita: " + ex.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String contenido) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null); // Opcional: No queremos un encabezado
        alerta.setContentText(contenido);
        alerta.showAndWait(); // Muestra la alerta y espera a que el usuario la cierre
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
                        String franjaTexto = horaInicio.toString() + " - " + siguienteHora.toString();

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
        System.out.println("Volviendo al menú principal...");

        try {
            // Cargar la vista principal (main-view.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/main-view.fxml"));
            Parent mainView = loader.load();

            // Obtener el controlador del MainController y pasar el DNI, si es necesario
            MainController mainController = loader.getController();
            if (mainController != null) {
                mainController.setDni(this.dniPaciente);
            }

            // Obtener la escena y el Stage actual a partir de un nodo de esta vista (por ejemplo, calendarContainer)
            Scene currentScene = calendarContainer.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            // Si el nodo raíz es una instancia de Region, vincular sus propiedades de tamaño al Stage
            if (mainView instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) mainView;
                region.prefWidthProperty().bind(currentStage.widthProperty());
                region.prefHeightProperty().bind(currentStage.heightProperty());
            }

            // Actualizar la raíz de la escena y el título de la ventana
            currentScene.setRoot(mainView);
            currentStage.setTitle("Menú Principal");
            currentStage.show();

            System.out.println("Regresaste al menú principal con éxito.");
        } catch (IOException e) {
            System.err.println("Error al regresar al menú principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarFechaLabel(LocalDate fecha) {
        String textoFecha = fecha.format(FECHA_FORMATTER);
        fechaLabel.setText("Fecha selecionada: " + textoFecha);
    }

    private void cargarHorarios(LocalTime horaInicio, LocalTime horaFin, LocalDate fecha) {
        // Generar las franjas disponibles desde horaInicio hasta horaFin
        while (!horaInicio.isAfter(horaFin.minusHours(1))) {
            LocalTime siguiente = horaInicio.plusHours(1);

            // Formato de la franja horaria (Ejemplo: "08:00 - 09:00")
            String franja = horaInicio + " - " + siguiente;

            // Agregar la franja al ComboBox
            horariosComboBox.getItems().add(franja);

            // Crear entrada para el calendario
            Entry<String> entrada = new Entry<>("Disponible");
            entrada.setInterval(fecha.atTime(horaInicio), fecha.atTime(siguiente));
            doctorSchedule.addEntry(entrada); // Añadir al calendario

            horaInicio = siguiente;
        }

        // Seleccionar la primera opción en el ComboBox (opcional)
        if (!horariosComboBox.getItems().isEmpty()) {
            horariosComboBox.getSelectionModel().selectFirst();
        }
    }
    public void setDatosParaModificacion(String idCita, String dniPaciente, int idMedico) {
        this.dniPaciente = dniPaciente;
        this.idMedico = idMedico;

        // Consultar los datos de la cita desde la base de datos
        String query = """
        SELECT fecha, hora_inicio, hora_fin 
        FROM citas 
        WHERE id_cita = ?;
    """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, Integer.parseInt(idCita)); // Convertir el ID de cita
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Recuperar los datos de la cita
                LocalDate fechaCita = rs.getDate("fecha").toLocalDate();
                LocalTime horaInicio = rs.getTime("hora_inicio").toLocalTime();
                LocalTime horaFin = rs.getTime("hora_fin").toLocalTime();

                // Actualizar etiquetas y ComboBox
                actualizarFechaLabel(fechaCita);

                String horarioActual = horaInicio + " - " + horaFin;
                horariosComboBox.setValue(horarioActual); // Mostrar el horario actual

                // Cambiar el texto del botón y la acción
                crearCitaButton.setText("Modificar Cita");
                crearCitaButton.setOnAction(event -> modificarCita(idCita));
            }

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar los datos de la cita",
                    "No se pudo cargar la información de la cita. Detalles técnicos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void modificarCita(String idCita) {
        String horarioSeleccionado = horariosComboBox.getValue();

        if (horarioSeleccionado == null || horarioSeleccionado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Horario no seleccionado",
                    "Por favor, selecciona un horario.");
            return;
        }

        String[] horas = horarioSeleccionado.split(" - ");
        if (horas.length != 2) {
            mostrarAlerta(Alert.AlertType.ERROR, "Formato de horario inválido",
                    "El formato del horario seleccionado no es válido.");
            return;
        }

        LocalTime horaInicio = LocalTime.parse(horas[0]);
        LocalTime horaFin = LocalTime.parse(horas[1]);

        // Obtener la fecha seleccionada
        String textoFecha = fechaLabel.getText().replace("Fecha selecionada: ", "").trim();
        LocalDate fechaCita;
        try {
            int anioActual = Year.now().getValue();
            fechaCita = LocalDate.parse(textoFecha + " de " + anioActual, FECHA_FORMATTER);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error en la fecha",
                    "No se pudo interpretar la fecha seleccionada.");
            return;
        }

        // Consulta para actualizar la cita
        String actualizarCitaSQL = """
        UPDATE citas
        SET fecha = ?, hora_inicio = ?, hora_fin = ?
        WHERE id_cita = ?;
    """;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(actualizarCitaSQL)) {

            stmt.setDate(1, Date.valueOf(fechaCita));
            stmt.setTime(2, Time.valueOf(horaInicio));
            stmt.setTime(3, Time.valueOf(horaFin));
            stmt.setInt(4, Integer.parseInt(idCita));

            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas > 0) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Cita Modificada",
                        "La cita ha sido modificada exitosamente.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al modificar la cita",
                        "No se pudo modificar la cita. Por favor, intenta de nuevo.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error en la base de datos",
                    "Hubo un problema al intentar modificar la cita.");
        }
    }
}