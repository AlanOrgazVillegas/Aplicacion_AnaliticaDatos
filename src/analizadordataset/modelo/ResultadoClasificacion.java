package analizadordataset.modelo;

import java.util.*;

public class ResultadoClasificacion {
    private String nombre;
    private double precision;
    private double[][] matrizConfusion;
    private String[] clases;
    private Map<String, String> metricas; // Cambiado a Map<String, String>
    private long tiempoEjecucion;
    
    public ResultadoClasificacion(String nombre) {
        this.nombre = nombre;
        this.metricas = new LinkedHashMap<>();
    }
    
    public void setPrecision(double precision) {
        this.precision = precision;
        metricas.put("Precisión", String.format("%.2f%%", precision * 100));
    }
    
    public void setMatrizConfusion(double[][] matrizConfusion, String[] clases) {
        this.matrizConfusion = matrizConfusion;
        this.clases = clases;
    }
    
    public void setTiempoEjecucion(long tiempoEjecucion) {
        this.tiempoEjecucion = tiempoEjecucion;
        metricas.put("Tiempo (ms)", String.valueOf(tiempoEjecucion));
    }
    
    public void agregarMetrica(String nombre, String valor) {
        metricas.put(nombre, valor);
    }
    
    public String generarReporte() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(nombre).append(" ===\n\n");
        
        sb.append("Métricas:\n");
        for (Map.Entry<String, String> entry : metricas.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        
        if (matrizConfusion != null && clases != null) {
            sb.append("Matriz de Confusión:\n");
            sb.append("==================\n\n");
            
            // Encabezado
            sb.append("     ");
            for (String clase : clases) {
                sb.append(String.format("%-10s", clase));
            }
            sb.append("\n");
            
            // Filas
            for (int i = 0; i < matrizConfusion.length; i++) {
                sb.append(String.format("%-5s", clases[i]));
                for (int j = 0; j < matrizConfusion[i].length; j++) {
                    sb.append(String.format("%-10.0f", matrizConfusion[i][j]));
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    // Getters
    public String getNombre() { return nombre; }
    public double getPrecision() { return precision; }
    public Map<String, String> getMetricas() { return metricas; }
}