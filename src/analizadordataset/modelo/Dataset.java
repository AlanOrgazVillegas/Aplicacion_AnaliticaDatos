package analizadordataset.modelo;

import java.io.*;
import java.util.*;

public class Dataset {

    private List<String[]> datos = new ArrayList<>();
    private String[] nombresColumnas;
    
    public void cargar(String ruta) throws Exception {
        datos.clear();
        BufferedReader br = new BufferedReader(new FileReader(ruta));
        
        String linea;
        boolean primera = true;
        
        while ((linea = br.readLine()) != null) {
            String[] fila = linea.split(",");

            if (primera) {
                nombresColumnas = fila;
                primera = false;
            } else {
                datos.add(fila);
            }
        }
        br.close();
    }
    
    public List<String[]> getDatos() { return datos; }
    public String[] getNombresColumnas() { return nombresColumnas; }
    public int getNumInstancias() { return datos.size(); }
    public int getNumAtributos() { return nombresColumnas.length; }
}
