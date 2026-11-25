package servidor;

import comum.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class JogoImpl extends UnicastRemoteObject implements JogoRemoto {
    private static final long serialVersionUID = 1L;
    private static final int NUM_NOS = 30;
    private static final int TOTAL_RODADAS = 3;

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Random random = new Random();

    private List<int[]> arestas;
    private List<List<Integer>> adjacencia;
    private int[] nosIniciaisPorRodada;
    private boolean[] nosAtivos;

    private int noPedra;
    private int rodadaAtual;
    private int turnoAtual; // 1 = Jogador 1, 2 = Jogador 2

    private String nomeJogador1;
    private String nomeJogador2;
    private int placarJogador1;
    private int placarJogador2;

    private boolean modoSingleplayer;
    private int idBot; // 0 = multiplayer, 1 ou 2 = singleplayer

    private boolean partidaIniciada;
    private boolean rodadaEncerrada;
    private boolean jogoEncerrado;
    private String mensagemStatus;

    public JogoImpl() throws RemoteException {
        super();
        gerarArvoreAleatoria();
        resetarSessao();
    }

    private void gerarArvoreAleatoria() {
        arestas = new ArrayList<>(NUM_NOS - 1);
        adjacencia = new ArrayList<>(NUM_NOS + 1);
        for (int i = 0; i <= NUM_NOS; i++) {
            adjacencia.add(new ArrayList<>());
        }

        List<Integer> ordemNos = new ArrayList<>(NUM_NOS);
        for (int i = 1; i <= NUM_NOS; i++) {
            ordemNos.add(i);
        }
        Collections.shuffle(ordemNos, random);

        for (int i = 1; i < NUM_NOS; i++) {
            int u = ordemNos.get(random.nextInt(i));
            int v = ordemNos.get(i);
            arestas.add(new int[]{u, v});
            adjacencia.get(u).add(v);
            adjacencia.get(v).add(u);
        }

        Collections.shuffle(ordemNos, random);
        nosIniciaisPorRodada = new int[TOTAL_RODADAS];
        for (int r = 0; r < TOTAL_RODADAS; r++) {
            nosIniciaisPorRodada[r] = ordemNos.get(r);
        }
    }

    private void resetarSessao() {
        nosAtivos = new boolean[NUM_NOS + 1];
        rodadaAtual = 0;
        turnoAtual = 1;
        nomeJogador1 = null;
        nomeJogador2 = null;
        placarJogador1 = 0;
        placarJogador2 = 0;
        modoSingleplayer = false;
        idBot = 0;
        partidaIniciada = false;
        rodadaEncerrada = false;
        jogoEncerrado = false;
        mensagemStatus = "Aguardando jogadores (0/2 conectados)...";
    }

    private void iniciarRodada(int numeroRodada) {
        Arrays.fill(nosAtivos, false);
        rodadaAtual = numeroRodada;
        noPedra = nosIniciaisPorRodada[rodadaAtual - 1];
        nosAtivos[noPedra] = true;
        turnoAtual = 1;
        rodadaEncerrada = false;

        if (!possuiVizinhoInativo(noPedra)) {
            rodadaEncerrada = true;
            placarJogador2++;
            verificarFimDeJogo("Jogador 1 não possui movimentos válidos no nó inicial " + noPedra + ". Jogador 2 venceu a rodada!");
            return;
        }

        if (modoSingleplayer && idBot == 1) {
            executarTurnoBot("Início da rodada " + rodadaAtual + "/" + TOTAL_RODADAS + " no nó " + noPedra + ".");
        } else {
            mensagemStatus = "Rodada " + rodadaAtual + "/" + TOTAL_RODADAS + " — Vez do Jogador 1 (" + nomeJogador1 + ").";
        }
    }

    private boolean possuiVizinhoInativo(int u) {
        for (int v : adjacencia.get(u)) {
            if (!nosAtivos[v]) {
                return true;
            }
        }
        return false;
    }

    private void verificarFimDeJogo(String motivoRodada) {
        if (rodadaAtual >= TOTAL_RODADAS) {
            jogoEncerrado = true;
            String campeao;
            if (placarJogador1 > placarJogador2) {
                campeao = "Jogador 1 (" + nomeJogador1 + ") venceu a partida!";
            } else if (placarJogador2 > placarJogador1) {
                campeao = "Jogador 2 (" + nomeJogador2 + ") venceu a partida!";
            } else {
                campeao = "Empate no placar geral!";
            }
            mensagemStatus = motivoRodada + " | FIM DE JOGO: " + campeao;
        } else {
            mensagemStatus = motivoRodada + " Clique em 'Próxima Rodada'.";
        }
    }

    private void executarTurnoBot(String prefixoStatus) {
        int destinoBot = escolherMelhorJogadaBot();
        noPedra = destinoBot;
        nosAtivos[destinoBot] = true;

        String atorBot = "Bot (Jogador " + idBot + ")";
        System.out.println("[SERVIDOR] " + atorBot + " moveu a pedra para o nó " + destinoBot);

        if (!possuiVizinhoInativo(noPedra)) {
            rodadaEncerrada = true;
            if (idBot == 1) {
                placarJogador1++;
            } else {
                placarJogador2++;
            }
            int idHumano = (idBot == 1) ? 2 : 1;
            verificarFimDeJogo(prefixoStatus + " " + atorBot + " moveu para " + destinoBot
                    + ". Jogador " + idHumano + " ficou sem vizinhos inativos e perdeu a rodada!");
        } else {
            turnoAtual = (idBot == 1) ? 2 : 1;
            String nomeHumano = (turnoAtual == 1) ? nomeJogador1 : nomeJogador2;
            mensagemStatus = prefixoStatus + " " + atorBot + " moveu para o nó " + destinoBot
                    + ". Vez de Jogador " + turnoAtual + " (" + nomeHumano + ").";
        }
    }

    private int escolherMelhorJogadaBot() {
        List<Integer> vizinhosDisponiveis = new ArrayList<>();
        for (int v : adjacencia.get(noPedra)) {
            if (!nosAtivos[v]) {
                vizinhosDisponiveis.add(v);
            }
        }
        Collections.shuffle(vizinhosDisponiveis, random);

        int melhorNo = vizinhosDisponiveis.get(0);
        int melhorScore = Integer.MIN_VALUE;

        System.out.println("[BOT] Avaliando movimentos a partir do nó " + noPedra + ":");
        for (int v : vizinhosDisponiveis) {
            nosAtivos[v] = true;
            int resOponente = avaliarEstado(v);
            nosAtivos[v] = false;

            // Se resOponente <= 0, o oponente perde em |resOponente| lances -> vitória do bot em (1 - resOponente) lances.
            // Se resOponente > 0, o oponente vence em resOponente lances -> derrota do bot em (1 + resOponente) lances.
            int score;
            if (resOponente <= 0) {
                int lancesParaVencer = 1 - resOponente;
                score = 1000 - lancesParaVencer; // positivo: prefere vencer mais rápido
                System.out.println("  -> Nó " + v + ": VITÓRIA forçada em " + lancesParaVencer + " lance(s)");
            } else {
                int lancesParaPerder = 1 + resOponente;
                score = -1000 + lancesParaPerder; // negativo: prefere adiar a derrota ao máximo
                System.out.println("  -> Nó " + v + ": DERROTA contra jogo ótimo em " + lancesParaPerder + " lance(s)");
            }

            if (score > melhorScore) {
                melhorScore = score;
                melhorNo = v;
            }
        }

        System.out.println("[BOT] Escolha final: nó " + melhorNo + (melhorScore > 0 ? " (posição ganhadora)" : " (posição perdedora)"));
        return melhorNo;
    }

    /**
     * Retorna > 0 se o jogador atual vence em +k lances com jogo ótimo;
     * Retorna <= 0 se o jogador atual perde em -k lances contra jogo ótimo.
     */
    private int avaliarEstado(int u) {
        boolean temMovimento = false;
        int menorLancesVitoria = Integer.MAX_VALUE;
        int maiorLancesDerrota = 0;

        for (int v : adjacencia.get(u)) {
            if (!nosAtivos[v]) {
                temMovimento = true;
                nosAtivos[v] = true;
                int resOponente = avaliarEstado(v);
                nosAtivos[v] = false;

                if (resOponente <= 0) {
                    menorLancesVitoria = Math.min(menorLancesVitoria, 1 - resOponente);
                } else {
                    maiorLancesDerrota = Math.max(maiorLancesDerrota, 1 + resOponente);
                }
            }
        }

        if (!temMovimento) {
            return 0; // Sem vizinhos inativos: derrota imediata (0 lances)
        }
        if (menorLancesVitoria != Integer.MAX_VALUE) {
            return menorLancesVitoria;
        }
        return -maiorLancesDerrota;
    }

    @Override
    public int entrarNaPartida(String nomeJogador) throws RemoteException, PartidaCheiaException {
        lock.lock();
        try {
            if (modoSingleplayer) {
                throw new PartidaCheiaException("O servidor já está em uma partida Singleplayer.");
            }
            String nomeLimpo = (nomeJogador == null || nomeJogador.trim().isEmpty())
                    ? "Anônimo" : nomeJogador.trim();

            if (nomeJogador1 == null) {
                nomeJogador1 = nomeLimpo;
                mensagemStatus = "Jogador 1 (" + nomeJogador1 + ") entrou. Aguardando Jogador 2 (1/2)...";
                System.out.println("[SERVIDOR] Jogador 1 conectado: " + nomeJogador1);
                return 1;
            } else if (nomeJogador2 == null) {
                nomeJogador2 = nomeLimpo;
                partidaIniciada = true;
                System.out.println("[SERVIDOR] Jogador 2 conectado: " + nomeJogador2);
                iniciarRodada(1);
                return 2;
            } else {
                throw new PartidaCheiaException("A partida já possui 2 jogadores ativos (Jogador 1 e Jogador 2).");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int entrarSingleplayer(String nomeJogador, int idDesejado)
            throws RemoteException, PartidaCheiaException {
        lock.lock();
        try {
            if (nomeJogador1 != null || nomeJogador2 != null) {
                throw new PartidaCheiaException("Já existe uma partida em andamento no servidor.");
            }
            String nomeLimpo = (nomeJogador == null || nomeJogador.trim().isEmpty())
                    ? "Jogador" : nomeJogador.trim();

            modoSingleplayer = true;
            partidaIniciada = true;

            if (idDesejado == 2) {
                idBot = 1;
                nomeJogador1 = "Algoritmo Ótimo";
                nomeJogador2 = nomeLimpo;
                System.out.println("[SERVIDOR] Singleplayer iniciado: Bot (J1) vs " + nomeJogador2 + " (J2)");
                iniciarRodada(1);
                return 2;
            } else {
                idBot = 2;
                nomeJogador1 = nomeLimpo;
                nomeJogador2 = "Algoritmo Ótimo";
                System.out.println("[SERVIDOR] Singleplayer iniciado: " + nomeJogador1 + " (J1) vs Bot (J2)");
                iniciarRodada(1);
                return 1;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ResultadoJogada realizarJogada(int idJogador, Jogada jogada)
            throws RemoteException, JogadaInvalidaException, ForaDoTurnoException {
        lock.lock();
        try {
            if (!partidaIniciada) {
                throw new JogadaInvalidaException("A partida ainda não começou. Aguarde o segundo jogador.");
            }
            if (jogoEncerrado || rodadaEncerrada) {
                throw new JogadaInvalidaException("A rodada atual já foi encerrada.");
            }
            if (idJogador != turnoAtual) {
                throw new ForaDoTurnoException("Fora de turno! Aguarde a jogada do Jogador " + turnoAtual + ".");
            }
            if (jogada == null) {
                throw new JogadaInvalidaException("Jogada nula recebida.");
            }

            int destino = jogada.getNoDestino();
            if (destino < 1 || destino > NUM_NOS) {
                throw new JogadaInvalidaException("Nó " + destino + " não existe na árvore.");
            }
            if (!adjacencia.get(noPedra).contains(destino)) {
                throw new JogadaInvalidaException("O nó " + destino + " não é vizinho do nó atual (" + noPedra + ").");
            }
            if (nosAtivos[destino]) {
                throw new JogadaInvalidaException("O nó " + destino + " já está ativado!");
            }

            noPedra = destino;
            nosAtivos[destino] = true;

            String ator = "Jogador " + idJogador;
            System.out.println("[SERVIDOR] " + ator + " moveu a pedra para o nó " + destino);

            if (!possuiVizinhoInativo(noPedra)) {
                rodadaEncerrada = true;
                if (idJogador == 1) {
                    placarJogador1++;
                } else {
                    placarJogador2++;
                }
                int idOponente = (idJogador == 1) ? 2 : 1;
                verificarFimDeJogo(ator + " moveu para " + destino + ". Jogador " + idOponente + " não tem vizinhos inativos e perdeu a rodada!");
            } else {
                turnoAtual = (turnoAtual == 1) ? 2 : 1;
                if (modoSingleplayer && turnoAtual == idBot) {
                    executarTurnoBot(ator + " moveu para " + destino + ".");
                } else {
                    String proximoNome = (turnoAtual == 1)
                            ? "Jogador 1 (" + nomeJogador1 + ")"
                            : "Jogador 2 (" + nomeJogador2 + ")";
                    mensagemStatus = ator + " moveu para o nó " + destino + ". Vez de " + proximoNome + ".";
                }
            }

            return new ResultadoJogada(true, mensagemStatus, criarSnapshot());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EstadoJogo obterEstado() throws RemoteException {
        lock.lock();
        try {
            return criarSnapshot();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EstadoJogo avancarRodada() throws RemoteException, JogadaInvalidaException {
        lock.lock();
        try {
            if (!partidaIniciada) {
                throw new JogadaInvalidaException("A partida ainda não iniciou.");
            }
            if (jogoEncerrado) {
                placarJogador1 = 0;
                placarJogador2 = 0;
                jogoEncerrado = false;
                gerarArvoreAleatoria();
                iniciarRodada(1);
            } else if (rodadaEncerrada) {
                iniciarRodada(rodadaAtual + 1);
            } else {
                throw new JogadaInvalidaException("A rodada atual ainda está em andamento.");
            }
            return criarSnapshot();
        } finally {
            lock.unlock();
        }
    }

    private EstadoJogo criarSnapshot() {
        List<int[]> copiaArestas = new ArrayList<>(arestas.size());
        for (int[] e : arestas) {
            copiaArestas.add(new int[]{e[0], e[1]});
        }
        return new EstadoJogo(
                NUM_NOS,
                copiaArestas,
                Arrays.copyOf(nosAtivos, nosAtivos.length),
                noPedra,
                rodadaAtual,
                TOTAL_RODADAS,
                turnoAtual,
                nomeJogador1,
                nomeJogador2,
                placarJogador1,
                placarJogador2,
                partidaIniciada,
                rodadaEncerrada,
                jogoEncerrado,
                mensagemStatus
        );
    }
}