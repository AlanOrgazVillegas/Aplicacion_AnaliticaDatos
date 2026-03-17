package analizadordataset.modelo;

import java.util.*;

public class Resultado {
    private String nombreTecnica;
    private List<AtributoRanking> ranking;
    private Map<String, String> metricas;
    private Date fechaEjecucion;
    
    public Resultado(String nombreTecnica, List<AtributoRanking> ranking) {
        this.nombreTecnica = nombreTecnica;
        this.ranking = ranking;
        this.metricas = new LinkedHashMap<>();
        this.fechaEjecucion = new Date();
    }
    
    public String getNombreTecnica() {
        return nombreTecnica;
    }
    
    public void setNombreTecnica(String nombreTecnica) {
        this.nombreTecnica = nombreTecnica;
    }
    
    public List<AtributoRanking> getRanking() {
        return ranking;
    }
    
    public void setRanking(List<AtributoRanking> ranking) {
        this.ranking = ranking;
    }
    
    public Map<String, String> getMetricas() {
        return metricas;
    }
    
    public void setMetricas(Map<String, String> metricas) {
        this.metricas = metricas;
    }
    
    public void agregarMetrica(String nombre, String valor) {
        this.metricas.put(nombre, valor);
    }
    
    public Date getFechaEjecucion() {
        return fechaEjecucion;
    }
    
    public String generarReporte() {
        StringBuilder reporte = new StringBuilder();
        
        // Encabezado
        reporte.append("========================================\n");
        reporte.append("TÉCNICA: ").append(nombreTecnica).append("\n");
        reporte.append("Fecha: ").append(fechaEjecucion).append("\n");
        reporte.append("========================================\n\n");
        
        // Métricas
        if (!metricas.isEmpty()) {
            reporte.append("--- MÉTRICAS DE EJECUCIÓN ---\n");
            for (Map.Entry<String, String> entry : metricas.entrySet()) {
                reporte.append(String.format("%-25s: %s\n", entry.getKey(), entry.getValue()));
            }
            reporte.append("\n");
        }
        
        // Ranking de atributos
        reporte.append("--- RANKING DE ATRIBUTOS (ordenados por peso) ---\n");
        reporte.append(String.format("%-4s %-30s %-15s\n", "Pos", "Atributo", "Peso"));
        reporte.append("------------------------------------------------\n");
        
        int posicion = 1;
        for (AtributoRanking attr : ranking) {
            reporte.append(String.format("%-4d %-30s %-15.6f\n", 
                posicion++, 
                truncarString(attr.getNombreAtributo(), 30), 
                attr.getPeso()));
        }
        
        return reporte.toString();
    }
    
    private String truncarString(String str, int longitud) {
        if (str.length() <= longitud) {
            return str;
        }
        return str.substring(0, longitud - 3) + "...";
    }
}