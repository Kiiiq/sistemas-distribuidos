package servidor;

import comum.JogoRemoto;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;

public class ServidorJogo {
    public static void main(String[] args) {
        try {
            String host = (args.length >= 1) ? args[0] : "127.0.0.1";
            int porta = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;

            System.setProperty("java.rmi.server.hostname", host);

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(porta);
                System.out.println("[RMI] Registry criado na porta " + porta);
            } catch (ExportException e) {
                registry = LocateRegistry.getRegistry(porta);
                System.out.println("[RMI] Conectado ao Registry existente na porta " + porta);
            }

            JogoRemoto servico = new JogoImpl();
            registry.rebind("JogoArvoreRMI", servico);

            System.out.println("[SERVIDOR] Pronto em rmi://" + host + ":" + porta + "/JogoArvoreRMI (Árvore aleatória de 30 nós)");
        } catch (Exception e) {
            System.err.println("[SERVIDOR] Erro ao iniciar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}