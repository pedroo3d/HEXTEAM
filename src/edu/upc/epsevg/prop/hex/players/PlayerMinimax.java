package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerType;
import edu.upc.epsevg.prop.hex.SearchType;
import java.awt.Point;
import java.util.List;

/**
 * Implementació de Minimax per al joc Hex amb poda alfa-beta.
 * @author 
 */
public class PlayerMinimax implements IPlayer, IAuto {

    private int maxDepth;                 // profunditat màxima
    private PlayerType playerType;       // tipus de jugador
    private long nodesExplored;          // nodes explorats
    private DijkstraHeuristic heuristic; // heurística

    public PlayerMinimax(int maxDepth, PlayerType playerType) {
        this.maxDepth = maxDepth;
        this.playerType = playerType;
        this.heuristic = new DijkstraHeuristic();
    }

    @Override
    public PlayerMove move(HexGameStatus game) {
        // Conservem 'move' per ser compatibles amb la interfície IPlayer
        nodesExplored = 0;
        Point millorMoviment = cercaMillorMoviment(game);
        return new PlayerMove(millorMoviment, nodesExplored, maxDepth, SearchType.MINIMAX);
    }

    /**
     * Cerca el millor moviment mitjançant dos passos:
     *  1) Bloquejar la victòria immediata de l'oponent si és possible.
     *  2) Aplicar Minimax per escollir el moviment òptim.
     */
    private Point cercaMillorMoviment(HexGameStatus joc) {
        int millorValor = Integer.MIN_VALUE;
        Point millorMoviment = null;
        List<MoveNode> moviments = joc.getMoves();

        // Pas 1: Bloquejar la victòria immediata de l'oponent
        for (MoveNode moviment : moviments) {
            HexGameStatus simulacioJoc = new HexGameStatus(joc);
            simulacioJoc.placeStone(moviment.getPoint());
            if (simulacioJoc.isGameOver()
                && simulacioJoc.GetWinner() == PlayerType.opposite(playerType)) {
                // Si l'oponent guanyaria en aquest moviment, el bloquegem
                return moviment.getPoint();
            }
        }

        // Pas 2: Cerca del millor moviment amb Minimax
        for (MoveNode moviment : moviments) {
            HexGameStatus nouJoc = new HexGameStatus(joc);
            nouJoc.placeStone(moviment.getPoint());

            int valor = minimax(nouJoc, maxDepth - 1, Integer.MIN_VALUE,
                                Integer.MAX_VALUE, false);

            if (valor > millorValor) {
                millorValor = valor;
                millorMoviment = moviment.getPoint();
            }
        }

        return millorMoviment;
    }

    /**
     * Implementació de Minimax amb poda alfa-beta.
     *
     * @param joc         estat actual del tauler
     * @param profunditat profunditat restant per explorar
     * @param alpha       valor alfa per a la poda
     * @param beta        valor beta per a la poda
     * @param esMaximitzant indica si és el torn de maximitzar o minimitzar
     * @return valor heurístic del node
     */
    private int minimax(HexGameStatus joc, int profunditat, int alpha, int beta, boolean esMaximitzant) {
        nodesExplored++;

        // Comprovem si la partida ha acabat
        if (joc.isGameOver()) {
            PlayerType guanyador = joc.GetWinner();
            // Si guanya el nostre jugador
            if (guanyador == playerType) {
                return Integer.MAX_VALUE - (maxDepth - profunditat);
            }
            // Si guanya l'altre jugador
            else if (guanyador != null) {
                return Integer.MIN_VALUE + (maxDepth - profunditat);
            }
            // Taules, encara que a Hex no solem tenir empats
            return 0;
        }

        // Si hem arribat a la profunditat límit, avaluem amb la nostra heurística
        if (profunditat == 0) {
            return heuristic.avalua(joc, playerType);
        }

        int millorPuntuacio = esMaximitzant ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<MoveNode> moviments = joc.getMoves();

        // Recorrem tots els possibles moviments
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

            // Poda alfa-beta
            if (beta <= alpha) {
                break;
            }
        }

        return millorPuntuacio;
    }

    @Override
    public void timeout() {
        // No s'usa en aquesta implementació
    }

    @Override
    public String getName() {       
        return "PlayerMiniMax";
    }
}
