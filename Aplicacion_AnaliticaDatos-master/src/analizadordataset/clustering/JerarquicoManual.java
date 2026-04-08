package analizadordataset.clustering;

import analizadordataset.modelo.*;
import analizadordataset.metricas.EvaluadorClusters;
import java.util.*;

public class JerarquicoManual implements Clustering {
    
    private int numClusters = 3;
    private String enlace = "average"; // single, complete, average
    
    public JerarquicoManual() {}
    
    public JerarquicoManual(int numClusters, String enlace) {
        this.numClusters = numClusters;
        this.enlace = enlace;
    }
    
    private class Cluster {
        List<Integer> indices;
        
        Cluster(int indice) {
            this.indices = new ArrayList<>();
            this.indices.add(indice);
        }
        
        Cluster(Cluster a, Cluster b) {
            this.indices = new ArrayList<>();
            this.indices.addAll(a.indices);
            this.indices.addAll(b.indices);
        }
        
        int size() {
            return indices.size();
        }
    }
    
    @Override
    public ResultadoClustering ejecutar(Dataset dataset) {
        int numInstancias = dataset.getNumInstancias();
        int numAtributos = dataset.getNumAtributos();
        
        // Usar solo atributos numéricos
        List<Integer> atributosNumericos = new ArrayList<>();
        for (int i = 0; i < numAtributos; i++) {
            if (dataset.getTipoColumna(i) == 'N') {
                atributosNumericos.add(i);
            }
        }
        
        if (atributosNumericos.isEmpty()) {
            throw new IllegalArgumentException("No hay atributos numéricos para clustering");
        }
        
        int numAtributosNum = atributosNumericos.size();
        double[][] datos = new double[numInstancias][numAtributosNum];
        
        // Cargar y normalizar datos
        double[] min = new double[numAtributosNum];
        double[] max = new double[numAtributosNum];
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);
        
        for (int i = 0; i < numInstancias; i++) {
            for (int j = 0; j < numAtributosNum; j++) {
                int attr = atributosNumericos.get(j);
                double val = dataset.getValorNumerico(i, attr);
                datos[i][j] = val;
                min[j] = Math.min(min[j], val);
                max[j] = Math.max(max[j], val);
            }
        }
        
        // Normalizar a [0,1]
        for (int i = 0; i < numInstancias; i++) {
            for (int j = 0; j < numAtributosNum; j++) {
                if (max[j] - min[j] > 0) {
                    datos[i][j] = (datos[i][j] - min[j]) / (max[j] - min[j]);
                }
            }
        }
        
        // Matriz de distancias
        double[][] distancias = new double[numInstancias][numInstancias];
        for (int i = 0; i < numInstancias; i++) {
            for (int j = i + 1; j < numInstancias; j++) {
                double dist = 0;
                for (int d = 0; d < numAtributosNum; d++) {
                    double diff = datos[i][d] - datos[j][d];
                    dist += diff * diff;
                }
                dist = Math.sqrt(dist);
                distancias[i][j] = dist;
                distancias[j][i] = dist;
            }
        }
        
        // Inicializar clusters (cada punto es un cluster)
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < numInstancias; i++) {
            clusters.add(new Cluster(i));
        }
        
        // Agrupar hasta tener numClusters
        List<String> historial = new ArrayList<>();
        
        while (clusters.size() > numClusters) {
            int mejorA = -1, mejorB = -1;
            double minDist = Double.MAX_VALUE;
            
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double dist = calcularDistanciaEntreClusters(clusters.get(i), clusters.get(j), distancias);
                    if (dist < minDist) {
                        minDist = dist;
                        mejorA = i;
                        mejorB = j;
                    }
                }
            }
            
            if (mejorA == -1) break;
            
            Cluster nuevo = new Cluster(clusters.get(mejorA), clusters.get(mejorB));
            historial.add("Unidos clusters de " + clusters.get(mejorA).size() + 
                         " y " + clusters.get(mejorB).size() + " elementos (dist: " + String.format("%.4f", minDist) + ")");
            
            clusters.remove(Math.max(mejorA, mejorB));
            clusters.remove(Math.min(mejorA, mejorB));
            clusters.add(nuevo);
        }
        
        // Asignar etiquetas
        int[] asignaciones = new int[numInstancias];
        for (int c = 0; c < clusters.size(); c++) {
            for (int idx : clusters.get(c).indices) {
                asignaciones[idx] = c + 1;
            }
        }
        
        // Calcular centroides
        double[][] centroides = new double[clusters.size()][numAtributosNum];
        int[] conteo = new int[clusters.size()];
        
        for (int i = 0; i < numInstancias; i++) {
            int cluster = asignaciones[i] - 1;
            if (cluster >= 0) {
                conteo[cluster]++;
                for (int d = 0; d < numAtributosNum; d++) {
                    centroides[cluster][d] += datos[i][d];
                }
            }
        }
        
        for (int c = 0; c < clusters.size(); c++) {
            if (conteo[c] > 0) {
                for (int d = 0; d < numAtributosNum; d++) {
                    centroides[c][d] /= conteo[c];
                }
            }
        }
        
        // Crear resultado
        ResultadoClustering resultado = new ResultadoClustering("Jerárquico Manual (" + enlace + ")");
        resultado.setAsignaciones(asignaciones);
        
        // Calcular métricas de evaluación
        if (clusters.size() >= 2) {
            EvaluadorClusters evaluador = new EvaluadorClusters(datos, asignaciones, centroides);
            double silhouette = evaluador.calcularSilhouette();
            double daviesBouldin = evaluador.calcularDaviesBouldin();
            double cohesion = evaluador.calcularCohesion();
            double separacion = evaluador.calcularSeparacion();
            resultado.setEvaluacion(silhouette, daviesBouldin, cohesion, separacion);
        } else {
            resultado.agregarMetrica("Silhouette", "No aplicable (menos de 2 clusters)");
        }
        
        resultado.agregarMetrica("Clusters", String.valueOf(clusters.size()));
        resultado.agregarMetrica("Tipo enlace", enlace);
        
        return resultado;
    }
    
    private double calcularDistanciaEntreClusters(Cluster a, Cluster b, double[][] distancias) {
        double minDist = Double.MAX_VALUE;
        double maxDist = Double.MIN_VALUE;
        double sumaDist = 0;
        int count = 0;
        
        for (int i : a.indices) {
            for (int j : b.indices) {
                double dist = distancias[i][j];
                minDist = Math.min(minDist, dist);
                maxDist = Math.max(maxDist, dist);
                sumaDist += dist;
                count++;
            }
        }
        
        switch (enlace) {
            case "single":
                return minDist;
            case "complete":
                return maxDist;
            case "average":
                return count > 0 ? sumaDist / count : Double.MAX_VALUE;
            default:
                return minDist;
        }
    }
}