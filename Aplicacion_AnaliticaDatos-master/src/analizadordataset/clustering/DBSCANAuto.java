package analizadordataset.clustering;

import analizadordataset.modelo.*;
import analizadordataset.metricas.EvaluadorClusters;
import java.util.*;

public class DBSCANAuto implements Clustering {
    
    private double eps = 0.5;
    private int minPts = 3;
    
    public DBSCANAuto() {}
    
    public DBSCANAuto(double eps, int minPts) {
        this.eps = eps;
        this.minPts = minPts;
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
        
        // DBSCAN
        int[] asignaciones = new int[numInstancias];
        Arrays.fill(asignaciones, -1); // -1 = no visitado
        int clusterId = 0;
        
        for (int i = 0; i < numInstancias; i++) {
            if (asignaciones[i] != -1) continue;
            
            List<Integer> vecinos = obtenerVecinos(datos, i, numAtributosNum);
            
            if (vecinos.size() < minPts) {
                asignaciones[i] = 0; // Marcado como ruido temporalmente
            } else {
                clusterId++;
                asignaciones[i] = clusterId;
                expandirCluster(datos, asignaciones, i, vecinos, clusterId, numAtributosNum);
            }
        }
        
        // Reasignar ruido (0) a -1
        for (int i = 0; i < numInstancias; i++) {
            if (asignaciones[i] == 0) {
                asignaciones[i] = -1;
            }
        }
        
        // Contar clusters reales (excluyendo ruido)
        Set<Integer> clustersSet = new HashSet<>();
        for (int a : asignaciones) {
            if (a > 0) {
                clustersSet.add(a);
            }
        }
        int clustersReales = clustersSet.size();
        int ruido = 0;
        for (int a : asignaciones) {
            if (a == -1) ruido++;
        }
        
        // Crear resultado
        ResultadoClustering resultado = new ResultadoClustering("DBSCAN Auto (eps=" + eps + ", minPts=" + minPts + ")");
        resultado.setAsignaciones(asignaciones);
        
        // Calcular métricas de evaluación SOLO si hay al menos 2 clusters
        if (clustersReales >= 2) {
            try {
                // Calcular centroides solo para clusters válidos
                double[][] centroides = calcularCentroides(datos, asignaciones, clustersReales, numAtributosNum);
                EvaluadorClusters evaluador = new EvaluadorClusters(datos, asignaciones, centroides);
                
                double silhouette = evaluador.calcularSilhouette();
                double daviesBouldin = evaluador.calcularDaviesBouldin();
                double cohesion = evaluador.calcularCohesion();
                double separacion = evaluador.calcularSeparacion();
                
                resultado.setEvaluacion(silhouette, daviesBouldin, cohesion, separacion);
            } catch (Exception e) {
                resultado.agregarMetrica("Silhouette", "Error en cálculo");
                resultado.agregarMetrica("Interpretación", "Error: " + e.getMessage());
            }
        } else if (clustersReales == 1) {
            resultado.agregarMetrica("Silhouette", "No aplicable (solo 1 cluster)");
            resultado.agregarMetrica("Davies-Bouldin", "No aplicable");
            resultado.agregarMetrica("Interpretación", "DBSCAN solo encontró 1 cluster. Ajusta eps o minPts");
        } else {
            resultado.agregarMetrica("Silhouette", "No aplicable (solo ruido)");
            resultado.agregarMetrica("Interpretación", "DBSCAN no encontró clusters. Ajusta eps o minPts");
        }
        
        resultado.agregarMetrica("Clusters", String.valueOf(clustersReales));
        resultado.agregarMetrica("Ruido", String.valueOf(ruido));
        resultado.agregarMetrica("Eps", String.valueOf(eps));
        resultado.agregarMetrica("MinPts", String.valueOf(minPts));
        
        return resultado;
    }
    
    private List<Integer> obtenerVecinos(double[][] datos, int punto, int dims) {
        List<Integer> vecinos = new ArrayList<>();
        for (int i = 0; i < datos.length; i++) {
            double dist = 0;
            for (int d = 0; d < dims; d++) {
                double diff = datos[punto][d] - datos[i][d];
                dist += diff * diff;
            }
            dist = Math.sqrt(dist);
            if (dist <= eps) {
                vecinos.add(i);
            }
        }
        return vecinos;
    }
    
    private void expandirCluster(double[][] datos, int[] asignaciones, int punto, 
                                  List<Integer> vecinos, int clusterId, int dims) {
        Queue<Integer> cola = new LinkedList<>(vecinos);
        while (!cola.isEmpty()) {
            int p = cola.poll();
            if (asignaciones[p] == -1) {
                asignaciones[p] = clusterId;
                List<Integer> nuevosVecinos = obtenerVecinos(datos, p, dims);
                if (nuevosVecinos.size() >= minPts) {
                    cola.addAll(nuevosVecinos);
                }
            }
        }
    }
    
    private double[][] calcularCentroides(double[][] datos, int[] asignaciones, int numClusters, int dims) {
        if (numClusters <= 0) {
            return new double[0][0];
        }
        
        double[][] centroides = new double[numClusters][dims];
        int[] conteo = new int[numClusters];
        
        for (int i = 0; i < datos.length; i++) {
            int cluster = asignaciones[i];
            if (cluster > 0 && cluster <= numClusters) {
                int idx = cluster - 1;
                conteo[idx]++;
                for (int d = 0; d < dims; d++) {
                    centroides[idx][d] += datos[i][d];
                }
            }
        }
        
        for (int c = 0; c < numClusters; c++) {
            if (conteo[c] > 0) {
                for (int d = 0; d < dims; d++) {
                    centroides[c][d] /= conteo[c];
                }
            }
        }
        
        return centroides;
    }
}