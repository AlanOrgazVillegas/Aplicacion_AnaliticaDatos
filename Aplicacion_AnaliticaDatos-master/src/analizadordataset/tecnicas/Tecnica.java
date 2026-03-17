package analizadordataset.tecnicas;

import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.Resultado;

public interface Tecnica {
    Resultado ejecutar(Dataset dataset);
}
