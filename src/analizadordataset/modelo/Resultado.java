package analizadordataset.modelo;

import java.util.List;

public class Resultado {

    private String nombreTecnica;
    private List<AtributoRanking> ranking;

    public Resultado(String nombreTecnica, List<AtributoRanking> ranking) {
        this.nombreTecnica = nombreTecnica;
        this.ranking = ranking;
    }

    public String generarReporte() {

        StringBuilder sb = new StringBuilder();

        sb.append("RESULTS FOR FEATURES SELECTION ON DATASET\n");
        sb.append("========================================\n\n");

        sb.append("=== Attribute Selection on all input data ===\n\n");

        sb.append("Search Method:\nAttribute ranking.\n\n");

        sb.append("Attribute Evaluator:\n");
        sb.append(nombreTecnica).append("\n\n");

        sb.append("Ranked attributes:\n");

        for (int i = 0; i < ranking.size(); i++) {
            AtributoRanking a = ranking.get(i);
            sb.append(String.format("%.6f", a.getPeso()))
              .append(" ")
              .append(i + 1)
              .append(" ")
              .append(a.getNombre())
              .append("\n");
        }

        return sb.toString();
    }
}