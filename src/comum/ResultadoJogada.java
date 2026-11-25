package comum;

import java.io.Serializable;

public class ResultadoJogada implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean sucesso;
    private final String mensagem;
    private final EstadoJogo estadoAtualizado;

    public ResultadoJogada(boolean sucesso, String mensagem, EstadoJogo estadoAtualizado) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.estadoAtualizado = estadoAtualizado;
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
    public EstadoJogo getEstadoAtualizado() { return estadoAtualizado; }
}