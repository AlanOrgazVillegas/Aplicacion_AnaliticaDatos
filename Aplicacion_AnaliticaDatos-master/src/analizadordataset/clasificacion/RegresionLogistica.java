package analizadordataset.clasificacion;

import analizadordataset.modelo.*;
import java.util.*;

public class RegresionLogistica implements Clasificador {
    
    private double[][] pesos; // [clase][atributo]
    private double[] bias;
    private double learningRate = 0.01;
    private int maxIteraciones = 1000;
    private Dataset dataset;
    private int numInstancias;
    private int numAtributos;
    private int claseIndex;
    private String[] clasesArray;
    
    @Override
    public ResultadoClasificacion ejecutar(Dataset dataset) {
        this.dataset = dataset;
        this.numInstancias = dataset.getNumInstancias();
        this.numAtributos = dataset.getNumAtributos();
        this.claseIndex = numAtributos - 1;
        
        // Verificar que la clase sea nominal
        if (dataset.getTipoColumna(claseIndex) != 'C') {
            throw new IllegalArgumentException("Regresión Logística requiere clase nominal");
        }
        
        long startTime = System.currentTimeMillis();
        
        // Obtener valores de clase
        String[] clases = new String[numInstancias];
        Set<String> clasesUnicas = new LinkedHashSet<>();
        for (int i = 0; i < numInstancias; i++) {
            clases[i] = dataset.getValorNominal(i, claseIndex);
            clasesUnicas.add(clases[i]);
        }
        clasesArray = clasesUnicas.toArray(new String[0]);
        
        // Normalizar atributos numéricos
        double[][] X = normalizarAtributos();
        
        // Crear matriz Y (one-hot encoding)
        double[][] Y = new double[numInstancias][clasesArray.length];
        Map<String, Integer> claseToIndex = new HashMap<>();
        for (int i = 0; i < clasesArray.length; i++) {
            claseToIndex.put(clasesArray[i], i);
        }
        for (int i = 0; i < numInstancias; i++) {
            int idx = claseToIndex.get(clases[i]);
            Y[i][idx] = 1.0;
        }
        
        // Inicializar pesos
        int numAtributosReales = numAtributos - 1;
        pesos = new double[clasesArray.length][numAtributosReales];
        bias = new double[clasesArray.length];
        
        // Entrenar con One-vs-Rest
        entrenar(X, Y);
        
        // Evaluar
        int aciertos = 0;
        double[][] matrizConfusion = new double[clasesArray.length][clasesArray.length];
        
        for (int i = 0; i < numInstancias; i++) {
            String clasePredicha = predecir(X[i]);
            int predIdx = claseToIndex.get(clasePredicha);
            int realIdx = claseToIndex.get(clases[i]);
            matrizConfusion[realIdx][predIdx]++;
            
            if (clasePredicha.equals(clases[i])) {
                aciertos++;
            }
        }
        
        double precision = (double) aciertos / numInstancias;
        long endTime = System.currentTimeMillis();
        
        ResultadoClasificacion resultado = new ResultadoClasificacion("Regresión Logística");
        resultado.setPrecision(precision);
        resultado.setMatrizConfusion(matrizConfusion, clasesArray);
        resultado.setTiempoEjecucion(endTime - startTime);
        
        resultado.agregarMetrica("Instancias", String.valueOf(numInstancias));
        resultado.agregarMetrica("Atributos", String.valueOf(numAtributos - 1));
        resultado.agregarMetrica("Clases", String.valueOf(clasesArray.length));
        resultado.agregarMetrica("Iteraciones", String.valueOf(maxIteraciones));
        
        return resultado;
    }
    
    private double[][] normalizarAtributos() {
        double[][] X = new double[numInstancias][numAtributos - 1];
        
        for (int attr = 0; attr < numAtributos - 1; attr++) {
            if (dataset.getTipoColumna(attr) == 'N') {
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                
                for (int i = 0; i < numInstancias; i++) {
                    double val = dataset.getValorNumerico(i, attr);
                    min = Math.min(min, val);
                    max = Math.max(max, val);
                }
                
                for (int i = 0; i < numInstancias; i++) {
                    double val = dataset.getValorNumerico(i, attr);
                    X[i][attr] = (max - min == 0) ? 0 : (val - min) / (max - min);
                }
            } else {
                // Atributos nominales: one-hot encoding simplificado
                Set<String> valores = dataset.getValoresUnicosColumna(attr);
                Map<String, Integer> valorToIndex = new HashMap<>();
                int idx = 0;
                for (String v : valores) {
                    valorToIndex.put(v, idx++);
                }
                
                for (int i = 0; i < numInstancias; i++) {
                    String val = dataset.getValorNominal(i, attr);
                    X[i][attr] = (double) valorToIndex.get(val) / valores.size();
                }
            }
        }
        
        return X;
    }
    
    private void entrenar(double[][] X, double[][] Y) {
        int numClases = clasesArray.length;
        int numAtributosReales = numAtributos - 1;
        
        for (int clase = 0; clase < numClases; clase++) {
            double[] w = new double[numAtributosReales];
            double b = 0;
            
            for (int iter = 0; iter < maxIteraciones; iter++) {
                double[] gradientesW = new double[numAtributosReales];
                double gradienteB = 0;
                
                for (int i = 0; i < numInstancias; i++) {
                    double z = b;
                    for (int j = 0; j < numAtributosReales; j++) {
                        z += w[j] * X[i][j];
                    }
                    double p = 1.0 / (1.0 + Math.exp(-z));
                    double error = p - Y[i][clase];
                    
                    for (int j = 0; j < numAtributosReales; j++) {
                        gradientesW[j] += error * X[i][j];
                    }
                    gradienteB += error;
                }
                
                // Actualizar pesos
                for (int j = 0; j < numAtributosReales; j++) {
                    w[j] -= learningRate * gradientesW[j] / numInstancias;
                }
                b -= learningRate * gradienteB / numInstancias;
            }
            
            pesos[clase] = w;
            bias[clase] = b;
        }
    }
    
    private String predecir(double[] x) {
        int numClases = clasesArray.length;
        double[] probabilidades = new double[numClases];
        
        for (int c = 0; c < numClases; c++) {
            double z = bias[c];
            for (int j = 0; j < x.length; j++) {
                z += pesos[c][j] * x[j];
            }
            probabilidades[c] = 1.0 / (1.0 + Math.exp(-z));
        }
        
        // Softmax para multiclase
        double suma = 0;
        for (double p : probabilidades) {
            suma += p;
        }
        for (int c = 0; c < numClases; c++) {
            probabilidades[c] /= suma;
        }
        
        int mejorClase = 0;
        for (int c = 1; c < numClases; c++) {
            if (probabilidades[c] > probabilidades[mejorClase]) {
                mejorClase = c;
            }
        }
        
        return clasesArray[mejorClase];
    }
}