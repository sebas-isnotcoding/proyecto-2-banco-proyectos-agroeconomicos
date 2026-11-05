import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VentanaRiego extends JFrame {

    private final ModeloRiego modelo = new ModeloRiego();
    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(new String[]{"S1", "S2", "S3", "S4", "b (L/s)"}, 4);
    private final JTable tabla = new JTable(modeloTabla);
    private final JTextArea salida = new JTextArea(16, 60);

    private final JTextField txtTol = new JTextField("0.001", 6);
    private final JTextField txtIter = new JTextField("50", 6);
    private final JComboBox<Integer> cmbSector = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    private final JTextField txtFuga = new JTextField("2", 6);
    private final JTextField txtNota = new JTextField("Emisor roto", 10);
    private final JTextField txtHumedad = new JTextField("35", 6);
    private final JLabel lblTurno = new JLabel();

    public VentanaRiego() {
        super("Simulador de Riego por Goteo - Gauss-Seidel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        salida.setEditable(false);
        salida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tabla.setRowHeight(24);
        cargarTabla();
        actualizarTurno();

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(520, 125));
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                "Matriz A (pérdida de carga / fricción) y vector b (caudales objetivo)"));

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(scrollTabla, BorderLayout.NORTH);
        centro.add(new JScrollPane(salida), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(panelLateral(), BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel panelLateral() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel sol = grupo("Solución (vectores, lista simple, pila)");
        sol.add(new JLabel("Tolerancia:"));   sol.add(txtTol);
        sol.add(new JLabel("Iter. máx.:"));   sol.add(txtIter);
        sol.add(boton("Resolver", this::resolver));
        sol.add(boton("Deshacer (pila)", () -> log(modelo.deshacer())));

        JPanel fug = grupo("Fugas (lista doble)");
        fug.add(new JLabel("Sector:"));       fug.add(cmbSector);
        fug.add(new JLabel("Pérdida L/s:"));  fug.add(txtFuga);
        fug.add(new JLabel("Nota:"));         fug.add(txtNota);
        fug.add(boton("Registrar fuga", this::registrarFuga));
        fug.add(boton("Ver trazabilidad", () -> log(
                modelo.getFugas().mostrarAdelante() + modelo.getFugas().mostrarAtras()
                        + String.format("Pérdida total: %.2f L/s", modelo.getFugas().totalPerdida()))));

        JPanel hum = grupo("Humedad del suelo (cola)");
        hum.add(new JLabel("Humedad %:"));    hum.add(txtHumedad);
        hum.add(boton("Encolar lectura", this::encolarHumedad));
        hum.add(boton("Procesar cola", () -> log(modelo.procesarHumedad())));

        JPanel sec = grupo("Sectores (lista circular)");
        sec.add(lblTurno);
        sec.add(boton("Siguiente sector", () -> {
            modelo.getSectores().avanzar();
            actualizarTurno();
            log(modelo.getSectores().mostrar());
        }));

        p.add(sol); p.add(fug); p.add(hum); p.add(sec);
        return p;
    }

    private JPanel grupo(String titulo) {
        JPanel g = new JPanel(new GridLayout(0, 2, 4, 4));
        g.setBorder(BorderFactory.createTitledBorder(titulo));
        return g;
    }

    // Todos los botones pasan por aquí: así las validaciones se muestran en ventana
    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (NumberFormatException ex) {
                aviso("Dato numérico inválido. Revise los campos (use punto decimal).");
            } catch (RuntimeException ex) {
                aviso(ex.getMessage() == null ? "Error inesperado." : ex.getMessage());
            }
        });
        return b;
    }

    private void resolver() {
        leerTabla();
        double tol = Double.parseDouble(txtTol.getText().trim());
        int iter = Integer.parseInt(txtIter.getText().trim());
        log(modelo.resolver(tol, iter));
    }

    private void registrarFuga() {
        leerTabla();
        modelo.registrarFuga((Integer) cmbSector.getSelectedItem(),
                Double.parseDouble(txtFuga.getText().trim()), txtNota.getText());
        cargarTabla(); // refleja el nuevo caudal b del sector
        log("Fuga registrada. Caudal del sector descontado en la tabla.");
    }

    private void encolarHumedad() {
        modelo.registrarHumedad(Double.parseDouble(txtHumedad.getText().trim()));
        log("Lectura encolada. Pendientes en la cola: " + modelo.getLecturasPendientes());
    }

    private void leerTabla() {
        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        double[][] A = new double[4][4];
        double[] b = new double[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++)
                A[i][j] = Double.parseDouble(tabla.getValueAt(i, j).toString().trim());
            b[i] = Double.parseDouble(tabla.getValueAt(i, 4).toString().trim());
        }
        modelo.setSistema(A, b);
    }

    private void cargarTabla() {
        double[][] A = modelo.getMatriz();
        double[] b = modelo.getB();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) modeloTabla.setValueAt(A[i][j], i, j);
            modeloTabla.setValueAt(b[i], i, 4);
        }
    }

    private void actualizarTurno() {
        lblTurno.setText("Riega: " + modelo.getSectores().sectorActual());
    }

    private void log(String texto) {
        salida.append(texto + "\n\n");
        salida.setCaretPosition(salida.getDocument().getLength());
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}