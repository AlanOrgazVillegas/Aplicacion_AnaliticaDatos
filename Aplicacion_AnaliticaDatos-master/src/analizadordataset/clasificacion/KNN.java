package analizadordataset.clasificacion;

import analizadordataset.modelo.*;
import java.util.*;

public class KNN implements Clasificador {
    
    private int k = 3; // Número de vecinos por defecto
    private Dataset dataset;
    private int numInstancias;
    private int numAtributos;
    private int claseIndex;
    
    public KNN() {
        // Constructor por defecto con k=3
    }
    
    public KNN(int k) {
        this.k = k;
    }
    
    @Override
    public ResultadoClasificacion ejecutar(Dataset dataset) {
        this.dataset = dataset;
        this.numInstancias = dataset.getNumInstancias();
        this.numAtributos = dataset.getNumAtributos();
        this.claseIndex = numAtributos - 1;
        
        // Verificar que la clase sea nominal
        if (dataset.getTipoColumna(claseIndex) != 'C') {
            throw new IllegalArgumentException("KNN requiere clase nominal");
        }
        
        long startTime = System.currentTimeMillis();
        
        // Obtener valores de clase
        String[] clases = new String[numInstancias];
        Set<String> clasesUnicas = new LinkedHashSet<>();
        for (int i = 0; i < numInstancias; i++) {
            clases[i] = dataset.getValorNominal(i, claseIndex);
            clasesUnicas.add(clases[i]);
        }
        String[] clasesArray = clasesUnicas.toArray(new String[0]);
        
        // Calcular min y max para normalización
        double[] minAtributos = new double[numAtributos - 1];
        double[] maxAtributos = new double[numAtributos - 1];
        Arrays.fill(minAtributos, Double.MAX_VALUE);
        Arrays.fill(maxAtributos, Double.MIN_VALUE);
        
        for (int i = 0; i < numInstancias; i++) {
            for (int j = 0; j < numAtributos - 1; j++) {
                if (dataset.getTipoColumna(j) == 'N') {
                    double valor = dataset.getValorNumerico(i, j);
                    minAtributos[j] = Math.min(minAtributos[j], valor);
                    maxAtributos[j] = Math.max(maxAtributos[j], valor);
                }
            }
        }
        
        // Evaluar con validación cruzada leave-one-out
        int aciertos = 0;
        double[][] matrizConfusion = new double[clasesArray.length][clasesArray.length];
        Map<String, Integer> indiceClase = new HashMap<>();
        for (int i = 0; i < clasesArray.length; i++) {
            indiceClase.put(clasesArray[i], i);
        }
        
        for (int i = 0; i < numInstancias; i++) {
            String clasePredicha = clasificar(i, clases, clasesArray, minAtributos, maxAtributos);
            if (clasePredicha.equals(clases[i])) {
                aciertos++;
            }
            
            int fila = indiceClase.get(clases[i]);
            int columna = indiceClase.get(clasePredicha);
            matrizConfusion[fila][columna]++;
        }
        
        double precision = (double) aciertos / numInstancias;
        long endTime = System.currentTimeMillis();
        
        // Crear resultado
        ResultadoClasificacion resultado = new ResultadoClasificacion("KNN (k=" + k + ")");
        resultado.setPrecision(precision);
        resultado.setMatrizConfusion(matrizConfusion, clasesArray);
        resultado.setTiempoEjecucion(endTime - startTime);
        
        resultado.agregarMetrica("Instancias", String.valueOf(numInstancias));
        resultado.agregarMetrica("Atributos", String.valueOf(numAtributos - 1));
        resultado.agregarMetrica("Clases", String.valueOf(clasesArray.length));
        resultado.agregarMetrica("k (vecinos)", String.valueOf(k));
        
        return resultado;
    }
    
    private String clasificar(int instancia, String[] clases, String[] clasesArray,
                              double[] minAtributos, double[] maxAtributos) {
        
        // Calcular distancias a todas las otras instancias
        List<Vecino> distancias = new ArrayList<>();
        
        for (int i = 0; i < numInstancias; i++) {
            if (i != instancia) {
                double distancia = calcularDistancia(instancia, i, minAtributos, maxAtributos);
                distancias.add(new Vecino(i, distancia));
            }
        }
        
        // Ordenar por distancia
        Collections.sort(distancias);
        
        // Obtener los k vecinos más cercanos
        Map<String, Integer> votos = new HashMap<>();
        for (int i = 0; i < Math.min(k, distancias.size()); i++) {
            Vecino vecino = distancias.get(i);
            String claseVecino = clases[vecino.indice];
            votos.put(claseVecino, votos.getOrDefault(claseVecino, 0) + 1);
        }
        
        // Encontrar la clase con más votos
        String clasePredicha = null;
        int maxVotos = -1;
        for (Map.Entry<String, Integer> entry : votos.entrySet()) {
            if (entry.getValue() > maxVotos) {
                maxVotos = entry.getValue();
                clasePredicha = entry.getKey();
            }
        }
        
        return clasePredicha;
    }
    
    private double calcularDistancia(int i, int j, double[] minAtributos, double[] maxAtributos) {
        double distancia = 0;
        
        for (int attr = 0; attr < numAtributos - 1; attr++) {
            double diff = calcularDiferencia(i, j, attr, minAtributos[attr], maxAtributos[attr]);
            distancia += diff * diff;
        }
        
        return Math.sqrt(distancia);
    }
    
    private double calcularDiferencia(int i, int j, int atributo, double minVal, double maxVal) {
        char tipo = dataset.getTipoColumna(atributo);
        
        if (tipo == 'N') { // Numérico
            double valI = dataset.getValorNumerico(i, atributo);
            double valJ = dataset.getValorNumerico(j, atributo);
            
            if (maxVal - minVal == 0) return 0;
            return Math.abs(valI - valJ) / (maxVal - minVal);
            
        } else { // Nominal
            String valI = dataset.getValorNominal(i, atributo);
            String valJ = dataset.getValorNominal(j, atributo);
            return valI.equals(valJ) ? 0 : 1;
        }
    }
    
    // Clase auxiliar para vecinos
    private class Vecino implements Comparable<Vecino> {
        int indice;
        double distancia;
        
        Vecino(int indice, double distancia) {
            this.indice = indice;
            this.distancia = distancia;
        }
        
        @Override
        public int compareTo(Vecino otro) {
            return Double.compare(this.distancia, otro.distancia);
        }
    }
}