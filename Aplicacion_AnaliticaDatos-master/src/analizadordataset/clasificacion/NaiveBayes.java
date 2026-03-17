package analizadordataset.clasificacion;

import analizadordataset.modelo.*;
import java.util.*;

public class NaiveBayes implements Clasificador {
    
    private int numInstancias;
    private int numAtributos;
    private int claseIndex;
    private Dataset dataset;
    
    // Estadísticas para cada clase y atributo
    private Map<String, Double> probabilidadesClase;
    private Map<String, Map<Integer, Map<String, Double>>> probabilidadesNominales;
    private Map<String, Map<Integer, double[]>> estadisticasNumericas; // [media, varianza]
    
    @Override
    public ResultadoClasificacion ejecutar(Dataset dataset) {
        this.dataset = dataset;
        this.numInstancias = dataset.getNumInstancias();
        this.numAtributos = dataset.getNumAtributos();
        this.claseIndex = numAtributos - 1;
        
        // Verificar que la clase sea nominal
        if (dataset.getTipoColumna(claseIndex) != 'C') {
            throw new IllegalArgumentException("NaiveBayes requiere clase nominal");
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
        
        // Inicializar estructuras
        probabilidadesClase = new HashMap<>();
        probabilidadesNominales = new HashMap<>();
        estadisticasNumericas = new HashMap<>();
        
        for (String clase : clasesArray) {
            probabilidadesNominales.put(clase, new HashMap<>());
            estadisticasNumericas.put(clase, new HashMap<>());
        }
        
        // Entrenar el modelo
        entrenar(clases, clasesArray);
        
        // Evaluar con validación cruzada leave-one-out
        double precision = evaluarLeaveOneOut(clases, clasesArray);
        
        // Generar matriz de confusión
        double[][] matrizConfusion = generarMatrizConfusion(clases, clasesArray);
        
        long endTime = System.currentTimeMillis();
        
        // Crear resultado
        ResultadoClasificacion resultado = new ResultadoClasificacion("Naive Bayes");
        resultado.setPrecision(precision);
        resultado.setMatrizConfusion(matrizConfusion, clasesArray);
        resultado.setTiempoEjecucion(endTime - startTime);
        
        resultado.agregarMetrica("Instancias", String.valueOf(numInstancias));
        resultado.agregarMetrica("Atributos", String.valueOf(numAtributos - 1));
        resultado.agregarMetrica("Clases", String.valueOf(clasesArray.length));
        
        return resultado;
    }
    
    private void entrenar(String[] clases, String[] clasesArray) {
        
        // Calcular probabilidades previas de cada clase
        Map<String, Integer> conteoClases = new HashMap<>();
        for (String clase : clases) {
            conteoClases.put(clase, conteoClases.getOrDefault(clase, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : conteoClases.entrySet()) {
            probabilidadesClase.put(entry.getKey(), (double) entry.getValue() / numInstancias);
        }
        
        // Calcular estadísticas para cada atributo por clase
        for (int attr = 0; attr < numAtributos - 1; attr++) {
            char tipo = dataset.getTipoColumna(attr);
            
            if (tipo == 'N') { // Atributo numérico
                for (String clase : clasesArray) {
                    // Recolectar valores de este atributo para esta clase
                    List<Double> valores = new ArrayList<>();
                    for (int i = 0; i < numInstancias; i++) {
                        if (clases[i].equals(clase)) {
                            valores.add(dataset.getValorNumerico(i, attr));
                        }
                    }
                    
                    if (valores.isEmpty()) continue;
                    
                    // Calcular media y varianza
                    double media = 0;
                    for (double v : valores) {
                        media += v;
                    }
                    media /= valores.size();
                    
                    double varianza = 0;
                    for (double v : valores) {
                        varianza += (v - media) * (v - media);
                    }
                    varianza /= valores.size();
                    
                    // Si la varianza es 0, poner un valor pequeño
                    if (varianza < 1e-10) {
                        varianza = 1e-10;
                    }
                    
                    // Guardar estadísticas
                    estadisticasNumericas.get(clase).put(attr, new double[]{media, varianza});
                }
                
            } else { // Atributo nominal
                // Obtener valores únicos de este atributo
                Set<String> valoresUnicos = dataset.getValoresUnicosColumna(attr);
                
                for (String clase : clasesArray) {
                    Map<Integer, Map<String, Double>> mapaAtributo = probabilidadesNominales.get(clase);
                    Map<String, Double> probabilidadesValor = new HashMap<>();
                    
                    // Contar ocurrencias de cada valor para esta clase
                    Map<String, Integer> conteo = new HashMap<>();
                    for (String valor : valoresUnicos) {
                        conteo.put(valor, 0);
                    }
                    
                    int totalClase = 0;
                    for (int i = 0; i < numInstancias; i++) {
                        if (clases[i].equals(clase)) {
                            totalClase++;
                            String valor = dataset.getValorNominal(i, attr);
                            conteo.put(valor, conteo.get(valor) + 1);
                        }
                    }
                    
                    if (totalClase == 0) continue;
                    
                    // Calcular probabilidades con suavizado Laplace
                    int numValores = valoresUnicos.size();
                    for (Map.Entry<String, Integer> entry : conteo.entrySet()) {
                        double prob = (double) (entry.getValue() + 1) / (totalClase + numValores);
                        probabilidadesValor.put(entry.getKey(), prob);
                    }
                    
                    mapaAtributo.put(attr, probabilidadesValor);
                }
            }
        }
    }
    
    private String clasificar(int instancia, String[] clasesArray) {
        double mejorProb = -Double.MAX_VALUE;
        String mejorClase = null;
        
        for (String clase : clasesArray) {
            Double probClase = probabilidadesClase.get(clase);
            if (probClase == null) continue;
            
            double logProb = Math.log(probClase);
            
            for (int attr = 0; attr < numAtributos - 1; attr++) {
                char tipo = dataset.getTipoColumna(attr);
                
                if (tipo == 'N') { // Atributo numérico
                    Map<Integer, double[]> statsMap = estadisticasNumericas.get(clase);
                    if (statsMap != null) {
                        double[] stats = statsMap.get(attr);
                        if (stats != null) {
                            double media = stats[0];
                            double varianza = stats[1];
                            double valor = dataset.getValorNumerico(instancia, attr);
                            
                            // Probabilidad bajo distribución normal
                            double prob = (1 / Math.sqrt(2 * Math.PI * varianza)) * 
                                          Math.exp(-Math.pow(valor - media, 2) / (2 * varianza));
                            
                            if (prob > 0) {
                                logProb += Math.log(prob);
                            }
                        }
                    }
                    
                } else { // Atributo nominal
                    Map<Integer, Map<String, Double>> mapaAtributo = probabilidadesNominales.get(clase);
                    if (mapaAtributo != null) {
                        Map<String, Double> probabilidadesValor = mapaAtributo.get(attr);
                        if (probabilidadesValor != null) {
                            String valor = dataset.getValorNominal(instancia, attr);
                            Double prob = probabilidadesValor.get(valor);
                            if (prob != null && prob > 0) {
                                logProb += Math.log(prob);
                            }
                        }
                    }
                }
            }
            
            if (logProb > mejorProb) {
                mejorProb = logProb;
                mejorClase = clase;
            }
        }
        
        return mejorClase != null ? mejorClase : clasesArray[0];
    }
    
    private double evaluarLeaveOneOut(String[] clases, String[] clasesArray) {
        int aciertos = 0;
        
        for (int i = 0; i < numInstancias; i++) {
            String clasePredicha = clasificar(i, clasesArray);
            if (clasePredicha.equals(clases[i])) {
                aciertos++;
            }
        }
        
        return (double) aciertos / numInstancias;
    }
    
    private double[][] generarMatrizConfusion(String[] clases, String[] clasesArray) {
        int n = clasesArray.length;
        double[][] matriz = new double[n][n];
        
        // Crear mapa de índice por clase
        Map<String, Integer> indiceClase = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indiceClase.put(clasesArray[i], i);
        }
        
        // Clasificar cada instancia
        for (int i = 0; i < numInstancias; i++) {
            String claseReal = clases[i];
            String clasePredicha = clasificar(i, clasesArray);
            
            Integer fila = indiceClase.get(claseReal);
            Integer columna = indiceClase.get(clasePredicha);
            
            if (fila != null && columna != null) {
                matriz[fila][columna]++;
            }
        }
        
        return matriz;
    }
}