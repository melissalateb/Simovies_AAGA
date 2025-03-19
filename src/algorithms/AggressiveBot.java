package algorithms;

import robotsimulator.Brain;
import characteristics.*;
import java.util.ArrayList;

/**
 * AggressiveBot : Bot agressif qui tire dès qu'un ennemi est détecté.
 *
 * Ce bot recherche en permanence des ennemis grâce au radar et tire immédiatement
 * sur eux. S'il n'en détecte pas, il se déplace aléatoirement.
 */
public class AggressiveBot extends Brain {

    @Override
    public void activate() {
        // Message d'activation (à voir dans les logs)
        sendLogMessage("AggressiveBot activé. Prêt à tirer !");
    }

    @Override
    public void step() {
        ArrayList<IRadarResult> radarResults = detectRadar();
        boolean enemyDetected = false;
        double enemyDirection = 0;

        // Parcours des résultats du radar pour détecter un ennemi
        for (IRadarResult radar : radarResults) {
            if (radar.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                    radar.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemyDetected = true;
                enemyDirection = radar.getObjectDirection();
                break;
            }
        }

        if (enemyDetected) {
            // Tire sur l'ennemi dès qu'il est détecté
            fire(enemyDirection);
            sendLogMessage("Ennemi détecté ! Tir dans la direction : " + enemyDirection);
        } else {
            // Aucun ennemi détecté : déplacement aléatoire pour chercher
            randomMovement();
        }
    }

    /**
     * Effectue un petit virage aléatoire puis avance.
     */
    private void randomMovement() {
        // Choix aléatoire du virage
        if (Math.random() < 0.5) {
            stepTurn(Parameters.Direction.LEFT);
        } else {
            stepTurn(Parameters.Direction.RIGHT);
        }
        move();
    }
}
