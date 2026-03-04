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

            // luego agregas:
            // case "GOWER":
            // tecnica = new Gower();
            // break;
        }

        if (tecnica == null) {
            return null;
        }

        return tecnica.ejecutar(dataset);
    }
}