package analizadordataset.clustering;

import analizadordataset.modelo.*;
import java.util.*;
import analizadordataset.metricas.EvaluadorClusters;

public class KMeansManual implements Clustering {
    
    private int k = 3;
    private int maxIteraciones = 100;
    private double[][] centroides;
    private int[] asignaciones;
    
    public KMeansManual() {}
    
    public KMeansManual(int k) {
        this.k = k;
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
        
        // Inicializar centroides con el método k-means++
        Random rand = new Random(42);
        centroides = new double[k][numAtributosNum];
        
        // Primer centroide aleatorio
        int primerIndice = rand.nextInt(numInstancias);
        centroides[0] = datos[primerIndice].clone();
        
        // Resto de centroides
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
        
        asignaciones = new int[numInstancias];
        boolean cambio = true;
        int iteracion = 0;
        
        while (cambio && iteracion < maxIteraciones) {
            cambio = false;
            iteracion++;
            
            // Asignar puntos al centroide más cercano
            for (int i = 0; i < numInstancias; i++) {
                int mejorCluster = 0;
                double menorDistancia = Double.MAX_VALUE;
                
                for (int c = 0; c < k; c++) {
                    double distancia = 0;
                    for (int d = 0; d < numAtributosNum; d++) {
                        double diff = datos[i][d] - centroides[c][d];
                        distancia += diff * diff;
                    }
                    distancia = Math.sqrt(distancia);
                    
                    if (distancia < menorDistancia) {
                        menorDistancia = distancia;
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
        
        // Calcular varianza intra-cluster
        double varianza = 0;
        for (int i = 0; i < numInstancias; i++) {
            int cluster = asignaciones[i];
            for (int d = 0; d < numAtributosNum; d++) {
                double diff = datos[i][d] - centroides[cluster][d];
                varianza += diff * diff;
            }
        }
        varianza = varianza / numInstancias;
        
        ResultadoClustering resultado = new ResultadoClustering("K-Means Manual (k=" + k + ")");
        resultado.setAsignaciones(asignaciones);
        resultado.agregarMetrica("Clusters", String.valueOf(k));
        resultado.agregarMetrica("Iteraciones", String.valueOf(iteracion));
        resultado.agregarMetrica("Varianza intra-cluster", String.format("%.4f", varianza));
        
        EvaluadorClusters evaluador = new EvaluadorClusters(datos, asignaciones, centroides);
double silhouette = evaluador.calcularSilhouette();
double daviesBouldin = evaluador.calcularDaviesBouldin();
double cohesion = evaluador.calcularCohesion();
double separacion = evaluador.calcularSeparacion();

resultado.setEvaluacion(silhouette, daviesBouldin, cohesion, separacion);
        return resultado;
    }
}