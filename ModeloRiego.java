import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

// Reúne TODAS las estructuras obligatorias y la lógica del problema
public class ModeloRiego {

    // ARREGLO BIDIMENSIONAL: coeficientes de pérdida de carga y fricción entre sectores
    private double[][] A = {
            {10, -2, -1, 0},
            {-2, 12, -3, -1},
            {-1, -3, 11, -2},
            {0, -1, -2, 9}
    };

    // ARREGLOS UNIDIMENSIONALES
    private double[] b = {20, 30, 25, 18};                       // caudales objetivo (L/s)
    private double[] presiones = new double[4];                  // presiones calculadas (m.c.a.)
    private final double[] presionObjetivo = {3.5, 4.5, 4.5, 3.5};

    // LISTAS ENLAZADAS (simple, doble y circular)
    private final ListaSimple aproximaciones = new ListaSimple();
    private final ListaDoble fugas = new ListaDoble();
    private final ListaCircular sectores = new ListaCircular();

    // PILA (LIFO): estados previos de presión
    private final Deque<double[]> estadosPrevios = new ArrayDeque<>();

    // COLA (FIFO): lecturas de humedad del suelo por validar
    private final Queue<Double> lecturasHumedad = new LinkedList<>();

    public ModeloRiego() {
        for (int i = 1; i <= 4; i++) sectores.agregar("Sector " + i);
    }

    public void setSistema(double[][] nuevaA, double[] nuevoB) {
        A = nuevaA;
        b = nuevoB;
    }

    public String resolver(double tol, int maxIter) {
        if (tol <= 0 || maxIter <= 0)
            throw new IllegalArgumentException("Tolerancia e iteraciones deben ser positivas.");
        if (!GaussSeidel.esDiagonalDominante(A))
            throw new IllegalArgumentException(
                    "La matriz no es diagonalmente dominante: Gauss-Seidel podría no converger.");

        estadosPrevios.push(presiones.clone()); // guarda el estado ANTES de sobrescribirlo
        presiones = GaussSeidel.resolver(A, b, tol, maxIter, aproximaciones);

        StringBuilder sb = new StringBuilder("=== Resultado Gauss-Seidel ===\n");
        sb.append(aproximaciones.mostrar());
        sb.append("Iteraciones: ").append(aproximaciones.getTamano()).append("\n");
        if (aproximaciones.getUltimoError() >= tol)
            sb.append("AVISO: no se alcanzó la tolerancia con las iteraciones permitidas.\n");
        for (int i = 0; i < presiones.length; i++) {
            double desv = presiones[i] - presionObjetivo[i];
            sb.append(String.format("Sector %d: presión %.4f m.c.a. | objetivo %.2f | desviación %+.4f%n",
                    i + 1, presiones[i], presionObjetivo[i], desv));
        }
        return sb.toString();
    }

    public String deshacer() {
        if (estadosPrevios.isEmpty()) return "No hay estados previos para deshacer.";
        presiones = estadosPrevios.pop();
        return "Estado restaurado: " + Arrays.toString(presiones)
                + " (quedan " + estadosPrevios.size() + " estados guardados)";
    }

    public void registrarFuga(int sector, double perdida, String nota) {
        if (sector < 1 || sector > b.length)
            throw new IllegalArgumentException("Sector inválido.");
        if (perdida <= 0 || perdida >= b[sector - 1])
            throw new IllegalArgumentException("La pérdida debe ser positiva y menor al caudal del sector.");
        if (nota == null || nota.trim().isEmpty())
            throw new IllegalArgumentException("Escriba una nota para la fuga.");
        b[sector - 1] -= perdida; // la fuga reduce el caudal que llega al sector
        fugas.agregar(sector, perdida, nota.trim());
    }

    public void registrarHumedad(double humedad) {
        if (humedad < 0 || humedad > 100)
            throw new IllegalArgumentException("La humedad debe estar entre 0 y 100 %.");
        lecturasHumedad.offer(humedad);
    }

    public String procesarHumedad() {
        if (lecturasHumedad.isEmpty()) return "No hay lecturas de humedad en la cola.";
        StringBuilder sb = new StringBuilder("=== Validación de humedad (FIFO) ===\n");
        int n = 1;
        while (!lecturasHumedad.isEmpty()) {
            double h = lecturasHumedad.poll();
            String estado = h < 40 ? "SECO: requiere riego"
                    : (h > 80 ? "SATURADO: suspender riego" : "ÓPTIMO");
            sb.append(String.format("Lectura %d: %.1f %% -> %s%n", n++, h, estado));
        }
        return sb.toString();
    }

    public double[][] getMatriz() { return A; }
    public double[] getB() { return b; }
    public ListaDoble getFugas() { return fugas; }
    public ListaCircular getSectores() { return sectores; }
    public int getEstadosGuardados() { return estadosPrevios.size(); }
    public int getLecturasPendientes() { return lecturasHumedad.size(); }
}