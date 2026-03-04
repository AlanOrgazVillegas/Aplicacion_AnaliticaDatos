package analizadordataset.tecnicas;

import analizadordataset.modelo.*;
import java.util.*;

public class ReliefF implements Tecnica {

    @Override
    public Resultado ejecutar(Dataset dataset) {

        String[] nombres = dataset.getNombresColumnas();
        int numAtributos = nombres.length - 1;
        double[] pesos = new double[numAtributos];

        for (int i = 0; i < dataset.getNumInstancias(); i++) {
            for (int j = 0; j < numAtributos; j++) {
                pesos[j] += Math.random(); // luego ponemos el real
            }
        }

        List<AtributoRanking> ranking = new ArrayList<>();

        for (int i = 0; i < numAtributos; i++) {
            ranking.add(new AtributoRanking(nombres[i], pesos[i]));
        }

        Collections.sort(ranking);

        return new Resultado("ReliefF Ranking Filter", ranking);
    }
}