package analizadordataset.metricas;

import java.util.*;

public class EvaluadorClusters {
    
    private double[][] datos;
    private int[] asignaciones;
    private double[][] centroides;
    private int numClusters;
    private int numInstancias;
    private int numAtributos;
    private Map<Integer, Integer> clusterToIndex; // Mapea ID de cluster a índice 0..n-1
    
    public EvaluadorClusters(double[][] datos, int[] asignaciones, double[][] centroides) {
        this.datos = datos;
        this.asignaciones = asignaciones;
        this.centroides = centroides;
        this.numInstancias = datos.length;
        this.numAtributos = datos[0].length;
        
        // Obtener clusters únicos (excluyendo ruido -1)
        Set<Integer> clustersSet = new TreeSet<>();
        for (int a : asignaciones) {
            if (a >= 0) {
                clustersSet.add(a);
            }
        }
        this.numClusters = clustersSet.size();
        
        // Crear mapeo de ID de cluster a índice consecutivo
        this.clusterToIndex = new HashMap<>();
        int idx = 0;
        for (int cluster : clustersSet) {
            clusterToIndex.put(cluster, idx++);
        }
    }
    
    public double calcularSilhouette() {
        if (numClusters < 2) return -1;
        
        double siluetaTotal = 0;
        int puntosValidos = 0;
        
        for (int i = 0; i < numInstancias; i++) {
            int clusterI = asignaciones[i];
            if (clusterI < 0) continue; // Ignorar ruido
            
            // Verificar si el cluster tiene más de 1 elemento
            int tamanioCluster = 0;
            for (int j = 0; j < numInstancias; j++) {
                if (asignaciones[j] == clusterI) tamanioCluster++;
            }
            
            if (tamanioCluster <= 1) {
                continue; // Saltar clusters con 1 solo elemento
            }
            
            // Calcular a: distancia media intra-cluster
            double a = 0;
            int countIntra = 0;
            for (int j = 0; j < numInstancias; j++) {
                if (i != j && asignaciones[j] == clusterI) {
                    a += distanciaEuclidiana(datos[i], datos[j]);
                    countIntra++;
                }
            }
            a = countIntra > 0 ? a / countIntra : 0;
            
            // Calcular b: distancia media al cluster más cercano
            double b = Double.MAX_VALUE;
            for (int c = 0; c < numClusters; c++) {
                int clusterId = obtenerClusterIdPorIndice(c);
                if (clusterId == clusterI) continue;
                
                double distMedia = 0;
                int countInter = 0;
                for (int j = 0; j < numInstancias; j++) {
                    if (asignaciones[j] == clusterId) {
                        distMedia += distanciaEuclidiana(datos[i], datos[j]);
                        countInter++;
                    }
                }
                distMedia = countInter > 0 ? distMedia / countInter : 0;
                b = Math.min(b, distMedia);
            }
            
            double s = (b - a) / Math.max(a, b);
            siluetaTotal += s;
            puntosValidos++;
        }
        
        return puntosValidos > 0 ? siluetaTotal / puntosValidos : 0;
    }
    
    public double calcularDaviesBouldin() {
        if (numClusters < 2) return Double.MAX_VALUE;
        
        // Calcular centroides si no se proporcionaron
        if (centroides == null || centroides.length != numClusters) {
            centroides = calcularCentroides();
        }
        
        // Calcular distancias intra-cluster
        double[] intraDistancias = new double[numClusters];
        for (int c = 0; c < numClusters; c++) {
            int clusterId = obtenerClusterIdPorIndice(c);
            double sumaDistancias = 0;
            int count = 0;
            for (int i = 0; i < numInstancias; i++) {
                if (asignaciones[i] == clusterId) {
                    sumaDistancias += distanciaEuclidiana(datos[i], centroides[c]);
                    count++;
                }
            }
            intraDistancias[c] = count > 0 ? sumaDistancias / count : 0;
        }
        
        // Calcular Davies-Bouldin
        double dbTotal = 0;
        for (int i = 0; i < numClusters; i++) {
            double maxRatio = 0;
            for (int j = 0; j < numClusters; j++) {
                if (i != j) {
                    double distCentroides = distanciaEuclidiana(centroides[i], centroides[j]);
                    if (distCentroides > 0) {
                        double ratio = (intraDistancias[i] + intraDistancias[j]) / distCentroides;
                        maxRatio = Math.max(maxRatio, ratio);
                    }
                }
            }
            dbTotal += maxRatio;
        }
        
        return dbTotal / numClusters;
    }
    
    public double calcularCohesion() {
        double sumaCohesion = 0;
        int totalPares = 0;
        
        for (int c = 0; c < numClusters; c++) {
            int clusterId = obtenerClusterIdPorIndice(c);
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < numInstancias; i++) {
                if (asignaciones[i] == clusterId) {
                    indices.add(i);
                }
            }
            
            for (int i = 0; i < indices.size(); i++) {
                for (int j = i + 1; j < indices.size(); j++) {
                    sumaCohesion += distanciaEuclidiana(datos[indices.get(i)], datos[indices.get(j)]);
                    totalPares++;
                }
            }
        }
        
        return totalPares > 0 ? sumaCohesion / totalPares : 0;
    }
    
    public double calcularSeparacion() {
        if (centroides == null || centroides.length != numClusters) {
            centroides = calcularCentroides();
        }
        
        double sumaSeparacion = 0;
        int totalPares = 0;
        
        for (int i = 0; i < numClusters; i++) {
            for (int j = i + 1; j < numClusters; j++) {
                sumaSeparacion += distanciaEuclidiana(centroides[i], centroides[j]);
                totalPares++;
            }
        }
        
        return totalPares > 0 ? sumaSeparacion / totalPares : 0;
    }
    
    private double distanciaEuclidiana(double[] a, double[] b) {
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            suma += diff * diff;
        }
        return Math.sqrt(suma);
    }
    
    private double[][] calcularCentroides() {
        double[][] centroidesCalc = new double[numClusters][numAtributos];
        int[] conteo = new int[numClusters];
        
        for (int i = 0; i < numInstancias; i++) {
            int cluster = asignaciones[i];
            if (cluster >= 0 && clusterToIndex.containsKey(cluster)) {
                int idx = clusterToIndex.get(cluster);
                conteo[idx]++;
                for (int j = 0; j < numAtributos; j++) {
                    centroidesCalc[idx][j] += datos[i][j];
                }
            }
        }
        
        for (int c = 0; c < numClusters; c++) {
            if (conteo[c] > 0) {
                for (int j = 0; j < numAtributos; j++) {
                    centroidesCalc[c][j] /= conteo[c];
                }
            }
        }
        
        return centroidesCalc;
    }
    
    private int obtenerClusterIdPorIndice(int indice) {
        for (Map.Entry<Integer, Integer> entry : clusterToIndex.entrySet()) {
            if (entry.getValue() == indice) {
                return entry.getKey();
            }
        }
        return -1;
    }
}