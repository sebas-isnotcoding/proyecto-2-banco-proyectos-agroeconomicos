// LISTA DOBLEMENTE ENLAZADA: trazabilidad de fugas (se puede recorrer en ambos sentidos)
public class ListaDoble {

    private static class Nodo {
        int sector;
        double perdida;
        String nota;
        Nodo anterior, siguiente; // dos enlaces

        Nodo(int sector, double perdida, String nota) {
            this.sector = sector;
            this.perdida = perdida;
            this.nota = nota;
        }
    }

    private Nodo primero, ultimo;
    private int tamano;

    public void agregar(int sector, double perdida, String nota) {
        Nodo nuevo = new Nodo(sector, perdida, nota);
        if (primero == null) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            nuevo.anterior = ultimo;
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
        tamano++;
    }

    public String mostrarAdelante() {
        if (primero == null) return "No hay fugas registradas.";
        StringBuilder sb = new StringBuilder("Fugas (más antigua -> más reciente):\n");
        for (Nodo n = primero; n != null; n = n.siguiente) sb.append(linea(n));
        return sb.toString();
    }

    public String mostrarAtras() {
        if (ultimo == null) return "No hay fugas registradas.";
        StringBuilder sb = new StringBuilder("Fugas (más reciente -> más antigua):\n");
        for (Nodo n = ultimo; n != null; n = n.anterior) sb.append(linea(n));
        return sb.toString();
    }

    private String linea(Nodo n) {
        return String.format("  Sector %d | %.2f L/s | %s%n", n.sector, n.perdida, n.nota);
    }

    public double totalPerdida() {
        double total = 0;
        for (Nodo n = primero; n != null; n = n.siguiente) total += n.perdida;
        return total;
    }

    public int getTamano() { return tamano; }
}