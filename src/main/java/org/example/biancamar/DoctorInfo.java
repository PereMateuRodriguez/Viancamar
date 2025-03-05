package org.example.biancamar;

class DoctorInfo {
    private int id;
    private String nombre;
    private String apellido;
    private String especialidad;
    private String hospital;
    private String horarioDisponible;

    public DoctorInfo(int id, String nombre, String apellido, String especialidad, String hospital, String horarioDisponible) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.especialidad = especialidad;
        this.hospital = hospital;
        this.horarioDisponible = horarioDisponible;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getHospital() {
        return hospital;
    }

    public String getHorarioDisponible() {
        return horarioDisponible;
    }
}