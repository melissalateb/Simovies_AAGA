package algorithms;

import robotsimulator.Brain;
import characteristics.*;
import java.util.ArrayList;
import java.util.Random;

import static characteristics.Parameters.*;

public class BrainCanevas extends Brain {
	// ------------------ PARAMÈTRES ------------------
	private static final double HEADINGPRECISION = 0.001; // Tolérance pour considérer l'angle atteint
	private static final int MOVE_BACK_DURATION = 25;       // Durée (en steps) pour reculer
	// bulletFiringLatency et LEFTTURNFULLANGLE doivent être définis dans Parameters

	// ------------------ VARIABLES ------------------
	private int stepNumber;             // Compteur global de steps
	private int stepNumberLastFire;     // Step où le dernier tir a été effectué
	private int stepNumberMoveBack;     // Step de début du recul
	private boolean turnTask;           // Indique si un virage doit être effectué
	private boolean moveBackTask;       // Indique si le bot doit reculer pour dégager son passage
	private double targetTurnDirection; // Angle cible lors d'un virage
	private Random rand;

	// ------------------ CONSTRUCTEUR ------------------
	public BrainCanevas() {
		super();
		rand = new Random();
	}

	@Override
	public void activate() {
		stepNumber = 0;
		stepNumberLastFire = 0;
		stepNumberMoveBack = 0;
		turnTask = false;
		moveBackTask = false;
		sendLogMessage("BrainCanevas activé – démarrage en PATROL");
		move();
	}

	@Override
	public void step() {
		stepNumber++;

		// PRIORITÉ 1 : DÉTECTION ET TIR SUR UN ENNEMI
		ArrayList<IRadarResult> radarDetections = detectRadar();
		IRadarResult enemy = null;
		for (IRadarResult r : radarDetections) {
			if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
					r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				if (enemy == null || r.getObjectDistance() < enemy.getObjectDistance()) {
					enemy = r;
				}
			}
		}
		if (enemy != null) {
			// Si le délai entre tirs est respecté, tirer sur l'ennemi
			if (stepNumber > stepNumberLastFire + bulletFiringLatency) {
				fire(enemy.getObjectDirection());
				stepNumberLastFire = stepNumber;
				sendLogMessage("Ennemi détecté – tir sur direction : " + enemy.getObjectDirection());
				return;
			} else {
				move();
				sendLogMessage("Ennemi détecté – attente de latence, avance");
				return;
			}
		}

		// PRIORITÉ 2 : SI UN OBJET (hors BULLET) EST TROP PRÈS, EFFECTUER UN RECUL
		for (IRadarResult r : radarDetections) {
			if (r.getObjectDistance() < 120 && r.getObjectType() != IRadarResult.Types.BULLET) {
				moveBackTask = true;
				stepNumberMoveBack = stepNumber;
				moveBack();
				sendLogMessage("Objet proche détecté – recul");
				return;
			}
		}

		// PRIORITÉ 3 : DÉTECTION D'OBSTACLE AVEC LE CAPTEUR FRONTAL
		IFrontSensorResult front = detectFront();
		if (front.getObjectType() == IFrontSensorResult.Types.WALL) {
			turnTask = true;
			targetTurnDirection = getHeading() + LEFTTURNFULLANGLE;
			targetTurnDirection = normalizeAngle(targetTurnDirection);
			sendLogMessage("Mur détecté – préparation au virage, cible = " + targetTurnDirection);
		}

		// Si en mode recul, continuer à reculer pendant MOVE_BACK_DURATION steps
		if (moveBackTask) {
			if (stepNumber < stepNumberMoveBack + MOVE_BACK_DURATION) {
				moveBack();
				return;
			} else {
				moveBackTask = false;
				turnTask = true;
				targetTurnDirection = getHeading() + LEFTTURNFULLANGLE;
				targetTurnDirection = normalizeAngle(targetTurnDirection);
				stepTurn(Direction.LEFT);
				sendLogMessage("Fin du recul – début du virage");
				return;
			}
		}

		// PRIORITÉ 4 : SI UN VIRAGE EST DEMANDÉ, TOURNER JUSQU'À ATTEINDRE L'ANGLE CIBLE
		if (turnTask) {
			if (isHeading(targetTurnDirection)) {
				turnTask = false;
				move();
				sendLogMessage("Virage terminé – reprise de l'avance");
			} else {
				stepTurn(Direction.LEFT);
				sendLogMessage("En cours de virage...");
			}
			return;
		}

		// PRIORITÉ 5 : MODE PATROL NORMAL
		if (front.getObjectType() == IFrontSensorResult.Types.NOTHING ||
				front.getObjectType() == IFrontSensorResult.Types.TeamMainBot ||
				front.getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
			move();
			sendLogMessage("PATROL : Avance");
		} else {
			// S'il y a un obstacle devant (autre que mur déjà traité), déclencher un virage
			turnTask = true;
			targetTurnDirection = getHeading() + LEFTTURNFULLANGLE;
			targetTurnDirection = normalizeAngle(targetTurnDirection);
			stepTurn(Direction.LEFT);
			sendLogMessage("Obstacle détecté – déclenchement du virage");
		}
	}

	/**
	 * Vérifie si l'angle courant (getHeading()) est proche de l'angle désiré.
	 * Tolérance fixée à HEADINGPRECISION.
	 */
	private boolean isHeading(double desired) {
		double diff = Math.abs(Math.sin(getHeading() - desired));
		sendLogMessage("Différence d'angle : " + diff);
		return diff < HEADINGPRECISION;
	}

	/**
	 * Normalise un angle pour qu'il soit dans l'intervalle [0, 2π).
	 */
	private double normalizeAngle(double angle) {
		while (angle < 0) angle += 2 * Math.PI;
		while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
		return angle;
	}
}
