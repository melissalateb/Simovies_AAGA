package algorithms;

import static characteristics.Parameters.*;
import static java.lang.Math.*;

import robotsimulator.Brain;
import characteristics.IRadarResult;
import characteristics.Parameters.Direction;
import java.util.ArrayList;

public class BrainMain extends Brain {

    // Variables simulant la position (à ajuster selon votre environnement)
    private double myX = 1500;  // Position initiale X
    private double myY = 1500;  // Position initiale Y
    private double speed = 5;   // Vitesse de déplacement

    // Variables de ciblage (en coordonnées absolues)
    private double enemyX = -1;
    private double enemyY = -1;
    private boolean enemyMessageReceived = false;

    // Paramètres de distances pour les robots Main
    private static final double TARGET_DISTANCE = 1000;
    private static final double MIN_SHOOT_DISTANCE = 800;
    private static final double MAX_SHOOT_DISTANCE = 1000;
    private static final double TOLERANCE_APPROACH = 50;  // Tolérance pour ajuster la distance

    @Override
    public void activate() {
        sendLogMessage("BrainMain activé – comportement MAIN (tir immédiat)");
    }

    @Override
    public void step() {
        // Récupération des messages diffusés par les Secondary
        ArrayList<String> messages = fetchAllMessages();
        enemyMessageReceived = false;
        for (String msg : messages) {
            // On attend un message de type "ENEMY:x:y"
            if (msg.startsWith("ENEMY:")) {
                try {
                    String[] parts = msg.split(":");
                    enemyX = Double.parseDouble(parts[1]);
                    enemyY = Double.parseDouble(parts[2]);
                    enemyMessageReceived = true;
                    sendLogMessage("Main: Message enemy reçu: (" + enemyX + ", " + enemyY + ")");
                } catch (Exception e) {
                    // Erreur de format : ignorer
                }
            }
        }

        if (enemyMessageReceived) {
            // Dès le début, commence à tirer dès qu'un message est reçu.
            double dx = enemyX - myX;
            double dy = enemyY - myY;
            double distance = sqrt(dx * dx + dy * dy);
            double angleToEnemy = atan2(dy, dx);
            sendLogMessage("Main: Distance à l'ennemi = " + distance);

            // Démarrer le tir immédiatement
            if (distance >= MIN_SHOOT_DISTANCE && distance <= MAX_SHOOT_DISTANCE) {
                fire(angleToEnemy);
                // Un petit virage pour orbiter légèrement autour de l'ennemi
                stepTurn(Direction.LEFT);
                sendLogMessage("Main: Tir et orbite");
            } else {
                // Si la distance n'est pas idéale, ajuster la position
                if (distance > TARGET_DISTANCE + TOLERANCE_APPROACH) {
                    move();
                    sendLogMessage("Main: Trop loin, avancer");
                } else if (distance < TARGET_DISTANCE - TOLERANCE_APPROACH) {
                    moveBack();
                    sendLogMessage("Main: Trop proche, reculer");
                } else {
                    fire(angleToEnemy);
                    stepTurn(Direction.LEFT);
                    sendLogMessage("Main: Tir et orbite (distance ajustée)");
                }
            }
            updatePosition();
        } else {
            // En l'absence de message, patrouiller
            patrolling();
        }
    }

    private void patrolling() {
        move();
        stepTurn(Direction.LEFT);
        updatePosition();
        sendLogMessage("Main: Patrouille");
    }

    private void updatePosition() {
        double heading = getHeading();
        myX += speed * cos(heading);
        myY += speed * sin(heading);
    }
}
