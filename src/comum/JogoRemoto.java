package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JogoRemoto extends Remote {
    int entrarNaPartida(String nomeJogador)
            throws RemoteException, PartidaCheiaException;

    int entrarSingleplayer(String nomeJogador, int idDesejado)
            throws RemoteException, PartidaCheiaException;

    ResultadoJogada realizarJogada(int idJogador, Jogada jogada)
            throws RemoteException, JogadaInvalidaException, ForaDoTurnoException;

    EstadoJogo obterEstado()
            throws RemoteException;

    EstadoJogo avancarRodada()
            throws RemoteException, JogadaInvalidaException;
}