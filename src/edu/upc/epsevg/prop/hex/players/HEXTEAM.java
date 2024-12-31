package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.PlayerType;
import edu.upc.epsevg.prop.hex.SearchType;

import java.awt.Point;
import java.util.*;

/**
 * HEXTEAM: Implementació amb IDS, taules de transposició i poda alfa-beta millorada.
 */
public class HEXTEAM implements IPlayer, IAuto {

    // Paràmetres de configuració
    private int maxDepth;               // profunditat màxima 
    private PlayerType playerType;      // tipus de jugador 
    private long nodesExplored;         // nodes explorats
    private boolean timeout;            // control del temps (ha expirat)
    private DijkstraHeuristic heuristic; // heurística 
    private final Map<Long, TranspositionEntry> transpositionTable; // taula de transposició
    private long startTime;             // temps d'inici
    private long timeoutMillis;         // temps límit en milisegons 

    /**
     * Constructor de la classe HEXTEAM
     * @param initialDepth profunditat inicial per a la cerca IDS
     * @param playerType tipus de jugador (PLAYER1 o PLAYER2)
     * @param timeoutMillis temps màxim permès per moviment
     */
    public HEXTEAM(int initialDepth, PlayerType playerType, long timeoutMillis) {
        this.maxDepth = initialDepth;
        this.playerType = playerType;
        this.timeout = false;
        this.heuristic = new DijkstraHeuristic();
        this.transpositionTable = new HashMap<>();
        this.timeoutMillis = timeoutMillis;
    }
 
    /**
     * Retorna el moviment seleccionat, utilitzant la cerca iterativa aprofundida (IDS).
     * @param joc
     * @return  PlayerMove
     */ 
    @Override
    public PlayerMove move(HexGameStatus joc) {
        timeout = false; 
        nodesExplored = 0;
        transpositionTable.clear();
        startTime = System.currentTimeMillis();
 
        Point millorMoviment = null;
        int profunditatActual = 1;
 
        // Es va incrementant la profunditat fins al límit o fins que expiri el temps
        while (!timeout && profunditatActual <= maxDepth) { 
            try {
                millorMoviment = cercaMillorMoviment(joc, profunditatActual);
            } catch (ExcepcioTempsEsgotat e) {
                break; // S'ha acabat el temps
            } 
            profunditatActual++;
        }
 
        return new PlayerMove(millorMoviment, nodesExplored, profunditatActual - 1, SearchType.MINIMAX_IDS);
    } 
 
    /** 
     * Cerca el millor moviment per a un nivell de profunditat concret.
     * @throws ExcepcioTempsEsgotat si s'excedeix el temps límit
     */
    private Point cercaMillorMoviment(HexGameStatus joc, int profunditat) throws ExcepcioTempsEsgotat {
        int millorValor = Integer.MIN_VALUE;
        Point millorMoviment = null;
        List<MoveNode> moviments = joc.getMoves();

        // Ordenem els moviments segons la heurística, per millorar la poda
        moviments.sort((a, b) -> {
            HexGameStatus jocA = new HexGameStatus(joc);
            jocA.placeStone(a.getPoint());
            HexGameStatus jocB = new HexGameStatus(joc);
            jocB.placeStone(b.getPoint());
            return Integer.compare(
                heuristic.avalua(jocB, playerType),
                heuristic.avalua(jocA, playerType)
            );
        });

        // Explorem els moviments (Minimax + poda alfa-beta) 
        for (MoveNode moviment : moviments) {
            comprovaTemps();
            HexGameStatus nouJoc = new HexGameStatus(joc);
            nouJoc.placeStone(moviment.getPoint());
            int valor = minimax(nouJoc, profunditat - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (valor > millorValor) {
                millorValor = valor;
                millorMoviment = moviment.getPoint();
            } 
        }
        return millorMoviment;
    }
 
    /** 
     * Implementació de Minimax amb poda alfa-beta i taula de transposició.
     * @throws ExcepcioTempsEsgotat si s'excedeix el temps límit
     */ 
    private int minimax(HexGameStatus joc, int profunditat, int alpha, int beta, boolean esMaximitzant) throws ExcepcioTempsEsgotat {
        comprovaTemps();
        nodesExplored++;
 
        // Càlcul o recuperació de la transposició
        long hashJoc = calculaHash(joc);
        if (transpositionTable.containsKey(hashJoc)) {
            TranspositionEntry entrada = transpositionTable.get(hashJoc);
            if (entrada.depth >= profunditat) {
                if (entrada.alpha >= beta) {
                    return entrada.alpha;
                }
                if (entrada.beta <= alpha) {
                    return entrada.beta;
                }
                alpha = Math.max(alpha, entrada.alpha);
                beta = Math.min(beta, entrada.beta);
            }
        }

        // Comprovem si la partida ha acabat
        if (joc.isGameOver()) {
            PlayerType guanyador = joc.GetWinner();
            if (guanyador == playerType) {
                // Resta de profunditat per evitar overflow
                return Integer.MAX_VALUE - profunditat;
            } else if (guanyador != null) {
                return Integer.MIN_VALUE + profunditat;
            }
            // No hi ha guanyador (rara vegades passa a Hex), tornem 0 
            return 0;
        }

        // Si hem arribat a la profunditat 0, avaluem
        if (profunditat == 0) { 
            int evaluacio = heuristic.avalua(joc, playerType);
            transpositionTable.put(hashJoc, new TranspositionEntry(evaluacio, alpha, beta, profunditat));
            return evaluacio; 
        }
 
        // Minimax recursiu 
        int millorPuntuacio = esMaximitzant ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<MoveNode> moviments = joc.getMoves(); 

        for (MoveNode moviment : moviments) { 
            HexGameStatus nouJoc = new HexGameStatus(joc);
            nouJoc.placeStone(moviment.getPoint());

            int puntuacio = minimax(nouJoc, profunditat - 1, alpha, beta, !esMaximitzant);

            if (esMaximitzant) { 
                millorPuntuacio = Math.max(millorPuntuacio, puntuacio);
                alpha = Math.max(alpha, millorPuntuacio);
            } else {
                millorPuntuacio = Math.min(millorPuntuacio, puntuacio);
                beta = Math.min(beta, millorPuntuacio);
            }

            if (beta <= alpha) {
                break;
            }
        }

        transpositionTable.put(hashJoc, new TranspositionEntry(millorPuntuacio, alpha, beta, profunditat));
        return millorPuntuacio;
    }

    /**
     * Genera un hash únic per a l'estat actual del tauler. 
     */
    private long calculaHash(HexGameStatus joc) {
        long hash = 0;
        for (int i = 0; i < joc.getSize(); i++) {
            for (int j = 0; j < joc.getSize(); j++) {
                hash = hash * 31 + joc.getPos(i, j);
            }
        }
        return hash;
    }
 
    /**
     * Comprova si ha expirat el temps límit.
     * @throws ExcepcioTempsEsgotat si el temps ha expirat
     */
    private void comprovaTemps() throws ExcepcioTempsEsgotat {
        if (System.currentTimeMillis() - startTime > timeoutMillis) {
            throw new ExcepcioTempsEsgotat();
        }
    }

    /**
     * Crida per l'entorn si es detecta que el temps ha expirat enmig d'una cerca.
     */
    @Override
    public void timeout() {
        timeout = true;
    }

    /**
     * Nom del jugador; definit a la interfície IPlayer.
     * @return 
     */
    @Override
    public String getName() {
        return "HEXTEAM";
    }

    /**
     * Classe interna per guardar informació a la taula de transposició.
     */
    private static class TranspositionEntry {
        int millorPuntuacio;
        int alpha;
        int beta;
        int depth;

        public TranspositionEntry(int millorPuntuacio, int alpha, int beta, int depth) {
            this.millorPuntuacio = millorPuntuacio;
            this.alpha = alpha;
            this.beta = beta;
            this.depth = depth;
        }
    }
 
    /**
     * Excepció personalitzada per indicar que el temps s'ha esgotat.
     */
    private static class ExcepcioTempsEsgotat extends Exception {}
}
