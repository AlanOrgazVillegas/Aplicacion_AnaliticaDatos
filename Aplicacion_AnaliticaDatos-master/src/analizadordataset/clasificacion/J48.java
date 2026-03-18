package analizadordataset.clasificacion;

import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.ResultadoClasificacion;

import java.util.*;

/**
 * J48 – Árbol de Decisión basado en el algoritmo C4.5.
 *
 * Implementa la interfaz {@link Clasificador} del proyecto.
 * Usa directamente la API de {@link Dataset} igual que NaiveBayes.
 *
 * Características:
 *  - Criterio de división: Gain Ratio (como C4.5 / J48 original)
 *  - Atributos numéricos: búsqueda del umbral óptimo de corte binario
 *  - Atributos nominales: ramificación multi-valor
 *  - Poda: min. instancias por hoja (minInstanciasPorHoja, default = 2)
 *  - Evaluación: validación cruzada 10-fold
 *  - Salida: precisión, matriz de confusión, árbol en texto
 */
public class J48 implements Clasificador {

    // ── Hiperparámetros ───────────────────────────────────────────────────────
    private int    minInstanciasPorHoja = 2;   // poda por tamaño mínimo de hoja
    private int    folds                = 10;  // folds para validación cruzada
    private double umbralConfianza      = 0.25;// no se usa en esta versión simplificada

    // ── Estado interno ────────────────────────────────────────────────────────
    private Dataset dataset;
    private int     numInstancias;
    private int     numAtributos;
    private int     claseIndex;

    // ── Constructores ─────────────────────────────────────────────────────────
    public J48() {}

    public J48(int minInstanciasPorHoja, int folds) {
        this.minInstanciasPorHoja = minInstanciasPorHoja;
        this.folds                = folds;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUNTO DE ENTRADA  (interfaz Clasificador)
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public ResultadoClasificacion ejecutar(Dataset dataset) {
        this.dataset       = dataset;
        this.numInstancias = dataset.getNumInstancias();
        this.numAtributos  = dataset.getNumAtributos();
        this.claseIndex    = numAtributos - 1;

        if (dataset.getTipoColumna(claseIndex) != 'C') {
            throw new IllegalArgumentException("J48 requiere una clase nominal (última columna).");
        }

        long inicio = System.currentTimeMillis();

        // ── Obtener clases ────────────────────────────────────────────────────
        String[] clases       = new String[numInstancias];
        Set<String> clasesSet = new LinkedHashSet<>();
        for (int i = 0; i < numInstancias; i++) {
            clases[i] = dataset.getValorNominal(i, claseIndex);
            clasesSet.add(clases[i]);
        }
        String[] clasesArray = clasesSet.toArray(new String[0]);

        // ── Índices de todos los atributos (sin la clase) ─────────────────────
        List<Integer> todosAtributos = new ArrayList<>();
        for (int i = 0; i < numAtributos - 1; i++) todosAtributos.add(i);

        // Índices de todas las instancias
        List<Integer> todasInstancias = new ArrayList<>();
        for (int i = 0; i < numInstancias; i++) todasInstancias.add(i);

        // ── Construir árbol con todos los datos ───────────────────────────────
        Nodo arbolCompleto = construirArbol(todasInstancias, todosAtributos, clases);

        // ── Validación cruzada k-fold ──────────────────────────────────────────
        double precision       = validacionCruzada(todasInstancias, todosAtributos,
                                                    clases, clasesArray);
        double[][] matrizConf  = generarMatrizConfusion(arbolCompleto, todasInstancias,
                                                         clases, clasesArray);

        long duracion = System.currentTimeMillis() - inicio;

        // ── Construir resultado ───────────────────────────────────────────────
        ResultadoClasificacion resultado = new ResultadoClasificacion("J48 – Árbol de Decisión (C4.5)");
        resultado.setPrecision(precision);
        resultado.setMatrizConfusion(matrizConf, clasesArray);
        resultado.setTiempoEjecucion(duracion);

        resultado.agregarMetrica("Instancias",              String.valueOf(numInstancias));
        resultado.agregarMetrica("Atributos",               String.valueOf(numAtributos - 1));
        resultado.agregarMetrica("Clases",                  String.valueOf(clasesArray.length));
        resultado.agregarMetrica("Folds (validación cruzada)", String.valueOf(folds));
        resultado.agregarMetrica("Min. instancias por hoja",String.valueOf(minInstanciasPorHoja));
        resultado.agregarMetrica("Criterio de división",    "Gain Ratio (C4.5)");
        resultado.agregarMetrica("Estructura del árbol",
                "\n" + arbolCompleto.toString("", true));

        return resultado;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DEL ÁRBOL
    // ═════════════════════════════════════════════════════════════════════════

    private Nodo construirArbol(List<Integer> instancias,
                                 List<Integer> atributosDisponibles,
                                 String[] clases) {

        // ── Caso base 1: sin instancias ───────────────────────────────────────
        if (instancias.isEmpty()) {
            return new Nodo(clasesMayoritaria(clases, Collections.emptyList()));
        }

        // ── Caso base 2: todos de la misma clase ──────────────────────────────
        String claseUnica = claseUnica(instancias, clases);
        if (claseUnica != null) {
            return new Nodo(claseUnica);
        }

        // ── Caso base 3: sin atributos disponibles ────────────────────────────
        if (atributosDisponibles.isEmpty()) {
            return new Nodo(clasesMayoritaria(clases, instancias));
        }

        // ── Caso base 4: pocas instancias (poda) ──────────────────────────────
        if (instancias.size() < minInstanciasPorHoja * 2) {
            return new Nodo(clasesMayoritaria(clases, instancias));
        }

        // ── Seleccionar el mejor atributo (Gain Ratio) ────────────────────────
        int    mejorAttr      = -1;
        double mejorGainRatio = -Double.MAX_VALUE;
        double mejorUmbral    = 0;      // solo para atributos numéricos

        for (int attr : atributosDisponibles) {
            double gainRatio;
            double umbral = 0;

            if (dataset.getTipoColumna(attr) == 'N') {
                double[] resultado = mejorUmbralNumerico(instancias, clases, attr);
                gainRatio = resultado[0];
                umbral    = resultado[1];
            } else {
                gainRatio = calcularGainRatioNominal(instancias, clases, attr);
            }

            if (gainRatio > mejorGainRatio) {
                mejorGainRatio = gainRatio;
                mejorAttr      = attr;
                mejorUmbral    = umbral;
            }
        }

        // Si no hay ganancia positiva → hoja
        if (mejorAttr == -1 || mejorGainRatio <= 0) {
            return new Nodo(clasesMayoritaria(clases, instancias));
        }

        // ── Dividir según el mejor atributo ───────────────────────────────────
        Nodo nodo = new Nodo(mejorAttr,
                             dataset.getNombresColumnas()[mejorAttr],
                             dataset.getTipoColumna(mejorAttr) == 'N',
                             mejorUmbral,
                             clasesMayoritaria(clases, instancias));

        if (dataset.getTipoColumna(mejorAttr) == 'N') {
            // División binaria: ≤ umbral  /  > umbral
            List<Integer> izq = new ArrayList<>(), der = new ArrayList<>();
            for (int i : instancias) {
                if (dataset.getValorNumerico(i, mejorAttr) <= mejorUmbral) izq.add(i);
                else der.add(i);
            }

            List<Integer> atrsRestantes = new ArrayList<>(atributosDisponibles);
            // Para numéricos NO quitamos el atributo (puede reusarse con otro umbral)

            if (!izq.isEmpty())
                nodo.agregarHijo("<= " + String.format("%.4f", mejorUmbral),
                                  construirArbol(izq, atrsRestantes, clases));
            if (!der.isEmpty())
                nodo.agregarHijo("> "  + String.format("%.4f", mejorUmbral),
                                  construirArbol(der, atrsRestantes, clases));

        } else {
            // División multi-valor
            List<Integer> atrsRestantes = new ArrayList<>(atributosDisponibles);
            atrsRestantes.remove(Integer.valueOf(mejorAttr));

            Map<String, List<Integer>> grupos = agruparPorValorNominal(instancias, mejorAttr);
            for (Map.Entry<String, List<Integer>> entry : grupos.entrySet()) {
                nodo.agregarHijo(entry.getKey(),
                                  construirArbol(entry.getValue(), atrsRestantes, clases));
            }
        }

        return nodo;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CÁLCULO DE GAIN RATIO
    // ═════════════════════════════════════════════════════════════════════════

    /** Gain Ratio para atributo nominal. */
    private double calcularGainRatioNominal(List<Integer> instancias,
                                             String[] clases, int attr) {
        double entropiaBase = entropia(instancias, clases);
        if (entropiaBase == 0) return 0;

        Map<String, List<Integer>> grupos = agruparPorValorNominal(instancias, attr);

        double entropiaCondicional = 0;
        double splitInfo           = 0;
        int    n                   = instancias.size();

        for (List<Integer> grupo : grupos.values()) {
            double p = (double) grupo.size() / n;
            entropiaCondicional += p * entropia(grupo, clases);
            if (p > 0) splitInfo -= p * Math.log(p) / Math.log(2);
        }

        double ganancia = entropiaBase - entropiaCondicional;
        return splitInfo == 0 ? 0 : ganancia / splitInfo;
    }

    /**
     * Busca el umbral óptimo de corte para un atributo numérico.
     * @return [gainRatio, umbral]
     */
    private double[] mejorUmbralNumerico(List<Integer> instancias,
                                          String[] clases, int attr) {
        // Ordenar instancias por valor del atributo
        List<Integer> ordenadas = new ArrayList<>(instancias);
        ordenadas.sort(Comparator.comparingDouble(i -> dataset.getValorNumerico(i, attr)));

        double entropiaBase  = entropia(instancias, clases);
        double mejorGR       = -Double.MAX_VALUE;
        double mejorUmbral   = 0;
        int    n             = instancias.size();

        for (int k = 0; k < ordenadas.size() - 1; k++) {
            double v1 = dataset.getValorNumerico(ordenadas.get(k),   attr);
            double v2 = dataset.getValorNumerico(ordenadas.get(k+1), attr);
            if (v1 == v2) continue;

            double umbral = (v1 + v2) / 2.0;

            List<Integer> izq = ordenadas.subList(0, k + 1);
            List<Integer> der = ordenadas.subList(k + 1, ordenadas.size());

            double pIzq = (double) izq.size() / n;
            double pDer = (double) der.size() / n;

            double entCondicional = pIzq * entropia(izq, clases)
                                  + pDer * entropia(der, clases);
            double ganancia   = entropiaBase - entCondicional;

            double splitInfo  = 0;
            if (pIzq > 0) splitInfo -= pIzq * Math.log(pIzq) / Math.log(2);
            if (pDer > 0) splitInfo -= pDer * Math.log(pDer) / Math.log(2);

            double gr = splitInfo == 0 ? 0 : ganancia / splitInfo;

            if (gr > mejorGR) {
                mejorGR     = gr;
                mejorUmbral = umbral;
            }
        }
        return new double[]{ mejorGR, mejorUmbral };
    }

    /** Entropía de Shannon de un subconjunto de instancias. */
    private double entropia(List<Integer> instancias, String[] clases) {
        if (instancias.isEmpty()) return 0;
        Map<String, Integer> conteo = new HashMap<>();
        for (int i : instancias) conteo.merge(clases[i], 1, Integer::sum);
        double ent = 0;
        int n = instancias.size();
        for (int c : conteo.values()) {
            double p = (double) c / n;
            if (p > 0) ent -= p * Math.log(p) / Math.log(2);
        }
        return ent;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CLASIFICACIÓN DE UNA INSTANCIA
    // ═════════════════════════════════════════════════════════════════════════

    private String clasificar(Nodo raiz, int instancia) {
        Nodo actual = raiz;
        while (!actual.esHoja()) {
            int attr = actual.atributo;
            if (actual.esNumerico) {
                double val = dataset.getValorNumerico(instancia, attr);
                String rama = val <= actual.umbral
                        ? "<= " + String.format("%.4f", actual.umbral)
                        : "> "  + String.format("%.4f", actual.umbral);
                Nodo hijo = actual.hijos.get(rama);
                if (hijo == null) return actual.claseMayoritaria;
                actual = hijo;
            } else {
                String val  = dataset.getValorNominal(instancia, attr);
                Nodo   hijo = actual.hijos.get(val);
                if (hijo == null) return actual.claseMayoritaria; // valor desconocido
                actual = hijo;
            }
        }
        return actual.claseHoja;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  VALIDACIÓN CRUZADA k-FOLD
    // ═════════════════════════════════════════════════════════════════════════

    private double validacionCruzada(List<Integer> todas, List<Integer> atributos,
                                      String[] clases, String[] clasesArray) {
        int k = Math.min(folds, todas.size());
        if (k <= 1) return evaluarEntrenamiento(todas, atributos, clases);

        // Mezclar instancias aleatoriamente
        List<Integer> mezcladas = new ArrayList<>(todas);
        Collections.shuffle(mezcladas, new Random(42));

        int aciertos = 0;
        int foldSize = mezcladas.size() / k;

        for (int fold = 0; fold < k; fold++) {
            int inicio = fold * foldSize;
            int fin    = (fold == k - 1) ? mezcladas.size() : inicio + foldSize;

            List<Integer> prueba       = mezcladas.subList(inicio, fin);
            List<Integer> entrenamiento = new ArrayList<>();
            entrenamiento.addAll(mezcladas.subList(0, inicio));
            entrenamiento.addAll(mezcladas.subList(fin, mezcladas.size()));

            Nodo arbol = construirArbol(entrenamiento, atributos, clases);

            for (int inst : prueba) {
                String predicha = clasificar(arbol, inst);
                if (predicha.equals(clases[inst])) aciertos++;
            }
        }

        return (double) aciertos / mezcladas.size();
    }

    /** Precisión sobre el mismo conjunto de entrenamiento (si k-fold no aplica). */
    private double evaluarEntrenamiento(List<Integer> instancias,
                                         List<Integer> atributos,
                                         String[] clases) {
        Nodo arbol   = construirArbol(instancias, atributos, clases);
        int aciertos = 0;
        for (int i : instancias) {
            if (clasificar(arbol, i).equals(clases[i])) aciertos++;
        }
        return (double) aciertos / instancias.size();
    }

    /** Matriz de confusión usando el árbol entrenado con todos los datos. */
    private double[][] generarMatrizConfusion(Nodo arbol, List<Integer> instancias,
                                               String[] clases, String[] clasesArray) {
        int n = clasesArray.length;
        double[][] matriz = new double[n][n];
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(clasesArray[i], i);

        for (int i : instancias) {
            String real     = clases[i];
            String predicha = clasificar(arbol, i);
            Integer fila    = idx.get(real);
            Integer col     = idx.get(predicha);
            if (fila != null && col != null) matriz[fila][col]++;
        }
        return matriz;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═════════════════════════════════════════════════════════════════════════

    /** Devuelve la clase si todas las instancias son de la misma, null si no. */
    private String claseUnica(List<Integer> instancias, String[] clases) {
        String primera = clases[instancias.get(0)];
        for (int i : instancias) {
            if (!clases[i].equals(primera)) return null;
        }
        return primera;
    }

    /** Devuelve la clase con más instancias en el subconjunto. */
    private String clasesMayoritaria(String[] clases, List<Integer> instancias) {
        if (instancias.isEmpty()) {
            // Contar en todo el dataset
            Map<String, Integer> cnt = new HashMap<>();
            for (String c : clases) cnt.merge(c, 1, Integer::sum);
            return cnt.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(clases[0]);
        }
        Map<String, Integer> cnt = new HashMap<>();
        for (int i : instancias) cnt.merge(clases[i], 1, Integer::sum);
        return cnt.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(clases[instancias.get(0)]);
    }

    /** Agrupa índices de instancias por valor nominal del atributo indicado. */
    private Map<String, List<Integer>> agruparPorValorNominal(List<Integer> instancias,
                                                                int attr) {
        Map<String, List<Integer>> grupos = new LinkedHashMap<>();
        for (int i : instancias) {
            String val = dataset.getValorNominal(i, attr);
            grupos.computeIfAbsent(val, k -> new ArrayList<>()).add(i);
        }
        return grupos;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  NODO DEL ÁRBOL (clase interna)
    // ═════════════════════════════════════════════════════════════════════════

    private static class Nodo {

        // Nodo interno
        int    atributo;
        String nombreAtributo;
        boolean esNumerico;
        double  umbral;           // solo para numéricos
        String  claseMayoritaria; // para clasificar instancias con rama desconocida

        // Nodo hoja
        String claseHoja;

        // Hijos: clave = valor de la rama (p.ej. "Sí", "<= 3.5", "> 3.5")
        Map<String, Nodo> hijos = new LinkedHashMap<>();

        /** Constructor para hoja. */
        Nodo(String claseHoja) {
            this.claseHoja = claseHoja;
        }

        /** Constructor para nodo interno. */
        Nodo(int atributo, String nombreAtributo, boolean esNumerico,
             double umbral, String claseMayoritaria) {
            this.atributo         = atributo;
            this.nombreAtributo   = nombreAtributo;
            this.esNumerico       = esNumerico;
            this.umbral           = umbral;
            this.claseMayoritaria = claseMayoritaria;
        }

        boolean esHoja() { return claseHoja != null; }

        void agregarHijo(String rama, Nodo hijo) { hijos.put(rama, hijo); }

        /** Representación textual del árbol (estilo Weka). */
        String toString(String prefijo, boolean ultimo) {
            StringBuilder sb = new StringBuilder();

            if (esHoja()) {
                sb.append(prefijo).append(ultimo ? "└── " : "├── ")
                  .append("[").append(claseHoja).append("]\n");
            } else {
                sb.append(prefijo).append(ultimo ? "└── " : "├── ")
                  .append(nombreAtributo);
                if (esNumerico) sb.append(" (umbral: ").append(String.format("%.4f", umbral)).append(")");
                sb.append("\n");

                List<Map.Entry<String, Nodo>> entradas = new ArrayList<>(hijos.entrySet());
                for (int i = 0; i < entradas.size(); i++) {
                    boolean esUltimo = (i == entradas.size() - 1);
                    String nuevoP    = prefijo + (ultimo ? "    " : "│   ");
                    sb.append(nuevoP)
                      .append(esUltimo ? "└── " : "├── ")
                      .append(entradas.get(i).getKey()).append(":\n");
                    sb.append(entradas.get(i).getValue()
                              .toString(nuevoP + (esUltimo ? "    " : "│   "), true));
                }
            }
            return sb.toString();
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public J48 setMinInstanciasPorHoja(int min) { this.minInstanciasPorHoja = min; return this; }
    public J48 setFolds(int folds)              { this.folds = folds;              return this; }
}