package org.example.biancamar;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cita {
    private int idCita;

    private final StringProperty fecha;
    private final StringProperty hora;
    private final StringProperty especialidad;
    private final StringProperty doctor;

    public Cita(String fecha, String hora, String especialidad, String doctor) {
        this.fecha = new SimpleStringProperty(fecha);
        this.hora = new SimpleStringProperty(hora);
        this.especialidad = new SimpleStringProperty(especialidad);
        this.doctor = new SimpleStringProperty(doctor);
    }
    public int getIdCita() {
        return idCita;
    }

    public void setIdCita(int idCita) {
        this.idCita = idCita;
    }


    public StringProperty fechaProperty() {
        return fecha;
    }

    public StringProperty horaProperty() {
        return hora;
    }

    public StringProperty especialidadProperty() {
        return especialidad;
    }

    public StringProperty doctorProperty() {
        return doctor;
    }
}