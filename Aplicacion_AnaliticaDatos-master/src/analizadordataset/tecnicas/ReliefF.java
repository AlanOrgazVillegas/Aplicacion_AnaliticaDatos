package analizadordataset.tecnicas;

import analizadordataset.modelo.*;
import java.util.*;

public class ReliefF implements Tecnica {
    
    private static final int DEFAULT_K = 10; // Número de vecinos por defecto
    private int k;
    
    public ReliefF() {
        this.k = DEFAULT_K;
    }
    
    public ReliefF(int k) {
        this.k = k;
    }
    
    @Override
    public Resultado ejecutar(Dataset dataset) {
        int numInstancias = dataset.getNumInstancias();
        int numAtributos = dataset.getNumAtributos();
        String[] nombresAtributos = dataset.getNombresColumnas();
        
        // El último atributo es la clase (asumimos clasificación supervisada)
        int claseIndex = numAtributos - 1;
        numAtributos--; // No incluimos la clase en los pesos
        
        // Inicializar pesos
        double[] pesos = new double[numAtributos];
        Arrays.fill(pesos, 0.0);
        
        // Obtener valores de la clase para todas las instancias
        String[] clases = new String[numInstancias];
        double[] clasesNumericas = new double[numInstancias];
        boolean claseNominal = dataset.getTipoColumna(claseIndex) == 'C'; // 'C' para nominal/categórico
        
        for (int i = 0; i < numInstancias; i++) {
            if (claseNominal) {
                clases[i] = dataset.getValorNominal(i, claseIndex);
            } else {
                clasesNumericas[i] = dataset.getValorNumerico(i, claseIndex);
            }
        }
        
        // Calcular min y max para cada atributo numérico (para normalización)
        double[] minAtributos = new double[numAtributos];
        double[] maxAtributos = new double[numAtributos];
        Arrays.fill(minAtributos, Double.MAX_VALUE);
        Arrays.fill(maxAtributos, Double.MIN_VALUE);
        
        for (int i = 0; i < numInstancias; i++) {
            for (int j = 0; j < numAtributos; j++) {
                if (dataset.getTipoColumna(j) == 'N') { // 'N' para numérico
                    double valor = dataset.getValorNumerico(i, j);
                    minAtributos[j] = Math.min(minAtributos[j], valor);
                    maxAtributos[j] = Math.max(maxAtributos[j], valor);
                }
            }
        }
        
        // Para cada instancia
        for (int i = 0; i < numInstancias; i++) {
            // Encontrar k vecinos más cercanos
            List<Vecino> distancias = new ArrayList<>();
            
            for (int j = 0; j < numInstancias; j++) {
                if (i != j) {
                    double distancia = calcularDistancia(dataset, i, j, numAtributos, 
                                                          minAtributos, maxAtributos);
                    distancias.add(new Vecino(j, distancia));
                }
            }
            
            // Ordenar por distancia
            Collections.sort(distancias);
            
            // Separar hits y misses según la clase
            List<Integer> hits = new ArrayList<>();
            List<Integer> misses = new ArrayList<>();
            
            String claseActual = claseNominal ? clases[i] : String.valueOf(clasesNumericas[i]);
            
            for (int v = 0; v < Math.min(k * 2, distancias.size()); v++) {
                Vecino vecino = distancias.get(v);
                int idxVecino = vecino.indice;
                
                if (claseNominal) {
                    if (clases[idxVecino].equals(claseActual)) {
                        if (hits.size() < k) hits.add(idxVecino);
                    } else {
                        if (misses.size() < k) misses.add(idxVecino);
                    }
                } else {
                    if (Math.abs(clasesNumericas[idxVecino] - clasesNumericas[i]) < 0.001) {
                        if (hits.size() < k) hits.add(idxVecino);
                    } else {
                        if (misses.size() < k) misses.add(idxVecino);
                    }
                }
                
                if (hits.size() >= k && misses.size() >= k) break;
            }
            
            // Si no encontramos suficientes hits o misses, continuar con los que tenemos
            if (hits.isEmpty() && misses.isEmpty()) {
                continue; // No hay vecinos para esta instancia
            }
            
            // Actualizar pesos para cada atributo
            for (int j = 0; j < numAtributos; j++) {
                double diffHits = 0;
                double diffMisses = 0;
                
                // Calcular diferencia con hits
                for (int hitIdx : hits) {
                    diffHits += calcularDiferencia(dataset, i, hitIdx, j, 
                                                   minAtributos[j], maxAtributos[j]);
                }
                
                // Calcular diferencia con misses
                for (int missIdx : misses) {
                    diffMisses += calcularDiferencia(dataset, i, missIdx, j, 
                                                     minAtributos[j], maxAtributos[j]);
                }
                
                // Actualizar peso según fórmula de ReliefF
                int kEfectivo = Math.min(k, Math.min(hits.size(), misses.size()));
                if (kEfectivo > 0) {
                    double factor = 1.0 / (numInstancias * kEfectivo);
                    pesos[j] = pesos[j] - (diffHits * factor) + (diffMisses * factor);
                }
            }
        }
        
        // Crear ranking de atributos
        List<AtributoRanking> ranking = new ArrayList<>();
        for (int i = 0; i < numAtributos; i++) {
            ranking.add(new AtributoRanking(nombresAtributos[i], pesos[i]));
        }
        
        // Ordenar de mayor a menor peso
        Collections.sort(ranking, Collections.reverseOrder());
        
        // Crear resultado con información adicional
        Collections.sort(ranking); // Usa el compareTo de AtributoRanking
        // Collections.sort(ranking, Collections.reverseOrder());
        
        // Agregar métricas adicionales
        Map<String, String> metricas = new LinkedHashMap<>();
        metricas.put("Número de vecinos (k)", String.valueOf(k));
        metricas.put("Instancias procesadas", String.valueOf(numInstancias));
        metricas.put("Atributos evaluados", String.valueOf(numAtributos));
        metricas.put("Tipo de clase", claseNominal ? "Nominal" : "Numérico");
        
        // Calcular estadísticas de los pesos
        double pesoMin = Double.MAX_VALUE;
        double pesoMax = Double.MIN_VALUE;
        double sumaPesos = 0;
        int pesosValidos = 0;
        
        for (double peso : pesos) {
            if (!Double.isNaN(peso) && !Double.isInfinite(peso)) {
                pesoMin = Math.min(pesoMin, peso);
                pesoMax = Math.max(pesoMax, peso);
                sumaPesos += peso;
                pesosValidos++;
            }
        }
        
        if (pesosValidos > 0) {
            double pesoPromedio = sumaPesos / pesosValidos;
            metricas.put("Peso mínimo", String.format("%.6f", pesoMin));
            metricas.put("Peso máximo", String.format("%.6f", pesoMax));
            metricas.put("Peso promedio", String.format("%.6f", pesoPromedio));
        } else {
            metricas.put("Peso mínimo", "N/A");
            metricas.put("Peso máximo", "N/A");
            metricas.put("Peso promedio", "N/A");
        }
        Resultado resultado = new Resultado("ReliefF Ranking Filter", ranking);
        resultado.setMetricas(metricas);
        
        return resultado;
    }
    
    private double calcularDistancia(Dataset dataset, int i, int j, int numAtributos,
                                      double[] minAtributos, double[] maxAtributos) {
        double distancia = 0;
        
        for (int attr = 0; attr < numAtributos; attr++) {
            double diff = calcularDiferencia(dataset, i, j, attr, 
                                             minAtributos[attr], maxAtributos[attr]);
            distancia += diff * diff;
        }
        
        return Math.sqrt(distancia);
    }
    
    private double calcularDiferencia(Dataset dataset, int i, int j, int atributo,
                                       double minVal, double maxVal) {
        char tipo = dataset.getTipoColumna(atributo);
        
        if (tipo == 'N') { // Numérico
            double valI = dataset.getValorNumerico(i, atributo);
            double valJ = dataset.getValorNumerico(j, atributo);
            
            if (maxVal - minVal == 0) return 0;
            return Math.abs(valI - valJ) / (maxVal - minVal);
            
        } else { // Nominal (C)
            String valI = dataset.getValorNominal(i, atributo);
            String valJ = dataset.getValorNominal(j, atributo);
            
            return valI.equals(valJ) ? 0 : 1;
        }
    }
    
    // Clase auxiliar para almacenar vecinos
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