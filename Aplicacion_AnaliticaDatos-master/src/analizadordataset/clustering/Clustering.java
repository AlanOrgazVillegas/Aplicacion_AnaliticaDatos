package analizadordataset.clustering;

import analizadordataset.modelo.ResultadoClustering;
import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.ResultadoClustering;

public interface Clustering {
    ResultadoClustering ejecutar(Dataset dataset);
}