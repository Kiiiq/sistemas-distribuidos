package comum;

import java.io.Serializable;

public class Jogada implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int noDestino;

    public Jogada(int noDestino) {
        this.noDestino = noDestino;
    }

    public int getNoDestino() {
        return noDestino;
    }
}