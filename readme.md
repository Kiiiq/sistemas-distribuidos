# Jogo da Árvore — Multiplayer & Singleplayer (Java RMI)

Jogo de estratégia em turnos disputado sobre uma árvore gerada aleatoriamente com **30 nós** (em partidas de **3 rodadas**), desenvolvido em Java utilizando **Java RMI** (*Remote Method Invocation*) para comunicação remota, sincronização com `ReentrantLock` no servidor e interface gráfica em Java Swing.

A cada rodada, uma pedra inicia em um nó sorteado (ativando-o). Os jogadores se alternam movendo a pedra para um nó vizinho ainda inativo. Vence a rodada quem deixar o adversário sem movimentos válidos.

---

## Estrutura do Projeto

```text
projeto-jogo-arvore-rmi/
├── bin/                  # Classes compiladas (.class)
└── src/
    ├── comum/            # Interface remota (JogoRemoto), DTOs e exceções
    ├── servidor/         # Implementação do jogo, IA ótima e RMI Registry
    └── cliente/          # Interface gráfica Swing e cliente RMI
```

---

## Pré-requisitos

* **JDK 17 ou superior** (recomendado JDK 21) instalado e configurado na variável de ambiente `PATH` (`javac` e `java` funcionando no terminal).
* Terminal **Bash** (Git Bash no Windows, Linux ou macOS).

---

## 1. Compilação

Na pasta raiz do projeto (onde está localizado o diretório `src`), compile todos os pacotes:

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/comum/*.java src/servidor/*.java src/cliente/*.java
```

---

## 2. Como Rodar na Mesma Máquina (`localhost`)

Para testar localmente, utilize terminais separados na raiz do projeto:

### Terminal 1 — Iniciar o Servidor RMI
```bash
java -cp bin servidor.ServidorJogo 127.0.0.1 1099
```
O servidor inicializa automaticamente o `RMI Registry` na porta `1099` e publica o serviço `JogoArvoreRMI`.

### Terminal 2 — Iniciar o Cliente (Jogador 1)
```bash
java -cp bin cliente.ClienteJogo 127.0.0.1 1099
```
Na janela aberta:
1. Mantenha **Host** como `127.0.0.1` e **Porta** como `1099`.
2. Escolha entre **Multiplayer (2 Jogadores)** ou **Singleplayer (Jogador 1 / Jogador 2)**.
3. Clique em **Conectar**.

### Terminal 3 — Iniciar o Cliente (Jogador 2 — apenas no modo Multiplayer)
```bash
java -cp bin cliente.ClienteJogo 127.0.0.1 1099
```
Clique em **Conectar** para entrar como Jogador 2 e iniciar a partida.

> **Atalho para abrir os dois clientes a partir de um único terminal:**
> ```bash
> java -cp bin cliente.ClienteJogo 127.0.0.1 1099 &
> java -cp bin cliente.ClienteJogo 127.0.0.1 1099 &
> ```

---

## 3. Como Rodar em Máquinas Diferentes na Rede Local (LAN)

Para jogar entre dois computadores conectados na mesma rede Wi-Fi ou cabo:

### Passo 1 — Descobrir o IP local da Máquina Servidora (Máquina 1)
* **Windows (Git Bash / CMD):**
  ```bash
  ipconfig
  ```
  Anote o **Endereço IPv4** do adaptador Wi-Fi ou Ethernet ativo (exemplo: `192.168.1.15`).
* **Linux / macOS:**
  ```bash
  ip a
  ```
  Anote o endereço `inet` da interface de rede local (exemplo: `192.168.1.15`).

### Passo 2 — Liberar o Java no Firewall da Máquina 1
O Java RMI utiliza a porta `1099` para o registro de nomes e portas TCP dinâmicas para o objeto remoto. No **Windows Defender Firewall** (ou `ufw` no Linux), permita o acesso do `java.exe` / `javaw.exe` em **Redes Privadas** para evitar bloqueios de conexão.

### Passo 3 — Iniciar o Servidor na Máquina 1
Substitua `192.168.1.15` pelo IPv4 real descoberto no Passo 1 (esse parâmetro configura `java.rmi.server.hostname` para que os clientes remotos consigam invocar o objeto na rede):

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/comum/*.java src/servidor/*.java src/cliente/*.java
java -cp bin servidor.ServidorJogo 192.168.1.15 1099
```

Para jogar também a partir da **Máquina 1**, abra um segundo terminal nela e execute:
```bash
java -cp bin cliente.ClienteJogo 192.168.1.15 1099
```

### Passo 4 — Conectar o Cliente na Máquina 2
Na **Máquina 2** (com o projeto compilado e na mesma rede local), execute:

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/comum/*.java src/servidor/*.java src/cliente/*.java
java -cp bin cliente.ClienteJogo 192.168.1.15 1099
```

Na janela gráfica:
1. Verifique se o campo **Host** aponta para o IP da Máquina 1 (`192.168.1.15`) e **Porta** está em `1099`.
2. Selecione **Multiplayer (2 Jogadores)** e clique em **Conectar**.

---

## Solução de Problemas Comuns

| Problema | Causa Provável | Solução |
| :--- | :--- | :--- |
| `javac: command not found` | O JDK não está no `PATH` do terminal. | Verifique a instalação com `javac -version` e adicione a pasta `bin` do JDK ao `PATH`. |
| `Connection refused to host: 127.0.0.1` na LAN | O servidor foi iniciado com `127.0.0.1` em vez do IP da rede. | Reinicie o `ServidorJogo` passando o IPv4 local da máquina (`192.168.x.x`). |
| Falha/Timeout ao conectar de outro PC | Firewall bloqueando portas do Java ou rede Wi-Fi com isolamento de clientes. | Libere o Java no Firewall para redes privadas ou use um roteador/hotspot sem isolamento AP. |
| `ExportException: Port already in use` | Já existe um servidor ou `rmiregistry` rodando na porta `1099`. | Encerre o processo anterior ou informe outra porta (ex.: `1100`) no servidor e nos clientes. |