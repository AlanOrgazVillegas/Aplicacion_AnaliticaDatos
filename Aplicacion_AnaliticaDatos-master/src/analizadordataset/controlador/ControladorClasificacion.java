package analizadordataset.controlador;

import analizadordataset.clasificacion.*;
import analizadordataset.modelo.*;

public class ControladorClasificacion {
    
    public ResultadoClasificacion ejecutarClasificador(String nombreClasificador, Dataset dataset) {
        
        Clasificador clasificador = null;
        
        switch (nombreClasificador.toUpperCase()) {
            case "NAIVEBAYES":
                clasificador = new NaiveBayes();
                break;
                
            case "J48":
                clasificador = new J48();
                break;
        }
        
        if (clasificador == null) {
            return null;
        }
        
        return clasificador.ejecutar(dataset);
    }
}