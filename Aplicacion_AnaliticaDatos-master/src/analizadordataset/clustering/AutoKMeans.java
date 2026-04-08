package analizadordataset.clustering;

import analizadordataset.modelo.*;
import analizadordataset.metricas.EvaluadorClusters;
import java.util.*;

public class AutoKMeans implements Clustering {
    
    private int minK = 2;
    private int maxK = 10;
    
    public AutoKMeans() {}
    
    public AutoKMeans(int minK, int maxK) {
        this.minK = minK;
        this.maxK = maxK;
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
        
        // Evaluar diferentes valores de k
        double mejorSilhouette = -1;
        int mejorK = minK;
        int[] mejoresAsignaciones = null;
        double[][] mejoresCentroides = null;
        
        for (int k = minK; k <= maxK && k <= numInstancias; k++) {
            // Ejecutar K-Means para este k
            double[][] centroides = new double[k][numAtributosNum];
            Random rand = new Random(42);
            
            // Inicializar centroides con k-means++
            int primerIndice = rand.nextInt(numInstancias);
            centroides[0] = datos[primerIndice].clone();
            
            for (int c = 1; c < k; c++) {
                double[] distanciasMinimas = new double[numInstancias];
                double sumaDistancias = 0;
                
                for (int i = 0; i < numInstancias; i++) {
                    double minDist = Double.MAX_VALUE;
                    for (int j = 0; j < c; j++) {
                        double dist = 0;
                        for (int d = 0; d < numAtributosNum; d++) {
                            double diff = datos[i][d] - centroides[j][d];
                            dist += diff * diff;
                        }
                        dist = Math.sqrt(dist);
                        minDist = Math.min(minDist, dist);
                    }
                    distanciasMinimas[i] = minDist;
                    sumaDistancias += minDist;
                }
                
                double r = rand.nextDouble() * sumaDistancias;
                double acumulado = 0;
                for (int i = 0; i < numInstancias; i++) {
                    acumulado += distanciasMinimas[i];
                    if (acumulado >= r) {
                        centroides[c] = datos[i].clone();
                        break;
                    }
                }
            }
            
            int[] asignaciones = new int[numInstancias];
            boolean cambio = true;
            int iteracion = 0;
            int maxIter = 100;
            
            while (cambio && iteracion < maxIter) {
                cambio = false;
                iteracion++;
                
                // Asignar puntos al centroide más cercano
                for (int i = 0; i < numInstancias; i++) {
                    int mejorCluster = 0;
                    double menorDistancia = Double.MAX_VALUE;
                    for (int c = 0; c < k; c++) {
                        double dist = 0;
                        for (int d = 0; d < numAtributosNum; d++) {
                            double diff = datos[i][d] - centroides[c][d];
                            dist += diff * diff;
                        }
                        dist = Math.sqrt(dist);
                        if (dist < menorDistancia) {
                            menorDistancia = dist;
                            mejorCluster = c;
                        }
                    }
                    if (asignaciones[i] != mejorCluster) {
                        asignaciones[i] = mejorCluster;
                        cambio = true;
                    }
                }
                
                // Recalcular centroides
                double[][] nuevosCentroides = new double[k][numAtributosNum];
                int[] conteo = new int[k];
                for (int i = 0; i < numInstancias; i++) {
                    int cluster = asignaciones[i];
                    conteo[cluster]++;
                    for (int d = 0; d < numAtributosNum; d++) {
                        nuevosCentroides[cluster][d] += datos[i][d];
                    }
                }
                for (int c = 0; c < k; c++) {
                    if (conteo[c] > 0) {
                        for (int d = 0; d < numAtributosNum; d++) {
                            nuevosCentroides[c][d] /= conteo[c];
                        }
                    } else {
                        nuevosCentroides[c] = centroides[c];
                    }
                }
                centroides = nuevosCentroides;
            }
            
            // Calcular silueta para este k
            EvaluadorClusters evaluador = new EvaluadorClusters(datos, asignaciones, centroides);
            double silhouette = evaluador.calcularSilhouette();
            
            if (silhouette > mejorSilhouette) {
                mejorSilhouette = silhouette;
                mejorK = k;
                mejoresAsignaciones = asignaciones.clone();
                mejoresCentroides = centroides;
            }
        }
        
        // Crear resultado
        ResultadoClustering resultado = new ResultadoClustering("Auto K-Means (k óptimo=" + mejorK + ")");
        resultado.setAsignaciones(mejoresAsignaciones);
        
        // Agregar métricas
        EvaluadorClusters evaluadorFinal = new EvaluadorClusters(datos, mejoresAsignaciones, mejoresCentroides);
        double silhouette = evaluadorFinal.calcularSilhouette();
        double daviesBouldin = evaluadorFinal.calcularDaviesBouldin();
        double cohesion = evaluadorFinal.calcularCohesion();
        double separacion = evaluadorFinal.calcularSeparacion();
        
        resultado.setEvaluacion(silhouette, daviesBouldin, cohesion, separacion);
        resultado.agregarMetrica("Clusters", String.valueOf(mejorK));
        resultado.agregarMetrica("Rango evaluado", minK + "-" + maxK);
        
        return resultado;
    }
}