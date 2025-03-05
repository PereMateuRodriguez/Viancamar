package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GestionCitasController {

    // Credenciales de la base de datos
    private final String dbUrl = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";       // URL de la base de datos
    private final String dbUser = "test";     // Usuario de la base de datos
    private final String dbPass = "contraseña_segura_patata_12112";     // Contraseña de la base de datos

    @FXML
    private TextField textFieldBuscarDoctor;

    @FXML
    private VBox vBoxResultados;

    @FXML
    private ComboBox<String> comboBoxEspecialidad;

    @FXML
    private ComboBox<String> comboBoxHospitales;

    @FXML
    private Button botonSiguiente; // Botón "Siguiente" que será habilitado/deshabilitado dinámicamente

    private String dniPaciente;

    // Inicialización de la vista
    @FXML
    private void initialize() {
        System.out.println("Inicializando la vista...");
        cargarEspecialidades(); // Carga especialidades en el ComboBox
        cargarHospitales();     // Carga hospitales en el ComboBox

        // Configurar el botón "Siguiente"
        botonSiguiente.setOnAction(event -> irPantallaSiguiente());
        botonSiguiente.setDisable(true); // Inicialmente, se deshabilita el botón

        // Agregar listeners dinámicos a los CheckBoxes existentes
        agregarListenersACheckBoxes();

        // Agregar listener para actualizar la búsqueda en tiempo real
        textFieldBuscarDoctor.textProperty().addListener((observable, oldValue, newValue) -> {
            buscarDoctor();

        });
        comboBoxEspecialidad.valueProperty().addListener((observable, oldValue, newValue) -> {
            buscarDoctor();
        });

        comboBoxHospitales.valueProperty().addListener((observable, oldValue, newValue) -> {
            buscarDoctor();
        });
    }


    private void mostrarResultados(List<DoctorInfo> doctores) {
        vBoxResultados.getChildren().clear(); // Limpiar resultados previos

        // Si no hay coincidencias, muestra un mensaje
        if (doctores.isEmpty()) {
            Label noResultados = new Label("No se encontraron resultados.");
            noResultados.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
            vBoxResultados.getChildren().add(noResultados);
            botonSiguiente.setDisable(true); // Deshabilitar el botón si no hay resultados
            return;
        }

        // Crear y añadir una tarjeta por cada doctor
        for (DoctorInfo doctor : doctores) {
            HBox tarjetaDoctor = crearTarjetaDoctor(doctor);
            vBoxResultados.getChildren().add(tarjetaDoctor);
        }

        // Volver a agregar listeners a los CheckBoxes
        agregarListenersACheckBoxes();
    }

    private HBox crearTarjetaDoctor(DoctorInfo doctor) {
        // Crear contenedor principal (HBox) para la tarjeta
        HBox tarjeta = new HBox(10);
        tarjeta.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px; -fx-padding: 10px; -fx-background-color: #f9f9f9;");

        // Información del doctor
        Label nombreCompleto = new Label(doctor.getNombre() + " " + doctor.getApellido());
        nombreCompleto.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label especialidad = new Label("Especialidad: " + doctor.getEspecialidad());
        Label hospital = new Label("Hospital: " + doctor.getHospital());
        Label horario = new Label("Horario: " + doctor.getHorarioDisponible());
        horario.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        VBox infoDoctor = new VBox(5, nombreCompleto, especialidad, hospital, horario);

        // Checkbox para seleccionar al doctor
        CheckBox checkBox = new CheckBox();
        checkBox.setOnAction(event -> manejarSeleccion(checkBox)); // Agregar evento al CheckBox

        tarjeta.getChildren().addAll(checkBox, infoDoctor); // Añade elementos al contenedor

        // Agregar metadata (doctor) al HBox
        tarjeta.setUserData(doctor);

        return tarjeta;
    }

    private void manejarSeleccion(CheckBox checkBoxSeleccionado) {
        if (checkBoxSeleccionado.isSelected()) {
            // Desmarcar otros CheckBoxes
            for (Node node : vBoxResultados.getChildren()) {
                if (node instanceof HBox hbox) {
                    for (Node innerNode : hbox.getChildren()) {
                        if (innerNode instanceof CheckBox checkBox && checkBox != checkBoxSeleccionado) {
                            checkBox.setSelected(false);
                        }
                    }
                }
            }
        }

        actualizarEstadoBotonSiguiente(); // Verificar si el botón debe habilitarse
    }

    // Método que actualiza el estado del botón "Siguiente"
    private void actualizarEstadoBotonSiguiente() {
        boolean haySeleccionado = vBoxResultados.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .flatMap(hbox -> hbox.getChildren().stream())
                .anyMatch(innerNode -> innerNode instanceof CheckBox checkBox && checkBox.isSelected());

        // Habilitar o deshabilitar el botón dependiendo de la selección
        botonSiguiente.setDisable(!haySeleccionado);
    }

    // Agregar listeners automáticamente a los CheckBoxes cuando se generan nuevas tarjetas
    private void agregarListenersACheckBoxes() {
        vBoxResultados.getChildren().forEach(node -> {
            if (node instanceof HBox hbox) {
                hbox.getChildren().forEach(innerNode -> {
                    if (innerNode instanceof CheckBox checkBox) {
                        checkBox.setOnAction(event -> manejarSeleccion(checkBox));
                    }
                });
            }
        });
    }

    @FXML
    private void buscarDoctor() {
        System.out.println("Buscando doctor...");

        // Recoger los filtros
        String nombreABuscar = textFieldBuscarDoctor.getText().trim();
        String especialidadSeleccionada = comboBoxEspecialidad.getValue();
        String hospitalSeleccionado = comboBoxHospitales.getValue();

        // Filtrar doctores
        List<DoctorInfo> doctores = buscarDoctoresEnBaseDeDatos(nombreABuscar, especialidadSeleccionada, hospitalSeleccionado);
        mostrarResultados(doctores);
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
            Stage currentStage = (Stage) vBoxResultados.getScene().getWindow();

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


    @FXML
    private void irPantallaSiguiente() {
        System.out.println("Pasando a la pantalla siguiente...");

        try {
            // Cargar el archivo FXML de la nueva pantalla
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/pantalla-horarios.fxml"));
            Parent root = loader.load();

            // Obtener el controlador de la nueva pantalla
            PantallaHorarios pantallaHorariosController = loader.getController();

            // Obtener los datos seleccionados
            String dniPacienteSeleccionado = this.dniPaciente; // DNI actual del paciente
            int idMedicoSeleccionado = obtenerIdMedicoSeleccionado(); // ID del médico seleccionado

            // Pasar los datos al controlador de la nueva pantalla
            pantallaHorariosController.setDatos(dniPacienteSeleccionado, idMedicoSeleccionado);

            // Cambiar la escena
            Scene nuevaEscena = new Scene(root);


            // Crear un nuevo Stage (VENTANA NUEVA en pantalla completa para PantallaHorarios)
            Stage nuevoStage = new Stage();
            nuevaEscena.getStylesheets().add(getClass().getResource("calendar-styles.css").toExternalForm());
            nuevoStage.setScene(nuevaEscena);
            nuevoStage.setTitle("Pantalla Horarios");
            nuevoStage.setMaximized(true);
            nuevoStage.show();



            // Cerrar la ventana actual, si es necesario
            Stage stageActual = (Stage) vBoxResultados.getScene().getWindow();
            stageActual.close(); // Opcional: Esto es si quieres que sólo la nueva pantalla permanezca abierta

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo cargar la pantalla siguiente: " + e.getMessage());
        }
    }
    // Método auxiliar para obtener el ID del médico seleccionado
    private int obtenerIdMedicoSeleccionado() {
        for (Node node : vBoxResultados.getChildren()) {
            if (node instanceof HBox hbox) {
                for (Node innerNode : hbox.getChildren()) {
                    if (innerNode instanceof CheckBox checkBox && checkBox.isSelected()) {
                        // Aquí asumimos que el ID del médico está como un dato interno de DoctorInfo
                        DoctorInfo doctorInfo = (DoctorInfo) hbox.getUserData();
                        return doctorInfo.getId();
                    }
                }
            }
        }
        throw new IllegalStateException("No se seleccionó ningún médico.");
    }

    public void setDni(String dniPaciente) {
        this.dniPaciente = dniPaciente;
        System.out.println("DNI recibido: " + dniPaciente);
    }

    private void cargarEspecialidades() {
        verificarConfiguracionBaseDeDatos();

        String consultaEspecialidades = "SELECT nombre FROM especialidades";

        try (Connection conexion = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement statement = conexion.createStatement();
             ResultSet rs = statement.executeQuery(consultaEspecialidades)) {

            comboBoxEspecialidad.getItems().clear();

            while (rs.next()) {
                comboBoxEspecialidad.getItems().add(rs.getString("nombre"));
            }

        } catch (SQLException e) {
            mostrarError("Error al cargar las especialidades: " + e.getMessage());
        }
    }

    private void cargarHospitales() {
        verificarConfiguracionBaseDeDatos();

        String consultaHospitales = """
        SELECT nombre, localidad, provincia
        FROM ubicaciones_hospitales;
        """;

        try (Connection conexion = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement statement = conexion.createStatement();
             ResultSet rs = statement.executeQuery(consultaHospitales)) {

            comboBoxHospitales.getItems().clear();

            while (rs.next()) {
                comboBoxHospitales.getItems().add(String.format("%s - %s, %s",
                        rs.getString("nombre"), rs.getString("localidad"), rs.getString("provincia")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al cargar los hospitales: " + e.getMessage());
        }
    }

    private List<DoctorInfo> buscarDoctoresEnBaseDeDatos(String nombre, String especialidad, String hospital) {
        List<DoctorInfo> doctores = new ArrayList<>();
        verificarConfiguracionBaseDeDatos();

        String consultaSQL = """
            SELECT m.id_medico, m.nombre AS nombre_medico, m.apellido AS apellido_medico,
            e.nombre AS especialidad, h.nombre AS hospital,
            string_agg(hm.dia_semana || ' ' || CAST(hm.hora_inicio AS varchar) || '-' || CAST(hm.hora_fin AS varchar), ', ') AS horarios
            FROM medicos m
            JOIN especialidades e ON m.especialidad = e.id_especialidad
            JOIN ubicaciones_hospitales h ON m.id_hospital = h.id_hospital
            LEFT JOIN horarios_medicos hm ON m.id_medico = hm.id_medico
            WHERE (LOWER(m.nombre) LIKE LOWER(?) OR LOWER(m.apellido) LIKE LOWER(?))
        """;

        if (especialidad != null && !especialidad.isEmpty()) {
            consultaSQL += " AND LOWER(e.nombre) = LOWER(?) ";
        }
        if (hospital != null && !hospital.isEmpty()) {
            consultaSQL += " AND LOWER(h.nombre) = LOWER(?) ";
        }

        consultaSQL += " GROUP BY m.id_medico, m.nombre, m.apellido, e.nombre, h.nombre;";

        try (Connection conexion = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conexion.prepareStatement(consultaSQL)) {

            ps.setString(1, "%" + nombre + "%");
            ps.setString(2, "%" + nombre + "%");

            int paramIndex = 3;

            if (especialidad != null && !especialidad.isEmpty()) {
                ps.setString(paramIndex++, especialidad);
            }
            if (hospital != null && !hospital.isEmpty()) {
                String nombreHospital = hospital.split(" - ")[0];
                ps.setString(paramIndex, nombreHospital);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                doctores.add(new DoctorInfo(
                        rs.getInt("id_medico"),
                        rs.getString("nombre_medico"),
                        rs.getString("apellido_medico"),
                        rs.getString("especialidad"),
                        rs.getString("hospital"),
                        rs.getString("horarios") == null ? "No disponible" : rs.getString("horarios")
                ));
            }

        } catch (SQLException e) {
            mostrarError("Error al conectar con la base de datos: " + e.getMessage());
        }

        return doctores;
    }

    private void verificarConfiguracionBaseDeDatos() {
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new RuntimeException("Faltan variables de entorno para la conexión a la base de datos.");
        }
    }

    private void mostrarError(String mensaje) {
        System.err.println("Error: " + mensaje);
    }
}