package org.example.biancamar;

public class HistorialMedico {
    private final String fechaCreacion;
    private final String resumen;
    private final String condicionesPreexistentes;
    private final String notas;
    private final String medicamentos;

    public HistorialMedico(String fechaCreacion, String resumen, String condicionesPreexistentes, String notas, String medicamentos) {
        this.fechaCreacion = fechaCreacion;
        this.resumen = resumen;
        this.condicionesPreexistentes = condicionesPreexistentes;
        this.notas = notas;
        this.medicamentos = medicamentos;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public String getResumen() {
        return resumen;
    }

    public String getCondicionesPreexistentes() {
        return condicionesPreexistentes;
    }

    public String getNotas() {
        return notas;
    }

    public String getMedicamentos() {
        return medicamentos;
    }
}