package analizadordataset.controlador;

import analizadordataset.tecnicas.*;
import analizadordataset.modelo.*;

public class ControladorTecnicas {

    public Resultado ejecutarTecnica(String nombreTecnica, Dataset dataset) {

        Tecnica tecnica = null;

        switch (nombreTecnica) {
            case "RELIEF":
                tecnica = new ReliefF();
                break;
                
            case "CORRELATION":
                tecnica = new CorrelationScore();
                break;
                
            case "GREEDY":
                tecnica = new GreedyStepwise();
                break;
                
            case "INFOGAIN":
                tecnica = new InformationGain();
                break;
        }

        if (tecnica == null) {
            return null;
        }

        return tecnica.ejecutar(dataset);
    }
}