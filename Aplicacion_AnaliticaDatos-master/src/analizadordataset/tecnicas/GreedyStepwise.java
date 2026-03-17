package analizadordataset.tecnicas;

import analizadordataset.modelo.AtributoRanking;
import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.Resultado;

import java.util.*;

/**
 * GreedyStepwise – Selección de características por búsqueda greedy forward/backward.
 *
 * Implementa la interfaz {@link Tecnica} del proyecto, usando directamente la API
 * de {@link Dataset} (getValorNumerico, getValorNominal, getTipoColumna, etc.)
 *
 * Evaluador de subconjuntos: CFS (Correlation-based Feature Selection)
 *   Merit(S) = k * r_cf / sqrt(k + k*(k-1)*r_ff)
 *   donde r_cf = correlación promedio atributo-clase
 *         r_ff = correlación promedio entre pares de atributos del subconjunto
 *
 * El último atributo del dataset se trata como la clase (igual que ReliefF).
 */
public class GreedyStepwise implements Tecnica {

    // ── Configuración ─────────────────────────────────────────────────────────
    public enum Direccion { FORWARD, BACKWARD }

    private Direccion direccion = Direccion.FORWARD;
    private double    umbral    = 0.0;    // mejora mínima para aceptar cada paso
    private boolean   ranking   = false;  // true = continuar aunque el merit no mejore

    // ── Constructores ──────────────────────────────────────────────────────────

    /** Constructor por defecto: búsqueda FORWARD, umbral 0, sin ranking forzado. */
    public GreedyStepwise() {}

    public GreedyStepwise(Direccion direccion) {
        this.direccion = direccion;
    }

    public GreedyStepwise(Direccion direccion, double umbral, boolean generarRanking) {
        this.direccion = direccion;
        this.umbral    = umbral;
        this.ranking   = generarRanking;
    }

    // ── Punto de entrada (interfaz Tecnica) ───────────────────────────────────

    @Override
    public Resultado ejecutar(Dataset dataset) {

        long inicio = System.currentTimeMillis();

        int totalCols  = dataset.getNumAtributos();
        int claseIndex = totalCols - 1;          // última columna = clase
        int numFeats   = totalCols - 1;           // número de features sin la clase
        int numInst    = dataset.getNumInstancias();
        String[] nombres = dataset.getNombresColumnas();

        // ── 1. Codificar todo el dataset a double[][] ──────────────────────────
        //    Nominales → label-encoding  |  Numéricos → valor directo
        Map<String, Double>[] mapas = construirMapasCodigo(dataset, totalCols);

        double[][] feats     = new double[numFeats][numInst];
        double[]   claseVec  = new double[numInst];

        for (int i = 0; i < numInst; i++) {
            claseVec[i] = toDouble(dataset, i, claseIndex, mapas[claseIndex]);
            for (int f = 0; f < numFeats; f++) {
                int col = colReal(f, claseIndex);
                feats[f][i] = toDouble(dataset, i, col, mapas[col]);
            }
        }

        // ── 2. Búsqueda greedy ─────────────────────────────────────────────────
        List<Integer> ordenLog  = new ArrayList<>();
        List<Double>  meritLog  = new ArrayList<>();
        Set<Integer>  resultado;

        if (direccion == Direccion.FORWARD) {
            resultado = busquedaForward(feats, claseVec, numFeats, ordenLog, meritLog);
        } else {
            resultado = busquedaBackward(feats, claseVec, numFeats, ordenLog, meritLog);
        }

        long duracion = System.currentTimeMillis() - inicio;

        // ── 3. Construir lista de AtributoRanking ──────────────────────────────
        //    Orden: primero los seleccionados (en orden de selección),
        //           luego los descartados con merit = 0.
        List<AtributoRanking> listaRanking = new ArrayList<>();

        for (int paso = 0; paso < ordenLog.size(); paso++) {
            int f   = ordenLog.get(paso);
            int col = colReal(f, claseIndex);
            if (resultado.contains(f)) {
                double m = paso < meritLog.size() ? meritLog.get(paso) : 0.0;
                listaRanking.add(new AtributoRanking(nombres[col], m));
            }
        }
        for (int f = 0; f < numFeats; f++) {
            if (!resultado.contains(f)) {
                int col = colReal(f, claseIndex);
                listaRanking.add(new AtributoRanking(nombres[col], 0.0));
            }
        }

        // ── 4. Métricas del resultado ──────────────────────────────────────────
        double meritFinal = meritLog.isEmpty() ? 0.0 : meritLog.get(meritLog.size() - 1);

        Map<String, String> metricas = new LinkedHashMap<>();
        metricas.put("Algoritmo",               "GreedyStepwise");
        metricas.put("Dirección de búsqueda",    direccion == Direccion.FORWARD
                                                 ? "Forward (hacia adelante)"
                                                 : "Backward (hacia atrás)");
        metricas.put("Evaluador",                "CFS – Correlation-based Feature Selection");
        metricas.put("Umbral mínimo de mejora",  String.valueOf(umbral));
        metricas.put("Instancias procesadas",    String.valueOf(numInst));
        metricas.put("Atributos evaluados",      String.valueOf(numFeats));
        metricas.put("Atributos seleccionados",  String.valueOf(resultado.size()));
        metricas.put("Merit final",              String.format("%.6f", meritFinal));
        metricas.put("Tiempo de ejecución",      duracion + " ms");

        // Log paso a paso
        StringBuilder logPasos = new StringBuilder();
        for (int paso = 0; paso < ordenLog.size(); paso++) {
            int f   = ordenLog.get(paso);
            int col = colReal(f, claseIndex);
            boolean sel = resultado.contains(f);
            String accion;
            if (direccion == Direccion.FORWARD) {
                accion = sel ? "+ Añadido  " : "  Ignorado ";
            } else {
                accion = sel ? "  Mantenido" : "- Eliminado";
            }
            double m = paso < meritLog.size() ? meritLog.get(paso) : 0.0;
            logPasos.append(String.format("  Paso %2d: %s [%s]  →  merit = %.6f%n",
                    paso + 1, accion, nombres[col], m));
        }
        metricas.put("Log de pasos:", "\n" + logPasos);

        Resultado res = new Resultado("GreedyStepwise – CFS Evaluator", listaRanking);
        res.setMetricas(metricas);
        return res;
    }

    // ── Búsqueda FORWARD ───────────────────────────────────────────────────────

    private Set<Integer> busquedaForward(double[][] feats, double[] clase,
                                          int numFeats,
                                          List<Integer> ordenLog,
                                          List<Double> meritLog) {
        Set<Integer> actual     = new LinkedHashSet<>();
        Set<Integer> candidatos = new LinkedHashSet<>();
        for (int i = 0; i < numFeats; i++) candidatos.add(i);

        double meritActual = evaluarCFS(feats, clase, actual);

        while (!candidatos.isEmpty()) {
            int    mejorF = -1;
            double mejorM = Double.NEGATIVE_INFINITY;

            for (int c : candidatos) {
                Set<Integer> prueba = new LinkedHashSet<>(actual);
                prueba.add(c);
                double m = evaluarCFS(feats, clase, prueba);
                if (m > mejorM) { mejorM = m; mejorF = c; }
            }

            if (mejorF != -1 && (mejorM - meritActual) > umbral) {
                actual.add(mejorF);
                candidatos.remove(mejorF);
                ordenLog.add(mejorF);
                meritLog.add(mejorM);
                meritActual = mejorM;
            } else {
                break; // ninguna adición mejora → detener
            }
        }
        return actual;
    }

    // ── Búsqueda BACKWARD ─────────────────────────────────────────────────────

    private Set<Integer> busquedaBackward(double[][] feats, double[] clase,
                                           int numFeats,
                                           List<Integer> ordenLog,
                                           List<Double> meritLog) {
        Set<Integer> actual = new LinkedHashSet<>();
        for (int i = 0; i < numFeats; i++) actual.add(i);

        double meritActual = evaluarCFS(feats, clase, actual);

        while (actual.size() > 1) {
            int    peorF  = -1;
            double mejorM = Double.NEGATIVE_INFINITY;

            for (int c : actual) {
                Set<Integer> prueba = new LinkedHashSet<>(actual);
                prueba.remove(c);
                double m = evaluarCFS(feats, clase, prueba);
                if (m > mejorM) { mejorM = m; peorF = c; }
            }

            if (peorF != -1 && (mejorM - meritActual) > umbral) {
                actual.remove(peorF);
                ordenLog.add(peorF);
                meritLog.add(mejorM);
                meritActual = mejorM;
            } else {
                break;
            }
        }
        return actual;
    }

    // ── Evaluador CFS ──────────────────────────────────────────────────────────

    /**
     * Merit(S) = (k * r_cf) / sqrt(k + k*(k-1)*r_ff)
     */
    private double evaluarCFS(double[][] feats, double[] clase, Set<Integer> subset) {
        if (subset.isEmpty()) return 0.0;

        Integer[] arr = subset.toArray(new Integer[0]);
        int k = arr.length;

        // Correlación promedio feature-clase
        double sumRcf = 0.0;
        for (int f : arr) {
            sumRcf += Math.abs(pearson(feats[f], clase));
        }
        double rCF = sumRcf / k;

        // Correlación promedio entre pares de features
        double sumRff = 0.0;
        int pares = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                sumRff += Math.abs(pearson(feats[arr[i]], feats[arr[j]]));
                pares++;
            }
        }
        double rFF = pares > 0 ? sumRff / pares : 0.0;

        double denom = k + k * (k - 1) * rFF;
        return denom <= 0 ? 0.0 : (k * rCF) / Math.sqrt(denom);
    }

    private double pearson(double[] x, double[] y) {
        int n = x.length;
        double mx = media(x), my = media(y);
        double num = 0, sx = 0, sy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - mx, dy = y[i] - my;
            num += dx * dy;
            sx  += dx * dx;
            sy  += dy * dy;
        }
        double d = Math.sqrt(sx * sy);
        return d == 0 ? 0.0 : num / d;
    }

    private double media(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s / a.length;
    }

    // ── Codificación nominal ───────────────────────────────────────────────────

    /**
     * Crea un mapa de label-encoding por cada columna.
     * Columnas numéricas tienen mapa vacío (no se usan).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double>[] construirMapasCodigo(Dataset dataset, int totalCols) {
        Map<String, Double>[] mapas = new LinkedHashMap[totalCols];
        for (int col = 0; col < totalCols; col++) {
            mapas[col] = new LinkedHashMap<>();
            if (dataset.getTipoColumna(col) == 'C') {
                double codigo = 0;
                for (int i = 0; i < dataset.getNumInstancias(); i++) {
                    String val = dataset.getValorNominal(i, col);
                    if (!mapas[col].containsKey(val)) {
                        mapas[col].put(val, codigo++);
                    }
                }
            }
        }
        return mapas;
    }

    /** Convierte un valor de celda a double, usando el mapa para nominales. */
    private double toDouble(Dataset dataset, int fila, int col,
                             Map<String, Double> mapa) {
        if (dataset.getTipoColumna(col) == 'N') {
            return dataset.getValorNumerico(fila, col);
        }
        return mapa.getOrDefault(dataset.getValorNominal(fila, col), 0.0);
    }

    /**
     * Mapea un índice de feature (0..numFeats-1) al índice real de columna
     * en el Dataset, saltando la columna de clase.
     */
    private int colReal(int featureIdx, int claseIndex) {
        return featureIdx < claseIndex ? featureIdx : featureIdx + 1;
    }

    // ── Setters fluent ────────────────────────────────────────────────────────

    public GreedyStepwise setDireccion(Direccion d)    { this.direccion = d; return this; }
    public GreedyStepwise setUmbral(double u)           { this.umbral = u;   return this; }
    public GreedyStepwise setGenerarRanking(boolean r)  { this.ranking = r;  return this; }
}