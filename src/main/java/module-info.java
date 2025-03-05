module org.example.biancamar {
    requires javafx.fxml;
    requires java.sql;
    requires com.calendarfx.view;
    requires java.desktop;
    requires org.apache.pdfbox;

    opens org.example.biancamar to javafx.fxml;
    exports org.example.biancamar;
}
