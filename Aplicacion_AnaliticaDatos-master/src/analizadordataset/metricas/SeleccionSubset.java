package analizadordataset.metricas;

import analizadordataset.modelo.*;
import analizadordataset.clustering.*;
import java.util.*;
import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.ResultadoClustering;
import analizadordataset.clustering.Clustering;

public class SeleccionSubset {
    
    private Dataset dataset;
    private Clustering algoritmo;
    
    public SeleccionSubset(Dataset dataset, Clustering algoritmo) {
        this.dataset = dataset;
        this.algoritmo = algoritmo;
    }
    
    /**
     * Evaluar diferentes subconjuntos de atributos y encontrar el mejor
     */
    public SubsetResultado encontrarMejorSubset(int maxAtributos) {
        int numAtributos = dataset.getNumAtributos();
        List<Integer> indicesAtributos = new ArrayList<>();
        for (int i = 0; i < numAtributos; i++) {
            indicesAtributos.add(i);
        }
        
        double mejorSilhouette = -1;
        List<Integer> mejorSubset = null;
        ResultadoClustering mejorResultado = null;
        
        // Probar diferentes combinaciones de atributos
        for (int size = 2; size <= Math.min(maxAtributos, numAtributos); size++) {
            List<List<Integer>> combinaciones = generarCombinaciones(indicesAtributos, size);
            
            for (List<Integer> subset : combinaciones) {
                Dataset subsetDataset = crearSubsetDataset(subset);
                
                try {
                    ResultadoClustering resultado = algoritmo.ejecutar(subsetDataset);
                    int[] asignaciones = resultado.getAsignaciones();
                    
                    // Calcular silhouette para este subset
                    double silhouette = calcularSilhouetteParaSubset(subsetDataset, asignaciones);
                    
                    if (silhouette > mejorSilhouette) {
                        mejorSilhouette = silhouette;
                        mejorSubset = subset;
                        mejorResultado = resultado;
                    }
                } catch (Exception e) {
                    // Ignorar errores
                }
            }
        }
        
        SubsetResultado resultado = new SubsetResultado();
        resultado.mejorSilhouette = mejorSilhouette;
        resultado.mejorSubset = mejorSubset;
        resultado.resultado = mejorResultado;
        
        return resultado;
    }
    
    /**
     * Algoritmo greedy para selección rápida de subset
     */
    public SubsetResultado encontrarSubsetGreedy(int maxAtributos) {
        int numAtributos = dataset.getNumAtributos();
        List<Integer> seleccionados = new ArrayList<>();
        List<Integer> disponibles = new ArrayList<>();
        
        for (int i = 0; i < numAtributos; i++) {
            disponibles.add(i);
        }
        
        double mejorSilhouetteGlobal = -1;
        
        while (seleccionados.size() < maxAtributos && !disponibles.isEmpty()) {
            int mejorAtributo = -1;
            double mejorSilhouette = -1;
            
            for (int attr : disponibles) {
                List<Integer> prueba = new ArrayList<>(seleccionados);
                prueba.add(attr);
                
                Dataset subsetDataset = crearSubsetDataset(prueba);
                
                try {
                    ResultadoClustering resultado = algoritmo.ejecutar(subsetDataset);
                    int[] asignaciones = resultado.getAsignaciones();
                    double silhouette = calcularSilhouetteParaSubset(subsetDataset, asignaciones);
                    
                    if (silhouette > mejorSilhouette) {
                        mejorSilhouette = silhouette;
                        mejorAtributo = attr;
                    }
                } catch (Exception e) {
                    // Ignorar
                }
            }
            
            if (mejorAtributo != -1) {
                seleccionados.add(mejorAtributo);
                disponibles.remove((Integer) mejorAtributo);
                mejorSilhouetteGlobal = mejorSilhouette;
            } else {
                break;
            }
        }
        
        SubsetResultado resultado = new SubsetResultado();
        resultado.mejorSilhouette = mejorSilhouetteGlobal;
        resultado.mejorSubset = seleccionados;
        
        if (!seleccionados.isEmpty()) {
            Dataset subsetDataset = crearSubsetDataset(seleccionados);
            resultado.resultado = algoritmo.ejecutar(subsetDataset);
        }
        
        return resultado;
    }
    
    private Dataset crearSubsetDataset(List<Integer> indices) {
    return dataset.crearSubset(indices);
}
    private double calcularSilhouetteParaSubset(Dataset subset, int[] asignaciones) {
        // Convertir Dataset a matriz de datos
        int numInstancias = subset.getNumInstancias();
        int numAtributos = subset.getNumAtributos();
        
        double[][] datos = new double[numInstancias][numAtributos];
        for (int i = 0; i < numInstancias; i++) {
            for (int j = 0; j < numAtributos; j++) {
                if (subset.getTipoColumna(j) == 'N') {
                    datos[i][j] = subset.getValorNumerico(i, j);
                }
            }
        }
        
        EvaluadorClusters evaluador = new EvaluadorClusters(datos, asignaciones, null);
        return evaluador.calcularSilhouette();
    }
    
    private List<List<Integer>> generarCombinaciones(List<Integer> elementos, int k) {
        List<List<Integer>> resultado = new ArrayList<>();
        generarCombinacionesAux(elementos, 0, k, new ArrayList<>(), resultado);
        return resultado;
    }
    
    private void generarCombinacionesAux(List<Integer> elementos, int inicio, int k,
                                          List<Integer> actual, List<List<Integer>> resultado) {
        if (actual.size() == k) {
            resultado.add(new ArrayList<>(actual));
            return;
        }
        
        for (int i = inicio; i < elementos.size(); i++) {
            actual.add(elementos.get(i));
            generarCombinacionesAux(elementos, i + 1, k, actual, resultado);
            actual.remove(actual.size() - 1);
        }
    }
    
    public class SubsetResultado {
        public double mejorSilhouette;
        public List<Integer> mejorSubset;
        public ResultadoClustering resultado;
        
        public String generarReporte() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== SELECCIÓN DE SUBSET DE VARIABLES ===\n\n");
            sb.append("Mejor Silhouette: ").append(String.format("%.4f", mejorSilhouette)).append("\n");
            sb.append("Atributos seleccionados: ");
            if (mejorSubset != null) {
                for (int i : mejorSubset) {
                    sb.append(i).append(" ");
                }
            }
            sb.append("\n");
            return sb.toString();
        }
    }
}