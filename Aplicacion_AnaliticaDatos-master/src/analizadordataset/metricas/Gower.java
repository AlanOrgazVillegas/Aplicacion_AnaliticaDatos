package analizadordataset.metricas;
import analizadordataset.modelo.Dataset;
/**
 *
 * @author Alan
 */
public class Gower {
    
    private Dataset dataset;
    private double[] rangos; // Guardará el (Max - Min) de cada columna
    
    // Constructor: Al instanciar, calcula los rangos automáticamente
    public Gower(Dataset dataset) {
        this.dataset = dataset;
        calcularRangos();
    }
    
    /**
     * Paso 1 de Gower: Encontrar Máximos y Mínimos para estandarizar
     */
    private void calcularRangos() {
        int numAtributos = dataset.getNumAtributos();
        int numInstancias = dataset.getNumInstancias();
        rangos = new double[numAtributos];

        // Recorrer columna por columna
        for (int j = 0; j < numAtributos; j++) {
            // Si la columna es Numérica ('N')
            if (dataset.getTipoColumna(j) == 'N') {
                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                
                // Buscar max y min en todas las filas de esa columna
                for (int i = 0; i < numInstancias; i++) {
                    double valor = dataset.getValorNumerico(i, j);
                    if (valor < min) min = valor;
                    if (valor > max) max = valor;
                }
                
                rangos[j] = max - min;
                
                // Evitar división por cero si todos los datos de la columna son iguales
                if (rangos[j] == 0) {
                    rangos[j] = 1.0; 
                }
            } else {
                // Si es Nominal ('C' u otro), no necesita rango numérico
                rangos[j] = 0.0;
            }
        }
    }
    
    /**
     * Calcula la Similitud de Gower entre dos filas específicas.
     * @return Valor entre 0.0 (diferentes) y 1.0 (idénticos)
     */
    public double calcularSimilitud(int fila1, int fila2) {
        int numAtributos = dataset.getNumAtributos();
        double sumaSimilitud = 0.0;

        for (int j = 0; j < numAtributos; j++) {
            if (dataset.getTipoColumna(j) == 'N') {
                // Fórmula Gower para números: 1 - (|v1 - v2| / Rango)
                double v1 = dataset.getValorNumerico(fila1, j);
                double v2 = dataset.getValorNumerico(fila2, j);
                sumaSimilitud += (1.0 - (Math.abs(v1 - v2) / rangos[j]));
            } else {
                // Fórmula Gower para texto/nominal: 1 si son iguales, 0 si no
                String v1 = dataset.getValorNominal(fila1, j);
                String v2 = dataset.getValorNominal(fila2, j);
                if (v1.equals(v2)) {
                    sumaSimilitud += 1.0;
                }
            }
        }

        // Promedio de todas las columnas
        return sumaSimilitud / numAtributos;
    }
    
    /**
     * Calcula la Distancia de Gower (es la inversa de la similitud)
     * @return Valor entre 0.0 (idénticos) y 1.0 (muy distantes)
     */
    public double calcularDistancia(int fila1, int fila2) {
        return 1.0 - calcularSimilitud(fila1, fila2);
    }
    
}
