package org.example.biancamar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlador principal de la aplicacion que gestiona la lista de citas y la navegacion a otras pantallas.
 */
public class MainController {
    @FXML
    private VBox citasContainer;



    @FXML
    private StackPane dynamicContent; // Contenedor dinamico central

    @FXML
    private Button btnGestionCita;

    @FXML
    private Button btnGestionUsuario;

    private String dni; // Almacena el DNI del cliente

    private Node initialContent; // Guarda el contenido inicial del StackPane (menu principal)

    /**
     * Se llama automaticamente al cargar la vista FXML.
     */
    @FXML
    private void initialize() {
        if (!dynamicContent.getChildren().isEmpty()) {
            initialContent = dynamicContent.getChildren().get(0);
            System.out.println("Contenido inicial guardado: " + initialContent);
        } else {
            System.err.println("Error: El StackPane dinámico no contiene contenido inicial.");
        }




        // Mostrar mensaje de carga inicial en el contenedor
        Label cargandoLabel = new Label("Cargando citas...");
        cargandoLabel.getStyleClass().add("cita-text");
        citasContainer.getChildren().clear();
        citasContainer.getChildren().add(cargandoLabel);

        // Cargar citas del cliente
        cargarCitasDelCliente();
        showCentralContent();

    }

    /**
     * Establece el DNI del paciente/cliente y carga sus citas.
     * @param dni DNI del paciente
     */
    public void setDni(String dni) {
        this.dni = dni;
        System.out.println("DNI recibido en MainController: " + dni);
        cargarCitasDelCliente();
    }

    /**
     * Carga las citas del cliente desde la base de datos y las muestra en el ListView.
     */

    private void cargarCitasDelCliente() {
        citasContainer.getChildren().clear();

        if (dni == null || dni.isEmpty()) {
            Label noDni = new Label("No se encontró DNI. No hay citas para mostrar.");
            citasContainer.getChildren().add(noDni);
            return;
        }

        // Conexión y consulta a la base de datos...
        String url = "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar";
        String user = "test";
        String password = "contraseña_segura_patata_12112";

        String query = """
            SELECT c.id_cita, c.fecha, c.hora_inicio, c.hora_fin,
                   m.nombre AS medico_nombre, m.apellido AS medico_apellido
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN medicos m ON c.id_medico = m.id_medico
            WHERE p.dni = ?
            ORDER BY c.fecha, c.hora_inicio
            """;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dni);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    Label sinCitas = new Label("No se encontraron citas para este cliente.");
                    citasContainer.getChildren().add(sinCitas);
                } else {
                    while (rs.next()) {
                        String idCita = rs.getString("id_cita");
                        String fecha = rs.getDate("fecha").toString();
                        String horaInicio = rs.getTime("hora_inicio").toString();
                        String horaFin = rs.getTime("hora_fin").toString();
                        String medicoNombre = rs.getString("medico_nombre");
                        String medicoApellido = rs.getString("medico_apellido");

                        // Formateamos la información en varias líneas
                        StringBuilder sb = new StringBuilder();
                        sb.append("ID: ").append(idCita).append("\n");
                        sb.append("Fecha: ").append(fecha).append("\n");
                        sb.append("Hora: ").append(horaInicio).append(" - ").append(horaFin).append("\n");
                        sb.append("Medico: ").append(medicoNombre).append(" ").append(medicoApellido);

                        // Agrega la tarjeta al contenedor
                        citasContainer.getChildren().add(createCitaCard(sb.toString()));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error al cargar las citas del cliente.");
            citasContainer.getChildren().add(errorLabel);
        }
    }

    private Node createCitaCard(String citaData) {
        VBox card = new VBox(5);
        card.getStyleClass().add("cita-item");

        // Separamos la información de la cita (suponiendo que está en líneas)
        String[] lineas = citaData.split("\n");
        for (String linea : lineas) {
            Label label = new Label(linea);
            label.getStyleClass().add("cita-text");
            card.getChildren().add(label);
        }

        Button btnDetalle = new Button("Detalle");
        btnDetalle.getStyleClass().add("cita-button");
        btnDetalle.setOnAction(e -> mostrarDetalleCita(citaData));
        card.getChildren().add(btnDetalle);

        return card;
    }

    public void showCentralContent() {
        // Contenedor principal para todas las secciones
        VBox mainContainer = new VBox(40);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(30));

        // ============================
        // Sección 1: ¿Quiénes Somos?
        // ============================
        VBox quienesSomosSection = new VBox(10);
        quienesSomosSection.setAlignment(Pos.TOP_LEFT);
        Label quienesSomosTitle = new Label("¿Quiénes Somos?");
        quienesSomosTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label quienesSomosContent = new Label("Somos una organización comprometida con la excelencia en la atención médica. " +
                "Nuestro objetivo es brindar servicios de salud de alta calidad, combinando tecnología de punta con un equipo de profesionales altamente capacitados.");
        quienesSomosContent.setWrapText(true);
        quienesSomosContent.setStyle("-fx-font-size: 16px; -fx-text-fill: #555;");
        quienesSomosSection.getChildren().addAll(quienesSomosTitle, quienesSomosContent);

        // ============================
        // Sección 2: Los Hospitales
        // ============================
        VBox hospitalesSection = new VBox(20);
        hospitalesSection.setAlignment(Pos.TOP_CENTER);
        hospitalesSection.setPadding(new Insets(20));
        Label hospitalesTitle = new Label("🏥 Nuestros Hospitales");
        hospitalesTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");
        FlowPane hospitalsPane = new FlowPane();
        hospitalsPane.setHgap(30);
        hospitalsPane.setVgap(30);
        hospitalsPane.setPadding(new Insets(20));
        hospitalsPane.setAlignment(Pos.CENTER);
        hospitalsPane.getChildren().addAll(
                createDoctorCard("Hospital Central", "Manacor", "/Imagenes/hospital1.jpg",
                        "Cuenta con tecnología avanzada y especialistas de renombre."),
                createDoctorCard("Hospital Regional", "Son Macia", "/Imagenes/hospital2.jpg",
                        "Atención integral y equipos de última generación."),
                createDoctorCard("Clínica Especializada", "Porto Cristo", "/Imagenes/hospital3.jpg",
                        "Líder en tratamientos de alta complejidad.")
        );
        hospitalesSection.getChildren().addAll(hospitalesTitle, hospitalsPane);

        // ============================
        // Sección 3: Doctores Más Relevantes (6 doctores en 2 filas de 3)
        // ============================
        VBox doctorsSection = new VBox(20);
        doctorsSection.setAlignment(Pos.TOP_CENTER);
        Label doctorsTitle = new Label("👨‍⚕️ Doctores Más Relevantes");
        doctorsTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // GridPane para mostrar 6 doctores en 2 filas de 3 columnas
        GridPane doctorGridPane = new GridPane();
        doctorGridPane.setHgap(30);
        doctorGridPane.setVgap(30);
        doctorGridPane.setPadding(new Insets(20));
        doctorGridPane.setAlignment(Pos.CENTER);

        // Lista de 6 doctores
        Node[] doctorCards = new Node[]{
                createDoctorCard("Dr. Juan Pérez", "Cardiología", "/Imagenes/doctor1.jpg", "Especialista en cirugía cardíaca con más de 20 años de experiencia."),
                createDoctorCard("Dra. María López", "Neurología", "/Imagenes/doctor2.jpg", "Innovadora en neurocirugía y tratamiento de trastornos neurológicos."),
                createDoctorCard("Dr. Carlos Rodríguez", "Oncología", "/Imagenes/doctor3.jpg", "Experto en terapias oncológicas personalizadas."),
                createDoctorCard("Dra. Ana Torres", "Pediatría", "/Imagenes/doctor4.jpg", "Especialista en el cuidado infantil y enfermedades pediátricas."),
                createDoctorCard("Dr. Manuel García", "Ortopedia", "/Imagenes/doctor5.jpg", "Cirujano ortopédico reconocido por su precisión en cirugías de columna."),
                createDoctorCard("Dra. Laura Fernández", "Ginecología", "/Imagenes/doctor6.jpg", "Líder en tratamientos ginecológicos y salud femenina.")
        };

        // Agregar doctores al GridPane en una distribución de 2 filas x 3 columnas
        int column = 0, row = 0;
        for (Node doctorCard : doctorCards) {
            doctorGridPane.add(doctorCard, column, row);
            column++;
            if (column == 3) { // Pasa a la siguiente fila después de 3 columnas
                column = 0;
                row++;
            }
        }

        doctorsSection.getChildren().addAll(doctorsTitle, doctorGridPane);

        // ============================
        // Sección 4: Noticias
        // ============================
        VBox newsSection = new VBox(20);
        newsSection.setAlignment(Pos.TOP_CENTER);
        Label newsTitle = new Label("📰 Últimas Noticias");
        newsTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox newsCardsBox = new VBox(20);
        newsCardsBox.setAlignment(Pos.CENTER);
        newsCardsBox.getChildren().addAll(
                createNewsCard("Inauguración de Clínica", "Se inaugura una nueva clínica con tecnología de punta."),
                createNewsCard("Campaña de Prevención", "Biancamar lanza una campaña de prevención y detección temprana."),
                createNewsCard("Estudio en Oncología", "Nuevo estudio demuestra avances en tratamientos personalizados.")
        );
        newsSection.getChildren().addAll(newsTitle, newsCardsBox);

        // ============================
        // Agregar todas las secciones
        // ============================
        mainContainer.getChildren().addAll(quienesSomosSection, hospitalesSection, doctorsSection, newsSection);

        // ============================
        // ScrollPane para permitir desplazamiento
        // ============================
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Reemplazar el contenido dinámico central
        dynamicContent.getChildren().clear();
        dynamicContent.getChildren().add(scrollPane);
    }


    private Node createHospitalCard(String name, String city, String imagePath, String description) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.5, 0, 0);");
        card.setPrefWidth(400); // Aumenta el ancho de la tarjeta si necesitas más espacio para las imágenes

        // Imagen del hospital
        ImageView imageView = new ImageView();
        imageView.setFitHeight(300); // Incrementar la altura (prueba con 300+ px)
        imageView.setFitWidth(450);  // Incrementar el ancho (prueba con 450+ px)
        imageView.setPreserveRatio(true); // Mantener las proporciones

        InputStream is = getClass().getResourceAsStream(imagePath);
        if (is != null) {
            imageView.setImage(new Image(is));
        } else {
            System.out.println("No se encontró la imagen en: " + imagePath);
        }

        Label nameLabel = new Label(name + " - " + city);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        card.getChildren().addAll(imageView, nameLabel, descLabel);
        return card;
    }


    /**
     * Crea una tarjeta para una noticia sin imagen, con título y descripción.
     */
    private Node createNewsCard(String title, String description) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.5, 0, 0);");
        card.setPrefWidth(300);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        card.getChildren().addAll(titleLabel, descLabel);
        return card;
    }

    /**
     * Crea una tarjeta para un doctor con imagen, nombre, especialidad y descripción.
     */
    private Node createDoctorCard(String name, String specialty, String imagePath, String description) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.5, 0, 0);");
        card.setPrefWidth(250);  // Tamaño fijo para todas las tarjetas
        card.setPrefHeight(300); // Altura fija para evitar que una tarjeta sea más alta que otra

        // Imagen del doctor
        ImageView imageView = new ImageView();
        imageView.setFitHeight(100);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        InputStream is = getClass().getResourceAsStream(imagePath);
        if (is != null) {
            imageView.setImage(new Image(is));
        } else {
            System.out.println("No se encontró la imagen en: " + imagePath);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(230);

        Label specialtyLabel = new Label(specialty);
        specialtyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #777;");
        specialtyLabel.setWrapText(true);
        specialtyLabel.setMaxWidth(230);

        // Descripción con ajuste de texto para que no se corte
        TextFlow descriptionFlow = new TextFlow(new Text(description));
        descriptionFlow.setMaxWidth(230);
        descriptionFlow.setPadding(new Insets(5, 5, 5, 5));

        card.getChildren().addAll(imageView, nameLabel, specialtyLabel, descriptionFlow);
        return card;
    }



    /**
     * Clase interna para personalizar la forma de mostrar cada cita en el ListView.
     */
    private class CitaListCell extends ListCell<String> {
        private final HBox content;
        private final VBox textContainer;
        private final Label idLabel;
        private final Label fechaLabel;
        private final Label horaLabel;
        private final Label medicoLabel;
        private final Button actionButton;

        // Patron para extraer datos del string multilinea (ID, Fecha, Hora, Medico)
        private final Pattern pattern = Pattern.compile(
                "ID: (.*?)\\nFecha: (.*?)\\nHora: (.*?)\\nMedico: (.*)");

        public CitaListCell() {
            idLabel = new Label();
            fechaLabel = new Label();
            horaLabel = new Label();
            medicoLabel = new Label();
            actionButton = new Button("Detalle");

            // Configurar wrapText para que se vea completo
            idLabel.setWrapText(true);
            fechaLabel.setWrapText(true);
            horaLabel.setWrapText(true);
            medicoLabel.setWrapText(true);

            // Ajustar estilos basicos
            idLabel.getStyleClass().add("cita-text");
            fechaLabel.getStyleClass().add("cita-text");
            horaLabel.getStyleClass().add("cita-text");
            medicoLabel.getStyleClass().add("cita-text");
            actionButton.getStyleClass().add("cita-button");

            // textContainer con padding y menor spacing
            textContainer = new VBox(4);
            textContainer.setStyle("-fx-padding: 5; -fx-background-color: #ffffff; -fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5;");
            textContainer.getChildren().addAll(idLabel, fechaLabel, horaLabel, medicoLabel);

            // Quitamos el spacer grande, hacemos un spacing menor
            content = new HBox(8);
            content.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 5;");

            // Incluir el container y el boton
            content.getChildren().addAll(textContainer, actionButton);

            // Evento del boton
            actionButton.setOnAction(event -> {
                String citaStr = getItem();
                if (citaStr != null) {
                    mostrarDetalleCita(citaStr);
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Extraer datos usando la regex
                Matcher matcher = pattern.matcher(item);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    String fecha = matcher.group(2);
                    String hora = matcher.group(3);
                    String medico = matcher.group(4);

                    idLabel.setText("ID: " + id);
                    fechaLabel.setText("Fecha: " + fecha);
                    horaLabel.setText("Hora: " + hora);
                    medicoLabel.setText("Medico: " + medico);
                } else {
                    // Si no coincide, ponemos todo en idLabel
                    idLabel.setText(item);
                    fechaLabel.setText("");
                    horaLabel.setText("");
                    medicoLabel.setText("");
                }

                setGraphic(content);
            }
        }
    }


    /**
     * Muestra la vista de detalle de una cita.
     * @param cita Cadena con la informacion de la cita.
     */
    private void mostrarDetalleCita(String cita) {
        try {
            System.out.println("Mostrando detalles para: " + cita);

            // Buscamos ID con la misma logica de parseo
            Pattern p = Pattern.compile("ID: (\\d+)");
            Matcher m = p.matcher(cita);
            int idCita = -1;
            if (m.find()) {
                idCita = Integer.parseInt(m.group(1));
            } else {
                System.err.println("No se encontro ID.");
                return;
            }

            int idMedico = obtenerIdMedicoDeCita(idCita);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/detalle-cita-view.fxml"));
            Parent detalleView = loader.load();

            DetalleCitaController detalleCitaController = loader.getController();
            detalleCitaController.setDatos(dni, idMedico, idCita);

            // Reemplazamos el contenido dinamico (StackPane)
            dynamicContent.getChildren().setAll(detalleView);
        } catch (IOException e) {
            System.err.println("Error al cargar el detalle de la cita:");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error: ID de cita no valido.");
        }
    }

    /**
     * Obtiene el idMedico correspondiente a la cita, consultando la base de datos.
     * @param idCita ID numerico de la cita
     * @return ID del medico, o -1 si no se encontro.
     */
    private int obtenerIdMedicoDeCita(int idCita) {
        String query = "SELECT id_medico FROM citas WHERE id_cita = ?";
        try (Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://pm0002.conectabalear.net:5432/Biancamar", "test", "contraseña_segura_patata_12112");
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, idCita);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_medico");
            } else {
                System.err.println("No se encontro un medico para la cita con ID: " + idCita);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Restaura la vista inicial en el dynamicContent, por ejemplo tras salir de un detalle.
     */
    public void restoreInitialView() {
        if (dynamicContent != null) {
            dynamicContent.getChildren().clear();
            showCentralContent();
            System.out.println("Vista inicial restaurada correctamente.");
        } else {
            System.err.println("El contenedor central (dynamicContent) es nulo.");
        }
    }

    // Navegacion a otras pantallas

    @FXML
    private void handleGestionCita() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/biancamar/gestion-citas-view.fxml"));
            Parent newView = loader.load();

            GestionCitasController controller = loader.getController();
            if (controller != null) {
                controller.setDni(this.dni);
            }

            dynamicContent.getChildren().setAll(newView);
        } catch (IOException e) {
            System.err.println("Error cargando pantalla de Gestion de Citas");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGestionUsuario() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/biancamar/gestiom-usuario-view.fxml"));
            Parent gestionUsuarioView = loader.load();

            GestionUsuariosController gestionUsuariosController = loader.getController();
            if (gestionUsuariosController != null) {
                gestionUsuariosController.setDni(this.dni);
            }

            // Nueva ventana para Gestion de Usuario
            Stage nuevoStage = new Stage();
            Scene scene = new Scene(gestionUsuarioView);
            nuevoStage.setScene(scene);
            nuevoStage.setTitle("Gestion de Usuario");
            nuevoStage.setMaximized(true);
            nuevoStage.show();

            // Cerrar la ventana actual
            Stage stageActual = (Stage) dynamicContent.getScene().getWindow();
            stageActual.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleConsultaResultados() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/consulta-resultados-view.fxml"));
            Parent consultaResultadosView = loader.load();

            ConsultaResultadosController controller = loader.getController();
            if (controller != null) {
                controller.setDni(this.dni);
            }

            Scene consultaResultadosScene = new Scene(consultaResultadosView);
            Stage currentStage = (Stage) dynamicContent.getScene().getWindow();

            // Vincular el tamaño del nodo raíz si es una instancia de Region
            if (consultaResultadosView instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) consultaResultadosView;
                region.prefWidthProperty().bind(currentStage.widthProperty());
                region.prefHeightProperty().bind(currentStage.heightProperty());
            }

            currentStage.setScene(consultaResultadosScene);
            currentStage.setTitle("Consulta de Resultados");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConsultaHistorial() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/biancamar/consulta-historial-view.fxml"));
            Parent consultaHistorialView = loader.load();

            ConsultaHistorialController controller = loader.getController();
            if (controller != null) {
                controller.setDni(this.dni);
            }

            Scene consultaHistorialScene = new Scene(consultaHistorialView);
            Stage currentStage = (Stage) dynamicContent.getScene().getWindow();

            // Vincular el tamaño del nodo raíz si es una instancia de Region
            if (consultaHistorialView instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) consultaHistorialView;
                region.prefWidthProperty().bind(currentStage.widthProperty());
                region.prefHeightProperty().bind(currentStage.heightProperty());
            }

            currentStage.setScene(consultaHistorialScene);
            currentStage.setTitle("Historial Medico");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleCreacionInformes() {
        System.out.println("Creacion de informes no implementada.");
    }
}
