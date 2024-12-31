package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.PlayerType;
import java.awt.Point;
import java.util.*;

/**
 *
 * @author pedroA
 */
public class DijkstraHeuristic {

    /**
     * Avalua l'estat del tauler fent servir múltiples heurístiques combinades. 
     * @param estatPartida estat actual del joc Hex.
     * @param jugador jugador per al qual volem calcular l'heurística.
     * @return valor heurístic resultant, 
     */
    public int avalua(HexGameStatus estatPartida, PlayerType jugador) {
        int puntuacioJugador = dijkstra(estatPartida, jugador);
        int puntuacioOponent = dijkstra(estatPartida, PlayerType.opposite(jugador));
        int puntuacioConnectivitat = avaluaConnectivitat(estatPartida, jugador);
        int puntuacioBloc = heuristicaBlocOponent(estatPartida, jugador);

        // Combina les diferents heurístiques per obtenir una puntuació final
        return (puntuacioOponent - puntuacioJugador) + puntuacioConnectivitat + puntuacioBloc;
    }

    /**
     * Implementació de l'algorisme de Dijkstra adaptat per avaluar distàncies en Hex.
     * @param joc estat actual del joc Hex.
     * @param jugador jugador per al qual es calcula la distància 
     * @return  distància mínima als vorals d'objectiu.
     */
    public int dijkstra(HexGameStatus joc, PlayerType jugador) {
        int midaTauler = joc.getSize();
        int[][] distancia = new int[midaTauler][midaTauler];
        boolean[][] visitats = new boolean[midaTauler][midaTauler];
        PriorityQueue<Point> cuaDePrioritat = new PriorityQueue<>(Comparator.comparingInt(p -> distancia[p.x][p.y]));

        // Inicialitza la cua de prioritat i la matriu de distàncies 
        for (int i = 0; i < midaTauler; i++) {
            Arrays.fill(distancia[i], Integer.MAX_VALUE);
            for (int j = 0; j < midaTauler; j++) {
                // Si és el voral inicial per a aquest jugador, el posem a la cua. 
                if ((jugador == PlayerType.PLAYER1 && i == 0) || (jugador == PlayerType.PLAYER2 && j == 0)) {
                    distancia[i][j] = (joc.getPos(i, j) == 0) ? 1 : 0;
                    cuaDePrioritat.add(new Point(i, j));
                }
            }
        }

        // S'executa l'algorisme de Dijkstra
        while (!cuaDePrioritat.isEmpty()) {
            Point actual = cuaDePrioritat.poll();
            if (visitats[actual.x][actual.y]) continue;
            visitats[actual.x][actual.y] = true;

            for (Point vei : joc.getNeigh(actual)) {
                if (!visitats[vei.x][vei.y]) {
                    int distanciaCalculada = distancia[actual.x][actual.y] + ((joc.getPos(vei) == 0) ? 1 : 0);
                    if (distanciaCalculada < distancia[vei.x][vei.y]) {
                        distancia[vei.x][vei.y] = distanciaCalculada;
                        cuaDePrioritat.add(vei);
                    }
                }
            }
        }
        
        
        // Calcula la distància mínima als vorals d'objectiu. 
        int distanciaMinima = Integer.MAX_VALUE; 
        for (int i = 0; i < midaTauler; i++) { 
            int indexVora = (jugador == PlayerType.PLAYER1) ? (midaTauler - 1) : i;
            int indexDist = (jugador == PlayerType.PLAYER1)
                    ? distancia[indexVora][i]
                    : distancia[i][indexVora];
            distanciaMinima = Math.min(distanciaMinima, indexDist);
        }

        return distanciaMinima;
    }

    /** 
     * Avalua la connectivitat de les peces del jugador al tauler. 
     * @param joc estat actual del joc Hex  
     * @param jugador jugador per al qual es vol avaluar la connectivitat.
     * @return puntuació de connectivitat
     */ 
    public int avaluaConnectivitat(HexGameStatus joc, PlayerType jugador) {
        return calculaConnectivitat(joc, jugador, true);
    }
       
    /** 
     * Avalua l'impacte de bloquejar l'oponent.
     * @param joc estat actual del joc Hex 
     * @param jugador jugador per al qual es vol avaluar el blocatge.  
     * @return puntuació de blocatge.  
     */ 
    public int heuristicaBlocOponent(HexGameStatus joc, PlayerType jugador) {
        return calculaConnectivitat(joc, PlayerType.opposite(jugador), false);
    }
 
    
    /**
     * Mètode genèric per calcular la connectivitat o el blocatge segons el paràmetre.
     * @param joc estat actual del joc 
     * @param jugador jugador a analitzar 
     * @param comptaPropi si true, compta la connectivitat pròpia;  si false, compta el blocatge
     * @return puntuació calculada. 
     */
    private int calculaConnectivitat(HexGameStatus joc, PlayerType jugador, boolean comptaPropi) {
        int midaTauler = joc.getSize();
        int puntuacio = 0;

        for (int i = 0; i < midaTauler; i++) {
            for (int j = 0; j < midaTauler; j++) {
                if (joc.getPos(i, j) == jugador.ordinal() + 1) {
                    for (Point vei : joc.getNeigh(new Point(i, j))) {
                        if (comptaPropi && joc.getPos(vei) == jugador.ordinal() + 1) {
                            puntuacio++; // Connectivitat pròpia
                        } else if (!comptaPropi
                                && joc.getPos(vei) == PlayerType.opposite(jugador).ordinal() + 1) {
                            puntuacio++; // bloqueja l'oponent
                        }
                    }
                }
            }
        }
        // Evitem comptar dues vegades les connexions. 
        return puntuacio / 2;
    }
}