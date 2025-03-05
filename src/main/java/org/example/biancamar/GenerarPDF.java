package org.example.biancamar;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public class GenerarPDF {

    public static void crearPDF(String pacienteDni, String contenidoInforme) {
        // Obtener la ruta de la raíz del proyecto

        String outputPath = pacienteDni;

        try (PDDocument document = new PDDocument()) {
            // Crear una nueva página
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Crear contenido en la página
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Título
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Informe Médico");
                contentStream.endText(); // Termina el bloque de texto

                // Subtítulo
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Paciente con DNI: " + pacienteDni);
                contentStream.endText(); // Termina el bloque de texto

                // Contenido del informe (manejo de líneas múltiples)
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 690); // Empieza desde la posición inicial
                contentStream.setLeading(14.5f); // Configuramos el interlineado

                // Usar un loop para manejar contenido con saltos de línea
                String[] lineas = contenidoInforme.split("\n");
                for (String linea : lineas) {
                    contentStream.showText(linea); // Escribe la línea actual
                    contentStream.newLine(); // Salta a la siguiente línea
                }
                contentStream.endText(); // Termina el bloque de texto
            }

            // Guardar el documento
            System.out.println("Guardando el PDF en: " + outputPath);
            document.save(outputPath);
            System.out.println("Archivo guardado exitosamente en: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error al generar el documento PDF: " + e.getMessage());
        }
    }

    // Método main para pruebas
    public static void main(String[] args) {
        // Invoca el método con un ejemplo de contenido
        String pacienteDni = "12345678A";
        String contenidoInforme = "Este es un informe de prueba para demostrar la generación del PDF.";
        crearPDF(pacienteDni, contenidoInforme);
    }
}