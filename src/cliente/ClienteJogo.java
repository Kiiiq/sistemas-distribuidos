package cliente;

import comum.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.*;
import javax.swing.border.Border;

public class ClienteJogo extends JFrame {
    private JogoRemoto stubServidor;
    private int idJogador = -1;

    // Cores da interface
    private static final Color BG_PRINCIPAL = new Color(15, 23, 42);
    private static final Color BG_BARRA = new Color(30, 41, 59);
    private static final Color BG_HUD = new Color(22, 32, 51);
    private static final Color BG_INPUT = new Color(15, 23, 42);
    private static final Color BORDA_SUTIL = new Color(51, 65, 85);
    private static final Color TEXTO_PRIMARIO = new Color(248, 250, 252);
    private static final Color TEXTO_SECUNDARIO = new Color(148, 163, 184);

    private static final Color COR_ESMERALDA = new Color(16, 185, 129);
    private static final Color COR_ESMERALDA_HOVER = new Color(5, 150, 105);
    private static final Color COR_AMBAR = new Color(245, 158, 11);
    private static final Color COR_AMBAR_HOVER = new Color(217, 119, 6);
    private static final Color COR_CIANO = new Color(56, 189, 248);
    private static final Color COR_ROSA = new Color(244, 63, 94);

    private final JTextField campoHost;
    private final JTextField campoPorta;
    private final JTextField campoNome;
    private final JComboBox<String> comboModo;
    private final BotaoEstilizado btnConectar;
    private final BotaoEstilizado btnProximaRodada;

    private final JLabel lblPapel = new JLabel("Desconectado");
    private final JLabel lblRodada = new JLabel("Rodada -/-");
    private final JLabel lblPlacar = new JLabel("Jogador 1: 0   ×   Jogador 2: 0");
    private final JLabel lblStatus = new JLabel("Escolha o modo, informe o IP/porta do servidor e clique em Conectar.");
    private final IndicadorStatus indicadorStatus = new IndicadorStatus();

    private final PainelArvore painelArvore;
    private Timer timerPolling;

    public ClienteJogo(String hostInicial, String portaInicial) {
        super("Jogo da Árvore — Java RMI");
        getContentPane().setBackground(BG_PRINCIPAL);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        campoHost = criarCampoTexto(hostInicial, 9);
        campoPorta = criarCampoTexto(portaInicial, 4);
        campoNome = criarCampoTexto("Player", 9);
        comboModo = criarComboModo();

        btnConectar = new BotaoEstilizado("Conectar", COR_ESMERALDA, COR_ESMERALDA_HOVER, BG_PRINCIPAL);
        btnProximaRodada = new BotaoEstilizado("Próxima Rodada", COR_AMBAR, COR_AMBAR_HOVER, BG_PRINCIPAL);
        btnProximaRodada.setEnabled(false);

        // 1. Barra superior de conexão e modo
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        painelTopo.setBackground(BG_BARRA);
        painelTopo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA_SUTIL));

        painelTopo.add(criarLabelBarra("Host"));
        painelTopo.add(campoHost);
        painelTopo.add(criarLabelBarra("Porta"));
        painelTopo.add(campoPorta);
        painelTopo.add(criarLabelBarra("Nome"));
        painelTopo.add(campoNome);
        painelTopo.add(criarLabelBarra("Modo"));
        painelTopo.add(comboModo);
        painelTopo.add(Box.createHorizontalStrut(4));
        painelTopo.add(btnConectar);
        painelTopo.add(btnProximaRodada);

        // 2. Barra HUD de papel, rodada e placar
        JPanel painelHud = new JPanel(new BorderLayout(12, 0));
        painelHud.setBackground(BG_HUD);
        painelHud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA_SUTIL),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        lblPapel.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPapel.setForeground(TEXTO_SECUNDARIO);

        lblRodada.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRodada.setForeground(COR_CIANO);
        lblRodada.setHorizontalAlignment(SwingConstants.CENTER);

        lblPlacar.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPlacar.setForeground(TEXTO_PRIMARIO);
        lblPlacar.setHorizontalAlignment(SwingConstants.RIGHT);

        painelHud.add(lblPapel, BorderLayout.WEST);
        painelHud.add(lblRodada, BorderLayout.CENTER);
        painelHud.add(lblPlacar, BorderLayout.EAST);

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(BG_BARRA);
        cabecalho.add(painelTopo, BorderLayout.NORTH);
        cabecalho.add(painelHud, BorderLayout.SOUTH);
        add(cabecalho, BorderLayout.NORTH);

        // 3. Centro: Árvore radial
        painelArvore = new PainelArvore(this::enviarJogada);
        add(painelArvore, BorderLayout.CENTER);

        // 4. Rodapé de status
        JPanel painelRodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        painelRodape.setBackground(BG_BARRA);
        painelRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDA_SUTIL),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));

        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblStatus.setForeground(TEXTO_PRIMARIO);
        indicadorStatus.setCor(TEXTO_SECUNDARIO);

        painelRodape.add(indicadorStatus);
        painelRodape.add(lblStatus);
        add(painelRodape, BorderLayout.SOUTH);

        btnConectar.addActionListener(e -> conectarAoServidor());
        btnProximaRodada.addActionListener(e -> solicitarProximaRodada());

        pack();
        setLocationRelativeTo(null);
    }

    private JLabel criarLabelBarra(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXTO_SECUNDARIO);
        return lbl;
    }

    private JTextField criarCampoTexto(String textoInicial, int colunas) {
        JTextField campo = new JTextField(textoInicial, colunas);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        campo.setBackground(BG_INPUT);
        campo.setForeground(TEXTO_PRIMARIO);
        campo.setCaretColor(TEXTO_PRIMARIO);
        campo.setDisabledTextColor(TEXTO_SECUNDARIO);
        Border borda = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA_SUTIL, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        );
        campo.setBorder(borda);
        return campo;
    }

    private JComboBox<String> criarComboModo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{
                "Multiplayer (2 Jogadores)",
                "Singleplayer (Jogador 1)",
                "Singleplayer (Jogador 2)"
        });
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXTO_PRIMARIO);
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createLineBorder(BORDA_SUTIL, 1, true));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    lbl.setBackground(COR_ESMERALDA_HOVER);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(BG_INPUT);
                    lbl.setForeground(TEXTO_PRIMARIO);
                }
                return lbl;
            }
        });
        return combo;
    }

    private void conectarAoServidor() {
        try {
            String host = campoHost.getText().trim();
            int porta = Integer.parseInt(campoPorta.getText().trim());
            String nome = campoNome.getText().trim();
            int modoSelecionado = comboModo.getSelectedIndex();

            Registry registry = LocateRegistry.getRegistry(host, porta);
            stubServidor = (JogoRemoto) registry.lookup("JogoArvoreRMI");

            if (modoSelecionado == 1) {
                idJogador = stubServidor.entrarSingleplayer(nome, 1);
            } else if (modoSelecionado == 2) {
                idJogador = stubServidor.entrarSingleplayer(nome, 2);
            } else {
                idJogador = stubServidor.entrarNaPartida(nome);
            }

            if (idJogador == 1) {
                lblPapel.setText("● Você é: Jogador 1 (Inicia)");
                lblPapel.setForeground(COR_AMBAR);
            } else {
                lblPapel.setText("● Você é: Jogador 2");
                lblPapel.setForeground(COR_CIANO);
            }

            btnConectar.setEnabled(false);
            campoHost.setEnabled(false);
            campoPorta.setEnabled(false);
            campoNome.setEnabled(false);
            comboModo.setEnabled(false);

            atualizarTela(stubServidor.obterEstado());
            iniciarPolling();
        } catch (PartidaCheiaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sala Indisponível", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Falha ao conectar via RMI: " + ex.getMessage(),
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciarPolling() {
        timerPolling = new Timer(300, e -> {
            if (stubServidor == null) return;
            try {
                EstadoJogo estado = stubServidor.obterEstado();
                atualizarTela(estado);
            } catch (RemoteException ex) {
                timerPolling.stop();
                indicadorStatus.setCor(COR_ROSA);
                lblStatus.setText("Conexão perdida com o servidor RMI.");
            }
        });
        timerPolling.start();
    }

    private void enviarJogada(int noDestino) {
        if (stubServidor == null || idJogador == -1) return;
        try {
            ResultadoJogada resultado = stubServidor.realizarJogada(idJogador, new Jogada(noDestino));
            atualizarTela(resultado.getEstadoAtualizado());
        } catch (ForaDoTurnoException | JogadaInvalidaException ex) {
            indicadorStatus.setCor(COR_AMBAR);
            lblStatus.setText("Aviso: " + ex.getMessage());
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, "Erro de comunicação remota: " + ex.getMessage(),
                    "Erro RMI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solicitarProximaRodada() {
        if (stubServidor == null) return;
        try {
            EstadoJogo estado = stubServidor.avancarRodada();
            atualizarTela(estado);
        } catch (JogadaInvalidaException ex) {
            indicadorStatus.setCor(COR_AMBAR);
            lblStatus.setText("Aviso: " + ex.getMessage());
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, "Erro de comunicação remota: " + ex.getMessage(),
                    "Erro RMI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarTela(EstadoJogo estado) {
        if (estado == null) return;
        String j1 = (estado.getNomeJogador1() != null) ? estado.getNomeJogador1() : "Aguardando...";
        String j2 = (estado.getNomeJogador2() != null) ? estado.getNomeJogador2() : "Aguardando...";

        lblRodada.setText("RODADA " + estado.getRodadaAtual() + " DE " + estado.getTotalRodadas());
        lblPlacar.setText("Jogador 1 (" + j1 + "): " + estado.getPlacarJogador1()
                + "   ×   Jogador 2 (" + j2 + "): " + estado.getPlacarJogador2());
        lblStatus.setText(estado.getMensagemStatus());

        if (estado.isJogoEncerrado() || estado.isRodadaEncerrada()) {
            indicadorStatus.setCor(COR_AMBAR);
        } else if (estado.isPartidaIniciada() && estado.getTurnoAtual() == idJogador) {
            indicadorStatus.setCor(COR_ESMERALDA);
        } else {
            indicadorStatus.setCor(COR_CIANO);
        }

        if (estado.isJogoEncerrado()) {
            btnProximaRodada.setText("Reiniciar Partida");
            btnProximaRodada.setEnabled(true);
        } else if (estado.isRodadaEncerrada()) {
            btnProximaRodada.setText("Próxima Rodada");
            btnProximaRodada.setEnabled(true);
        } else {
            btnProximaRodada.setEnabled(false);
        }

        painelArvore.atualizarEstado(estado, idJogador);
    }

    // Botão arredondado customizado para o tema escuro
    private static class BotaoEstilizado extends JButton {
        private final Color corNormal;
        private final Color corHover;
        private final Color corTextoAtivo;
        private boolean hover = false;

        public BotaoEstilizado(String texto, Color corNormal, Color corHover, Color corTextoAtivo) {
            super(texto);
            this.corNormal = corNormal;
            this.corHover = corHover;
            this.corTextoAtivo = corTextoAtivo;

            setFont(new Font("SansSerif", Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(7, 15, 7, 15));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(51, 65, 85));
            } else if (hover) {
                g2.setColor(corHover);
            } else {
                g2.setColor(corNormal);
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            g2.setColor(isEnabled() ? corTextoAtivo : new Color(148, 163, 184));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // Indicador circular luminoso na barra de status
    private static class IndicadorStatus extends JComponent {
        private Color cor = Color.GRAY;

        public IndicadorStatus() {
            setPreferredSize(new Dimension(12, 12));
        }

        public void setCor(Color novaCor) {
            this.cor = novaCor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillOval(1, 1, 10, 10);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        String host = (args.length >= 1) ? args[0] : "127.0.0.1";
        String porta = (args.length >= 2) ? args[1] : "1099";
        SwingUtilities.invokeLater(() -> new ClienteJogo(host, porta).setVisible(true));
    }
}