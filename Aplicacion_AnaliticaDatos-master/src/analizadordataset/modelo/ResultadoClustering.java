package analizadordataset.modelo;

import java.util.*;

public class ResultadoClustering {
    private String nombre;
    private int[] asignaciones;
    private Map<String, String> metricas;
    
    public ResultadoClustering(String nombre) {
        this.nombre = nombre;
        this.metricas = new LinkedHashMap<>();
    }
    
    public ResultadoClustering(String nombre, int[] asignaciones, Map<String, String> metricas) {
        this.nombre = nombre;
        this.asignaciones = asignaciones;
        this.metricas = metricas != null ? metricas : new LinkedHashMap<>();
    }
    
    public void setAsignaciones(int[] asignaciones) {
        this.asignaciones = asignaciones;
    }
    
    public int[] getAsignaciones() {
        return asignaciones;
    }
    
    public void agregarMetrica(String clave, String valor) {
        metricas.put(clave, valor);
    }
    
    public Map<String, String> getMetricas() {
        return metricas;
    }
    
    public void setEvaluacion(double silhouette, double daviesBouldin, double cohesion, double separacion) {
        metricas.put("Silhouette", String.format("%.4f", silhouette));
        metricas.put("Davies-Bouldin", String.format("%.4f", daviesBouldin));
        metricas.put("Cohesión", String.format("%.4f", cohesion));
        metricas.put("Separación", String.format("%.4f", separacion));
        
        // Interpretación
        if (silhouette > 0.7) {
            metricas.put("Interpretación", "Excelente agrupamiento");
        } else if (silhouette > 0.5) {
            metricas.put("Interpretación", "Buen agrupamiento");
        } else if (silhouette > 0.3) {
            metricas.put("Interpretación", "Agrupamiento aceptable");
        } else {
            metricas.put("Interpretación", "Agrupamiento débil");
        }
    }
    
    public String generarReporte() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(nombre).append(" ===\n\n");
        
        sb.append("MÉTRICAS:\n");
        for (Map.Entry<String, String> entry : metricas.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        
        if (asignaciones != null) {
            sb.append("DISTRIBUCIÓN DE CLUSTERS:\n");
            Map<Integer, Integer> conteo = new TreeMap<>();
            for (int c : asignaciones) {
                conteo.put(c, conteo.getOrDefault(c, 0) + 1);
            }
            for (Map.Entry<Integer, Integer> entry : conteo.entrySet()) {
                sb.append("  Cluster ").append(entry.getKey())
                  .append(": ").append(entry.getValue()).append(" instancias\n");
            }
        }
        
        return sb.toString();
    }
}