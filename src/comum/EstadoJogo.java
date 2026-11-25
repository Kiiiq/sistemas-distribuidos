package comum;

import java.io.Serializable;
import java.util.List;

public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int numNos;
    private final List<int[]> arestas;
    private final boolean[] nosAtivos;
    private final int noPedra;
    private final int rodadaAtual;
    private final int totalRodadas;
    private final int turnoAtual; // 1 = Jogador 1, 2 = Jogador 2
    private final String nomeJogador1;
    private final String nomeJogador2;
    private final int placarJogador1;
    private final int placarJogador2;
    private final boolean partidaIniciada;
    private final boolean rodadaEncerrada;
    private final boolean jogoEncerrado;
    private final String mensagemStatus;

    public EstadoJogo(
            int numNos,
            List<int[]> arestas,
            boolean[] nosAtivos,
            int noPedra,
            int rodadaAtual,
            int totalRodadas,
            int turnoAtual,
            String nomeJogador1,
            String nomeJogador2,
            int placarJogador1,
            int placarJogador2,
            boolean partidaIniciada,
            boolean rodadaEncerrada,
            boolean jogoEncerrado,
            String mensagemStatus) {
        this.numNos = numNos;
        this.arestas = arestas;
        this.nosAtivos = nosAtivos;
        this.noPedra = noPedra;
        this.rodadaAtual = rodadaAtual;
        this.totalRodadas = totalRodadas;
        this.turnoAtual = turnoAtual;
        this.nomeJogador1 = nomeJogador1;
        this.nomeJogador2 = nomeJogador2;
        this.placarJogador1 = placarJogador1;
        this.placarJogador2 = placarJogador2;
        this.partidaIniciada = partidaIniciada;
        this.rodadaEncerrada = rodadaEncerrada;
        this.jogoEncerrado = jogoEncerrado;
        this.mensagemStatus = mensagemStatus;
    }

    public int getNumNos() { return numNos; }
    public List<int[]> getArestas() { return arestas; }
    public boolean[] getNosAtivos() { return nosAtivos; }
    public int getNoPedra() { return noPedra; }
    public int getRodadaAtual() { return rodadaAtual; }
    public int getTotalRodadas() { return totalRodadas; }
    public int getTurnoAtual() { return turnoAtual; }
    public String getNomeJogador1() { return nomeJogador1; }
    public String getNomeJogador2() { return nomeJogador2; }
    public int getPlacarJogador1() { return placarJogador1; }
    public int getPlacarJogador2() { return placarJogador2; }
    public boolean isPartidaIniciada() { return partidaIniciada; }
    public boolean isRodadaEncerrada() { return rodadaEncerrada; }
    public boolean isJogoEncerrado() { return jogoEncerrado; }
    public String getMensagemStatus() { return mensagemStatus; }
}