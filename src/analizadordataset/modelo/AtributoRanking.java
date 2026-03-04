package analizadordataset.modelo;

public class AtributoRanking implements Comparable<AtributoRanking>{

    private String nombre;
    private double peso;
    
    public AtributoRanking(String nombre, double peso) {
        this.nombre = nombre;
        this.peso = peso;
    }
    
    public String getNombre() { return nombre; }
    public double getPeso() { return peso; }
    
    @Override
    public int compareTo(AtributoRanking otro) {
        return Double.compare(otro.peso, this.peso);
    }
}
