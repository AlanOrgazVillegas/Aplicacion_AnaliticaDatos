package analizadordataset.controlador;

import analizadordataset.clustering.*;
import analizadordataset.modelo.*;

public class ControladorClasificacionNS {
    
    private int k = 3; // Valor por defecto
    private String enlace = "average"; // Valor por defecto
    private double eps = 0.5;
    private int minPts = 3;
    
    public void setK(int k) {
        this.k = k;
    }
    
    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }
    
    public void setEps(double eps) {
        this.eps = eps;
    }
    
    public void setMinPts(int minPts) {
        this.minPts = minPts;
    }
    
    public ResultadoClustering ejecutarClustering(String nombreAlgoritmo, Dataset dataset) {
        
        Clustering algoritmo = null;
        String nombreUpper = nombreAlgoritmo.toUpperCase();
        
        switch (nombreUpper) {
            case "KMEANSMANUAL":
                algoritmo = new KMeansManual(k);
                break;
                
            case "JERARQUICOMANUAL":
                algoritmo = new JerarquicoManual(k, enlace);
                break;
                
            case "DBSCANAUTO":
                algoritmo = new DBSCANAuto(eps, minPts);
                break;
                
            case "AUTOKMEANS":
                algoritmo = new AutoKMeans();
                break;
                
            default:
                return null;
        }
        
        return algoritmo.ejecutar(dataset);
    }
}