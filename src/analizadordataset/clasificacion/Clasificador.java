package analizadordataset.clasificacion;

import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.ResultadoClasificacion;

public interface Clasificador {
    ResultadoClasificacion ejecutar(Dataset dataset);
}