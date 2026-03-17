package analizadordataset.modelo;

public class AtributoRanking implements Comparable<AtributoRanking> {
    private String nombreAtributo;
    private double peso;
    
    public AtributoRanking(String nombreAtributo, double peso) {
        this.nombreAtributo = nombreAtributo;
        this.peso = peso;
    }
    
    public String getNombreAtributo() {
        return nombreAtributo;
    }
    
    public void setNombreAtributo(String nombreAtributo) {
        this.nombreAtributo = nombreAtributo;
    }
    
    public double getPeso() {
        return peso;
    }
    
    public void setPeso(double peso) {
        this.peso = peso;
    }
    
    @Override
    public int compareTo(AtributoRanking otro) {
        // Orden descendente por peso
        return Double.compare(otro.peso, this.peso);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %.6f", nombreAtributo, peso);
    }
}