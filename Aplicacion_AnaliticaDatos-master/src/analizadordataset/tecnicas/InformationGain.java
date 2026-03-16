/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package analizadordataset.tecnicas;

/**
 *
 * @author Martin Cesar
 */
import analizadordataset.modelo.*;
import java.util.*;

public class InformationGain implements Tecnica {

    public InformationGain() {
        // Constructor por defecto
    }

    @Override
    public Resultado ejecutar(Dataset dataset) {
        int numInstancias = dataset.getNumInstancias();
        int numAtributos = dataset.getNumAtributos() - 1; // Excluir la clase
        String[] nombresAtributos = dataset.getNombresColumnas();
        int claseIndex = dataset.getNumAtributos() - 1;

        // Information Gain requiere clase categórica (nominal)
        if (dataset.getTipoColumna(claseIndex) != 'C') {
            throw new IllegalArgumentException("Information Gain requiere que la clase sea categórica.");
        }

        double entropiaTotal = calcularEntropia(dataset, claseIndex);
        double[] scores = new double[numAtributos];

        for (int i = 0; i < numAtributos; i++) {
            scores[i] = entropiaTotal - calcularEntropiaCondicional(dataset, i, claseIndex);
        }

        List<AtributoRanking> ranking = new ArrayList<>();
        for (int i = 0; i < numAtributos; i++) {
            // Redondeo a 4 decimales para precisión
            double scoreRedondeado = Math.round(scores[i] * 10000.0) / 10000.0;
            ranking.add(new AtributoRanking(nombresAtributos[i], scoreRedondeado));
        }

        // Ordenar de mayor a menor ganancia
        ranking.sort((a, b) -> Double.compare(b.getPeso(), a.getPeso()));

        Map<String, String> metricas = new LinkedHashMap<>();
        metricas.put("Entropía Base", String.format("%.4f", entropiaTotal));
        metricas.put("Atributos procesados", String.valueOf(numAtributos));

        Resultado resultado = new Resultado("Information Gain Ranking Filter", ranking);
        resultado.setMetricas(metricas);

        return resultado;
    }

    private double calcularEntropia(Dataset dataset, int columna) {
        Map<String, Integer> frecuencias = new HashMap<>();
        int total = dataset.getNumInstancias();

        for (int i = 0; i < total; i++) {
            String valor = dataset.getValorNominal(i, columna);
            frecuencias.put(valor, frecuencias.getOrDefault(valor, 0) + 1);
        }

        double entropia = 0;
        for (int f : frecuencias.values()) {
            double p = (double) f / total;
            entropia -= p * (Math.log(p) / Math.log(2));
        }
        return entropia;
    }

    private double calcularEntropiaCondicional(Dataset dataset, int attrIdx, int claseIdx) {
        int total = dataset.getNumInstancias();
        Map<String, Map<String, Integer>> conteos = new HashMap<>();
        Map<String, Integer> totalPorValor = new HashMap<>();

        for (int i = 0; i < total; i++) {
            String valAttr = (dataset.getTipoColumna(attrIdx) == 'N') 
                ? String.valueOf(dataset.getValorNumerico(i, attrIdx)) 
                : dataset.getValorNominal(i, attrIdx);
            String valClase = dataset.getValorNominal(i, claseIdx);

            conteos.putIfAbsent(valAttr, new HashMap<>());
            Map<String, Integer> mapaClase = conteos.get(valAttr);
            mapaClase.put(valClase, mapaClase.getOrDefault(valClase, 0) + 1);
            totalPorValor.put(valAttr, totalPorValor.getOrDefault(valAttr, 0) + 1);
        }

        double entropiaCond = 0;
        for (String valAttr : conteos.keySet()) {
            double probSubconjunto = (double) totalPorValor.get(valAttr) / total;
            double entropiaSub = 0;
            Map<String, Integer> clases = conteos.get(valAttr);
            for (int count : clases.values()) {
                double p = (double) count / totalPorValor.get(valAttr);
                entropiaSub -= p * (Math.log(p) / Math.log(2));
            }
            entropiaCond += probSubconjunto * entropiaSub;
        }
        return entropiaCond;
    }
}