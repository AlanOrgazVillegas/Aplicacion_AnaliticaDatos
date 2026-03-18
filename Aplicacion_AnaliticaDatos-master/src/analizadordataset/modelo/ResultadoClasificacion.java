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
        sb.append("MATRIZ DE CONFUSIÓN\n");
        sb.append("===================\n\n");
        
        // Determinar el ancho máximo de los nombres de clase (máximo 10 caracteres)
        int anchoMaximo = 10;
        
        // Encabezado - mostrar solo índices en lugar de nombres largos
        sb.append("     "); // Espacio para la columna de clases reales
        for (int i = 0; i < clases.length; i++) {
            sb.append(String.format("%4d ", i + 1));
        }
        sb.append("\n");
        
        // Línea separadora
        sb.append("     ");
        for (int i = 0; i < clases.length; i++) {
            sb.append("-----");
        }
        sb.append("\n");
        
        // Filas de la matriz
        for (int i = 0; i < matrizConfusion.length; i++) {
            // Mostrar índice de la clase real en lugar del nombre completo
            sb.append(String.format("%3d |", i + 1));
            
            for (int j = 0; j < matrizConfusion[i].length; j++) {
                sb.append(String.format("%4.0f ", matrizConfusion[i][j]));
            }
            sb.append("\n");
        }
        
        sb.append("\n");
        
        // Leyenda de clases (abajo de la matriz)
        sb.append("Leyenda de clases:\n");
        for (int i = 0; i < clases.length; i++) {
            String nombreCorto = clases[i].length() > 25 ? 
                clases[i].substring(0, 22) + "..." : 
                clases[i];
            sb.append(String.format("  %d = %s\n", i + 1, nombreCorto));
        }
    }
    
    return sb.toString();
}
    
    // Getters
    public String getNombre() { return nombre; }
    public double getPrecision() { return precision; }
    public Map<String, String> getMetricas() { return metricas; }
}