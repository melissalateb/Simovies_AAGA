package algorithms;

import robotsimulator.Brain;
import characteristics.*;
import java.util.ArrayList;

import static characteristics.Parameters.*;

public class AggressiveBotTeamA extends Brain {
    // Positions et direction internes
    private double myX, myY, myHeading;
    private String myName;
    private static int robotsActivated = 0;

    // États simples pour gérer le comportement
    private enum State { PATROL, ATTACK }
    private State state = State.PATROL;

    @Override
    public void activate() {
        // Affectation de la position initiale selon l'ordre d'activation (pour l'équipe A)
        if (robotsActivated == 0) {
            myX = teamASecondaryBot1InitX;
            myY = teamASecondaryBot1InitY;
            myHeading = teamASecondaryBot1InitHeading;
        } else {
            myX = teamASecondaryBot2InitX;
            myY = teamASecondaryBot2InitY;
            myHeading = teamASecondaryBot2InitHeading;
        }
        myName = "AggroBotTeamA_V2_" + (robotsActivated + 1);
        robotsActivated++;
        state = State.PATROL;
        sendLogMessage(myName + " activé, prêt à agir !");
    }

    @Override
    public void step() {
        // 1. Vérifier la présence d'un mur devant
        IFrontSensorResult frontResult = detectFront();
        if (frontResult.getObjectType() == IFrontSensorResult.Types.WALL) {
            sendLogMessage(myName + " mur détecté ! Évitement en cours...");
            // On effectue un virage pour éviter le mur
            stepTurn(Direction.LEFT);
            move();
            return;
        }

        // 2. Lire les résultats du radar
        ArrayList<IRadarResult> radarResults = detectRadar();
        sendLogMessage(myName + " Nombre de détections radar : " + radarResults.size());
        boolean enemyFound = false;
        double enemyDir = 0;
        for (IRadarResult r : radarResults) {
            // Pour débogage, on peut loguer chaque détection si besoin
            // sendLogMessage(myName + " Radar -> Type: " + r.getObjectType() +
            //                ", Direction: " + r.getObjectDirection() +
            //                ", Distance: " + r.getObjectDistance());
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                    r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemyFound = true;
                enemyDir = r.getObjectDirection();
                break;
            }
        }

        // 3. Si un ennemi est trouvé, passer en mode ATTACK, sinon PATROL
        if (enemyFound) {
            state = State.ATTACK;
        } else {
            state = State.PATROL;
        }

        // 4. Exécuter l'action selon l'état courant
        if (state == State.ATTACK) {
            fire(enemyDir);
            sendLogMessage(myName + " ATTACK : Tire sur l'ennemi à la direction " + enemyDir);
        } else {
            // En mode PATROL, on effectue un mouvement aléatoire
            randomMovement();
        }

        // 5. Mise à jour interne de la position
        updatePosition();
    }

    /**
     * Effectue un virage aléatoire (à gauche ou à droite) puis avance.
     */
    private void randomMovement() {
        if (Math.random() < 0.5) {
            stepTurn(Direction.LEFT);
            sendLogMessage(myName + " PATROL : Tourne à gauche");
        } else {
            stepTurn(Direction.RIGHT);
            sendLogMessage(myName + " PATROL : Tourne à droite");
        }
        move();
    }

    /**
     * Met à jour la position et la direction internes du bot.
     */
    private void updatePosition() {
        myX += teamASecondaryBotSpeed * Math.cos(getHeading());
        myY += teamASecondaryBotSpeed * Math.sin(getHeading());
        myHeading = getHeading();
    }
}
