package cliente;

import comum.EstadoJogo;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;

public class PainelArvore extends JPanel {
    private EstadoJogo estado;
    private int meuIdJogador = -1;
    private int noHover = -1;
    private int hashArvoreAtual = 0;

    private final Map<Integer, Point2D.Double> posicoesNormalizadas = new HashMap<>();
    private final Map<Integer, Point2D.Double> posicoesTela = new HashMap<>();
    private final Set<Integer> vizinhosValidos = new HashSet<>();

    private static final int RAIO_NO = 18;

    // Paleta de cores (Slate / Emerald / Amber / Rose)
    private static final Color COR_FUNDO = new Color(15, 23, 42);
    private static final Color COR_GRADE = new Color(30, 41, 59, 120);
    private static final Color COR_ARESTA_PADRAO = new Color(71, 85, 105, 185);
    private static final Color COR_ARESTA_ATIVA = new Color(244, 63, 94, 145);
    private static final Color COR_ARESTA_VALIDA = new Color(16, 185, 129, 240);

    private static final Color COR_PEDRA_CENTRO = new Color(253, 224, 71);
    private static final Color COR_PEDRA_BORDA = new Color(217, 119, 6);

    private static final Color COR_VALIDO_CENTRO = new Color(52, 211, 153);
    private static final Color COR_VALIDO_BORDA = new Color(5, 150, 105);

    private static final Color COR_ATIVADO_CENTRO = new Color(225, 29, 72);
    private static final Color COR_ATIVADO_BORDA = new Color(136, 19, 55);

    private static final Color COR_INATIVO_CENTRO = new Color(51, 65, 85);
    private static final Color COR_INATIVO_BORDA = new Color(100, 116, 139);

    public PainelArvore(IntConsumer aoClicarNo) {
        setBackground(COR_FUNDO);
        setPreferredSize(new Dimension(1120, 650));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!podeInteragir()) return;
                int clicado = encontrarNoSobCursor(e.getPoint());
                if (clicado != -1) {
                    aoClicarNo.accept(clicado);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int novoHover = podeInteragir() ? encontrarNoSobCursor(e.getPoint()) : -1;
                if (novoHover != noHover) {
                    noHover = novoHover;
                    setCursor(vizinhosValidos.contains(noHover)
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private boolean podeInteragir() {
        return estado != null
                && estado.isPartidaIniciada()
                && !estado.isRodadaEncerrada()
                && !estado.isJogoEncerrado();
    }

    private int encontrarNoSobCursor(Point ponto) {
        for (Map.Entry<Integer, Point2D.Double> entry : posicoesTela.entrySet()) {
            if (entry.getValue().distance(ponto) <= RAIO_NO + 3) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public void atualizarEstado(EstadoJogo novoEstado, int idJogador) {
        this.estado = novoEstado;
        this.meuIdJogador = idJogador;

        if (estado != null) {
            int novoHash = calcularHashArestas(estado);
            if (novoHash != hashArvoreAtual) {
                hashArvoreAtual = novoHash;
                gerarLayoutForceDirected();
            }
        }

        recalcularVizinhosValidos();
        if (!vizinhosValidos.contains(noHover)) {
            noHover = -1;
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    private int calcularHashArestas(EstadoJogo est) {
        int h = est.getNumNos();
        for (int[] e : est.getArestas()) {
            h = 31 * h + Math.min(e[0], e[1]);
            h = 31 * h + Math.max(e[0], e[1]);
        }
        return h;
    }

    private void recalcularVizinhosValidos() {
        vizinhosValidos.clear();
        if (!podeInteragir() || estado.getTurnoAtual() != meuIdJogador) {
            return;
        }

        int pedra = estado.getNoPedra();
        boolean[] ativos = estado.getNosAtivos();
        for (int[] aresta : estado.getArestas()) {
            int u = aresta[0];
            int v = aresta[1];
            if (u == pedra && !ativos[v]) {
                vizinhosValidos.add(v);
            } else if (v == pedra && !ativos[u]) {
                vizinhosValidos.add(u);
            }
        }
    }

    private void gerarLayoutForceDirected() {
        posicoesNormalizadas.clear();
        int n = estado.getNumNos();
        if (n == 0) return;

        // Simulação em espaço 1.65 x 1.0 para acompanhar a proporção widescreen da janela
        double larguraSim = 1.65;
        double alturaSim = 1.00;
        double area = larguraSim * alturaSim;
        double k = 0.88 * Math.sqrt(area / n);

        double[] x = new double[n + 1];
        double[] y = new double[n + 1];
        Random rng = new Random(hashArvoreAtual);

        // Posição inicial distribuída em espiral suave para evitar mínimos locais
        for (int i = 1; i <= n; i++) {
            double angulo = i * 2.399963229728653; // ângulo áureo
            double raio = 0.42 * Math.sqrt((double) i / n);
            x[i] = (larguraSim / 2.0) + raio * larguraSim * Math.cos(angulo) + (rng.nextDouble() - 0.5) * 0.04;
            y[i] = (alturaSim / 2.0) + raio * alturaSim * Math.sin(angulo) + (rng.nextDouble() - 0.5) * 0.04;
        }

        List<int[]> arestas = estado.getArestas();
        double[] dispX = new double[n + 1];
        double[] dispY = new double[n + 1];

        int iteracoes = 650;
        double tempInicial = 0.25;

        for (int it = 0; it < iteracoes; it++) {
            Arrays.fill(dispX, 0.0);
            Arrays.fill(dispY, 0.0);

            // 1. Repulsão entre todos os pares de nós
            for (int u = 1; u <= n; u++) {
                for (int v = u + 1; v <= n; v++) {
                    double dx = x[u] - x[v];
                    double dy = y[u] - y[v];
                    double dist = Math.hypot(dx, dy);
                    if (dist < 0.0001) {
                        dx = 0.001;
                        dy = 0.001;
                        dist = 0.0014;
                    }
                    double forcaRep = (k * k) / dist;
                    double fx = (dx / dist) * forcaRep;
                    double fy = (dy / dist) * forcaRep;
                    dispX[u] += fx;
                    dispY[u] += fy;
                    dispX[v] -= fx;
                    dispY[v] -= fy;
                }
            }

            // 2. Atração ao longo das arestas da árvore
            for (int[] e : arestas) {
                int u = e[0];
                int v = e[1];
                double dx = x[u] - x[v];
                double dy = y[u] - y[v];
                double dist = Math.max(0.0001, Math.hypot(dx, dy));
                double forcaAtr = (dist * dist) / k;
                double fx = (dx / dist) * forcaAtr;
                double fy = (dy / dist) * forcaAtr;
                dispX[u] -= fx;
                dispY[u] -= fy;
                dispX[v] += fx;
                dispY[v] += fy;
            }

            // 3. Gravidade suave para manter o grafo centrado e compacto
            for (int i = 1; i <= n; i++) {
                dispX[i] += ((larguraSim / 2.0) - x[i]) * 0.08;
                dispY[i] += ((alturaSim / 2.0) - y[i]) * 0.08;
            }

            // 4. Aplica deslocamento com resfriamento gradual
            double progresso = 1.0 - ((double) it / iteracoes);
            double temp = Math.max(0.0015, tempInicial * progresso * progresso);

            for (int i = 1; i <= n; i++) {
                double len = Math.hypot(dispX[i], dispY[i]);
                if (len > 0.0) {
                    double passo = Math.min(len, temp);
                    x[i] += (dispX[i] / len) * passo;
                    y[i] += (dispY[i] / len) * passo;
                }
            }
        }

        // Normaliza para [0.0, 1.0] preservando toda a extensão da tela
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 1; i <= n; i++) {
            minX = Math.min(minX, x[i]);
            maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]);
            maxY = Math.max(maxY, y[i]);
        }

        double spanX = Math.max(0.0001, maxX - minX);
        double spanY = Math.max(0.0001, maxY - minY);

        for (int i = 1; i <= n; i++) {
            double nx = (x[i] - minX) / spanX;
            double ny = (y[i] - minY) / spanY;
            posicoesNormalizadas.put(i, new Point2D.Double(nx, ny));
        }
    }

    private void projetarParaTela() {
        posicoesTela.clear();
        double margemX = 56.0;
        double margemTopo = 46.0;
        double margemBase = 76.0;

        double larguraUtil = Math.max(120.0, getWidth() - 2 * margemX);
        double alturaUtil = Math.max(120.0, getHeight() - margemTopo - margemBase);

        for (Map.Entry<Integer, Point2D.Double> entry : posicoesNormalizadas.entrySet()) {
            Point2D.Double norm = entry.getValue();
            double px = margemX + norm.x * larguraUtil;
            double py = margemTopo + norm.y * alturaUtil;
            posicoesTela.put(entry.getKey(), new Point2D.Double(px, py));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        desenharGradePonteada(g2);

        if (estado == null) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String msg = "Conecte-se ao servidor RMI para visualizar a árvore de 30 nós.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }

        projetarParaTela();
        desenharArestas(g2);
        desenharNos(g2);
        desenharLegendaFlutuante(g2);
    }

    private void desenharGradePonteada(Graphics2D g2) {
        g2.setColor(COR_GRADE);
        int espaco = 32;
        for (int x = espaco; x < getWidth(); x += espaco) {
            for (int y = espaco; y < getHeight(); y += espaco) {
                g2.fillOval(x, y, 2, 2);
            }
        }
    }

    private void desenharArestas(Graphics2D g2) {
        boolean[] ativos = estado.getNosAtivos();
        int noPedra = estado.getNoPedra();

        for (int[] aresta : estado.getArestas()) {
            int u = aresta[0];
            int v = aresta[1];
            Point2D.Double p1 = posicoesTela.get(u);
            Point2D.Double p2 = posicoesTela.get(v);
            if (p1 == null || p2 == null) continue;

            boolean ehJogadaValida = (u == noPedra && vizinhosValidos.contains(v))
                    || (v == noPedra && vizinhosValidos.contains(u));
            boolean ambosAtivos = ativos[u] && ativos[v];

            if (ehJogadaValida) {
                g2.setColor(COR_ARESTA_VALIDA);
                g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else if (ambosAtivos) {
                g2.setColor(COR_ARESTA_ATIVA);
                g2.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g2.setColor(COR_ARESTA_PADRAO);
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }

            g2.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                    (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
    }

    private void desenharNos(Graphics2D g2) {
        boolean[] ativos = estado.getNosAtivos();
        int noPedra = estado.getNoPedra();
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));

        for (int u = 1; u <= estado.getNumNos(); u++) {
            Point2D.Double p = posicoesTela.get(u);
            if (p == null) continue;

            boolean ehPedra = (u == noPedra && estado.isPartidaIniciada());
            boolean ehValido = vizinhosValidos.contains(u);
            boolean ehHover = (u == noHover && ehValido);
            int raio = ehHover ? RAIO_NO + 3 : RAIO_NO;

            // Halo externo
            if (ehPedra) {
                g2.setColor(new Color(250, 204, 21, 75));
                g2.fillOval((int) p.x - raio - 8, (int) p.y - raio - 8, (raio + 8) * 2, (raio + 8) * 2);
            } else if (ehValido) {
                g2.setColor(new Color(16, 185, 129, ehHover ? 110 : 55));
                g2.fillOval((int) p.x - raio - 6, (int) p.y - raio - 6, (raio + 6) * 2, (raio + 6) * 2);
            }

            // Sombra projetada
            g2.setColor(new Color(0, 0, 0, 125));
            g2.fillOval((int) p.x - raio + 2, (int) p.y - raio + 3, raio * 2, raio * 2);

            // Gradiente interno do nó
            Color corCentro;
            Color corBorda;
            if (ehPedra) {
                corCentro = COR_PEDRA_CENTRO;
                corBorda = COR_PEDRA_BORDA;
            } else if (ehValido) {
                corCentro = COR_VALIDO_CENTRO;
                corBorda = COR_VALIDO_BORDA;
            } else if (ativos[u]) {
                corCentro = COR_ATIVADO_CENTRO;
                corBorda = COR_ATIVADO_BORDA;
            } else {
                corCentro = COR_INATIVO_CENTRO;
                corBorda = COR_INATIVO_BORDA;
            }

            RadialGradientPaint gradiente = new RadialGradientPaint(
                    new Point2D.Double(p.x - raio * 0.3, p.y - raio * 0.3),
                    raio * 1.4f,
                    new float[]{0.0f, 1.0f},
                    new Color[]{corCentro, corBorda}
            );
            g2.setPaint(gradiente);
            g2.fillOval((int) p.x - raio, (int) p.y - raio, raio * 2, raio * 2);

            // Contorno do nó
            g2.setColor(ehPedra || ehHover ? Color.WHITE : new Color(226, 232, 240, 210));
            g2.setStroke(new BasicStroke(ehPedra || ehHover ? 2.3f : 1.6f));
            g2.drawOval((int) p.x - raio, (int) p.y - raio, raio * 2, raio * 2);

            // Número do nó centralizado
            String label = String.valueOf(u);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(ehPedra ? new Color(15, 23, 42) : Color.WHITE);
            g2.drawString(label,
                    (int) Math.round(p.x - fm.stringWidth(label) / 2.0),
                    (int) Math.round(p.y + fm.getAscent() / 2.0 - 2));
        }
    }

    private void desenharLegendaFlutuante(Graphics2D g2) {
        int xCaixa = 18;
        int yCaixa = getHeight() - 42;
        int larguraCaixa = 545;
        int alturaCaixa = 30;

        g2.setColor(new Color(30, 41, 59, 225));
        g2.fill(new RoundRectangle2D.Double(xCaixa, yCaixa, larguraCaixa, alturaCaixa, 14, 14));
        g2.setColor(new Color(71, 85, 105, 185));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(xCaixa, yCaixa, larguraCaixa, alturaCaixa, 14, 14));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int yTexto = yCaixa + 19;
        desenharItemLegenda(g2, xCaixa + 14, yTexto, COR_PEDRA_CENTRO, "Pedra Atual");
        desenharItemLegenda(g2, xCaixa + 125, yTexto, COR_VALIDO_CENTRO, "Jogada Válida");
        desenharItemLegenda(g2, xCaixa + 245, yTexto, COR_ATIVADO_CENTRO, "Nó Ativado");
        desenharItemLegenda(g2, xCaixa + 355, yTexto, COR_INATIVO_BORDA, "Nó Inativo");
    }

    private void desenharItemLegenda(Graphics2D g2, int x, int y, Color cor, String texto) {
        g2.setColor(cor);
        g2.fillOval(x, y - 10, 11, 11);
        g2.setColor(new Color(226, 232, 240));
        g2.drawString(texto, x + 16, y);
    }
}