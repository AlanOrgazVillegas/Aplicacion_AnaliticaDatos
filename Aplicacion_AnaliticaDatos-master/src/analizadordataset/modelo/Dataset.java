package analizadordataset.modelo;

import java.io.*;
import java.util.*;

public class Dataset {
    private List<String[]> datos;
    private String[] nombresColumnas;
    private char[] tiposColumnas; // 'N' para numérico, 'C' para categórico/nominal
    private int numInstancias;
    private int numAtributos;
    
    public Dataset() {
        this.datos = new ArrayList<>();
    }
    
    public void cargar(String rutaArchivo) throws IOException {
        datos.clear();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            // Leer cabecera
            String linea = br.readLine();
            if (linea == null) {
                throw new IOException("El archivo está vacío");
            }
            
            // Procesar nombres de columnas
            nombresColumnas = linea.split(",");
            numAtributos = nombresColumnas.length;
            
            // Inicializar array de tipos (asumimos que todos son numéricos hasta determinar lo contrario)
            tiposColumnas = new char[numAtributos];
            Arrays.fill(tiposColumnas, 'N'); // Por defecto numérico
            
            // Leer datos
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                
                String[] valores = linea.split(",");
                if (valores.length != numAtributos) {
                    System.err.println("Advertencia: línea con " + valores.length + 
                                     " columnas, esperaba " + numAtributos);
                    continue;
                }
                
                // Detectar tipos de datos
                for (int i = 0; i < valores.length; i++) {
                    if (tiposColumnas[i] == 'N') {
                        // Intentar parsear como número
                        try {
                            Double.parseDouble(valores[i].trim());
                        } catch (NumberFormatException e) {
                            // Si no es número, es nominal
                            tiposColumnas[i] = 'C';
                        }
                    }
                }
                
                datos.add(valores);
            }
        }
        
        numInstancias = datos.size();
        
        // Verificación final de tipos (si hay muy pocos valores numéricos, tratar como nominal)
        for (int i = 0; i < numAtributos; i++) {
            if (tiposColumnas[i] == 'N') {
                int valoresNumericos = 0;
                for (String[] fila : datos) {
                    try {
                        Double.parseDouble(fila[i].trim());
                        valoresNumericos++;
                    } catch (NumberFormatException e) {
                        // No hacer nada
                    }
                }
                // Si menos del 80% son numéricos, tratar como nominal
                if (valoresNumericos < numInstancias * 0.8) {
                    tiposColumnas[i] = 'C';
                }
            }
        }
    }
    
    public int getNumInstancias() {
        return numInstancias;
    }
    
    public int getNumAtributos() {
        return numAtributos;
    }
    
    public String[] getNombresColumnas() {
        return nombresColumnas;
    }
    
    public char getTipoColumna(int indice) {
        if (indice < 0 || indice >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + indice);
        }
        return tiposColumnas[indice];
    }
    
    public String getValorNominal(int fila, int columna) {
        if (fila < 0 || fila >= numInstancias) {
            throw new IndexOutOfBoundsException("Índice de fila inválido: " + fila);
        }
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        if (tiposColumnas[columna] != 'C') {
            throw new IllegalStateException("La columna " + columna + " no es nominal");
        }
        
        String[] filaDatos = datos.get(fila);
        return filaDatos[columna].trim();
    }
    
    public double getValorNumerico(int fila, int columna) {
        if (fila < 0 || fila >= numInstancias) {
            throw new IndexOutOfBoundsException("Índice de fila inválido: " + fila);
        }
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        if (tiposColumnas[columna] != 'N') {
            throw new IllegalStateException("La columna " + columna + " no es numérica");
        }
        
        String[] filaDatos = datos.get(fila);
        try {
            return Double.parseDouble(filaDatos[columna].trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Valor no numérico en fila " + fila + 
                                           ", columna " + columna + ": " + filaDatos[columna]);
        }
    }
    
    public String getValorComoString(int fila, int columna) {
        if (fila < 0 || fila >= numInstancias) {
            throw new IndexOutOfBoundsException("Índice de fila inválido: " + fila);
        }
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        
        String[] filaDatos = datos.get(fila);
        return filaDatos[columna].trim();
    }
    
    public Object getValor(int fila, int columna) {
        if (tiposColumnas[columna] == 'N') {
            return getValorNumerico(fila, columna);
        } else {
            return getValorNominal(fila, columna);
        }
    }
    
    public Set<String> getValoresUnicosColumna(int columna) {
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        
        Set<String> valoresUnicos = new TreeSet<>();
        for (String[] fila : datos) {
            valoresUnicos.add(fila[columna].trim());
        }
        return valoresUnicos;
    }
    
    public double getMinimoColumna(int columna) {
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        if (tiposColumnas[columna] != 'N') {
            throw new IllegalStateException("La columna " + columna + " no es numérica");
        }
        
        double min = Double.MAX_VALUE;
        for (int i = 0; i < numInstancias; i++) {
            double valor = getValorNumerico(i, columna);
            min = Math.min(min, valor);
        }
        return min;
    }
    
    public double getMaximoColumna(int columna) {
        if (columna < 0 || columna >= numAtributos) {
            throw new IndexOutOfBoundsException("Índice de columna inválido: " + columna);
        }
        if (tiposColumnas[columna] != 'N') {
            throw new IllegalStateException("La columna " + columna + " no es numérica");
        }
        
        double max = Double.MIN_VALUE;
        for (int i = 0; i < numInstancias; i++) {
            double valor = getValorNumerico(i, columna);
            max = Math.max(max, valor);
        }
        return max;
    }
    
    public void imprimirResumen() {
        System.out.println("=== RESUMEN DEL DATASET ===");
        System.out.println("Instancias: " + numInstancias);
        System.out.println("Atributos: " + numAtributos);
        System.out.println("\nColumnas:");
        for (int i = 0; i < numAtributos; i++) {
            System.out.printf("  %d. %s (%s)\n", 
                i + 1, 
                nombresColumnas[i], 
                tiposColumnas[i] == 'N' ? "Numérico" : "Nominal");
        }
        
        System.out.println("\nPrimeras 5 filas:");
        for (int i = 0; i < Math.min(5, numInstancias); i++) {
            StringBuilder sb = new StringBuilder();
            String[] fila = datos.get(i);
            for (int j = 0; j < fila.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(fila[j].trim());
            }
            System.out.println("  " + sb.toString());
        }
    }
    
    public void eliminarColumnas(int[] indicesAEliminar) {
        // 1. Evitar que el usuario borre absolutamente todo
        if (indicesAEliminar.length >= numAtributos) {
            throw new IllegalArgumentException("No puedes eliminar todas las columnas del dataset.");
        }

        // 2. Crear una "máscara" de lo que vamos a conservar
        boolean[] conservar = new boolean[numAtributos];
        Arrays.fill(conservar, true);
        for (int idx : indicesAEliminar) {
            conservar[idx] = false;
        }

        // 3. Preparar los nuevos arreglos con el nuevo tamaño
        int nuevoNumAtributos = numAtributos - indicesAEliminar.length;
        String[] nuevosNombres = new String[nuevoNumAtributos];
        char[] nuevosTipos = new char[nuevoNumAtributos];

        int contador = 0;
        for (int i = 0; i < numAtributos; i++) {
            if (conservar[i]) {
                nuevosNombres[contador] = nombresColumnas[i];
                nuevosTipos[contador] = tiposColumnas[i];
                contador++;
            }
        }

        // 4. Recortar los datos fila por fila
        List<String[]> nuevosDatos = new ArrayList<>();
        for (String[] fila : datos) {
            String[] nuevaFila = new String[nuevoNumAtributos];
            contador = 0;
            for (int i = 0; i < numAtributos; i++) {
                if (conservar[i]) {
                    nuevaFila[contador] = fila[i];
                    contador++;
                }
            }
            nuevosDatos.add(nuevaFila);
        }

        // 5. Reemplazar los datos viejos con los nuevos en la memoria
        this.nombresColumnas = nuevosNombres;
        this.tiposColumnas = nuevosTipos;
        this.datos = nuevosDatos;
        this.numAtributos = nuevoNumAtributos;
    }
    public Dataset clonar() {
    Dataset copia = new Dataset();
    
    // Copiar nombres de columnas
    copia.nombresColumnas = this.nombresColumnas.clone();
    
    // Copiar tipos de columnas
    copia.tiposColumnas = this.tiposColumnas.clone();
    
    // Copiar datos
    copia.datos = new ArrayList<>();
    for (String[] fila : this.datos) {
        copia.datos.add(fila.clone());
    }
    
    copia.numInstancias = this.numInstancias;
    copia.numAtributos = this.numAtributos;
    
    return copia;
}
    // Obtener un dataset sin la última columna (asumida como clase)
public Dataset getDatasetSinClase() throws Exception {
    if (getNumAtributos() <= 1) return clonar();
    Dataset nuevo = new Dataset();
    nuevo.nombresColumnas = new String[getNumAtributos() - 1];
    System.arraycopy(this.nombresColumnas, 0, nuevo.nombresColumnas, 0, getNumAtributos() - 1);
    
    nuevo.tiposColumnas = new char[getNumAtributos() - 1];
    System.arraycopy(this.tiposColumnas, 0, nuevo.tiposColumnas, 0, getNumAtributos() - 1);
    
    nuevo.datos = new ArrayList<>();
    for (String[] fila : this.datos) {
        String[] nuevaFila = new String[getNumAtributos() - 1];
        System.arraycopy(fila, 0, nuevaFila, 0, getNumAtributos() - 1);
        nuevo.datos.add(nuevaFila);
    }
    nuevo.numInstancias = this.numInstancias;
    nuevo.numAtributos = this.numAtributos - 1;
    return nuevo;
}

// Convertir columnas seleccionadas a matriz double (estandariza si true)
public double[][] getMatrizNumerica(int[] columnasIndices, boolean estandarizar) {
    if (columnasIndices == null) {
        columnasIndices = new int[getNumAtributos()];
        for (int i = 0; i < getNumAtributos(); i++) columnasIndices[i] = i;
    }
    int n = getNumInstancias();
    int m = columnasIndices.length;
    double[][] matriz = new double[n][m];
    
    // Codificación de variables categóricas
    java.util.Map<Integer, java.util.Map<String, Integer>> codificadores = new java.util.HashMap<>();
    for (int j = 0; j < m; j++) {
        int col = columnasIndices[j];
        if (tiposColumnas[col] == 'C') {
            codificadores.put(j, new java.util.HashMap<>());
        }
    }
    
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            int col = columnasIndices[j];
            String valor = datos.get(i)[col].trim();
            if (tiposColumnas[col] == 'N') {
                matriz[i][j] = Double.parseDouble(valor);
            } else {
                java.util.Map<String, Integer> mapa = codificadores.get(j);
                if (!mapa.containsKey(valor)) {
                    mapa.put(valor, mapa.size());
                }
                matriz[i][j] = mapa.get(valor);
            }
        }
    }
    
    if (estandarizar) {
        for (int j = 0; j < m; j++) {
            double media = 0;
            for (int i = 0; i < n; i++) media += matriz[i][j];
            media /= n;
            double var = 0;
            for (int i = 0; i < n; i++) var += Math.pow(matriz[i][j] - media, 2);
            double desv = Math.sqrt(var / n);
            if (desv < 1e-9) desv = 1;
            for (int i = 0; i < n; i++) {
                matriz[i][j] = (matriz[i][j] - media) / desv;
            }
        }
    }
    return matriz;
}
public Dataset crearSubset(List<Integer> indicesColumnas) {
    Dataset subset = new Dataset();
    
    // Nuevos nombres de columnas
    String[] nuevosNombres = new String[indicesColumnas.size()];
    char[] nuevosTipos = new char[indicesColumnas.size()];
    
    for (int i = 0; i < indicesColumnas.size(); i++) {
        int idx = indicesColumnas.get(i);
        nuevosNombres[i] = this.nombresColumnas[idx];
        nuevosTipos[i] = this.tiposColumnas[idx];
    }
    
    subset.nombresColumnas = nuevosNombres;
    subset.tiposColumnas = nuevosTipos;
    subset.numAtributos = nuevosNombres.length;
    subset.numInstancias = this.numInstancias;
    subset.datos = new ArrayList<>();
    
    // Copiar solo las columnas seleccionadas
    for (String[] fila : this.datos) {
        String[] nuevaFila = new String[indicesColumnas.size()];
        for (int i = 0; i < indicesColumnas.size(); i++) {
            int idx = indicesColumnas.get(i);
            nuevaFila[i] = fila[idx];
        }
        subset.datos.add(nuevaFila);
    }
    
    return subset;
}
}