package algorithms;

import static characteristics.Parameters.EAST;
import static characteristics.Parameters.NORTH;
import static characteristics.Parameters.SOUTH;
import static characteristics.Parameters.WEST;
import static characteristics.Parameters.teamASecondaryBot1InitHeading;
import static characteristics.Parameters.teamASecondaryBot1InitX;
import static characteristics.Parameters.teamASecondaryBot1InitY;
import static characteristics.Parameters.teamASecondaryBot2InitHeading;
import static characteristics.Parameters.teamASecondaryBot2InitX;
import static characteristics.Parameters.teamASecondaryBot2InitY;
import static characteristics.Parameters.teamBSecondaryBotRadius;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import characteristics.IRadarResult;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;
import characteristics.Parameters.Direction;
import robotsimulator.Brain;

public class SecondaryTeam extends Brain {

	private static final double SAFE_DISTANCE = teamBSecondaryBotRadius * 12;
	private static final double HEADINGPRECISION = 0.001;


	private static final double ANGLEPRECISION = 0.1;
	// Liste des noms stylés inspirés du monde du gaming
	private static final List<String> availableNames = List.of("Shadow", "Phoenix", "Vortex", "Titan", "Nova", "Blitz",
			"Cipher", "Echo", "Falcon", "Razor");

	private enum State {
		SCOUT, REPORT, EVADE, DISTRACT, REGROUP, RETREAT, DECIDE_AVOIDANCE, AVOID_WALL, MOVE_AWAY
	}



	private double lastEnemyX = -1;
	private double lastEnemyY = -1;
	private double lastEnemyBearing = Double.NaN;
	private double lastEnemyDistance = Double.NaN;
	private Direction avoidanceDirection;
	private List<Point> enemyPositions = new ArrayList<>();


	private State state = State.SCOUT;
	private double myX, myY, zoneX, zoneY;
	private double myHeading;
	private String myName;
	private IRadarResult myTarget;
	private boolean regroupe;
	// Compteur static pour suivre le nombre de robots activés
	private static int robotsActivated = 0;

	@Override
	public void activate() {
		// Attribuer un nom unique au robot
		assignName();

		// Assigner la position initiale basée sur l'ordre d'activation
		if (robotsActivated == 0) {
			myX = teamASecondaryBot1InitX;
			myY = teamASecondaryBot1InitY;
			myHeading = teamASecondaryBot1InitHeading; // Supposé être une valeur en radians ou convertir si nécessaire
		} else if (robotsActivated == 1) {
			myX = teamASecondaryBot2InitX;
			myY = teamASecondaryBot2InitY;
			myHeading = teamASecondaryBot2InitHeading; // Supposé être une valeur en radians ou convertir si nécessaire
		}

		// Incrementer le nombre de robots activés
		robotsActivated++;
		state = State.SCOUT;
		// Affichage des informations du robot pour vérification
		System.out.println(
				"Robot " + myName + " activated with position (" + myX + ", " + myY + ") and heading " + myHeading);
	}

	private void assignName() {
		if (availableNames.size() > robotsActivated) {
			myName = availableNames.get(robotsActivated);
		} else {
			// Générer un nom aléatoire si plus de robots que de noms disponibles
			myName = "Robot" + (robotsActivated + 1);
		}
	}

	@Override
	public void step() {
		chooseSafeDirection();
		if(regroupe) {
			regroupe = false;
			state = state.REGROUP;
		}
		sendLogMessage(myName+" "+state+" "+(state == State.AVOID_WALL));
		switch (state) {
			case SCOUT:
				handleScout();
				break;
			case REPORT:
				handleReport();
				break;
			case EVADE:
				handleEvade();
				break;
			case DISTRACT:
				handleDistract();
				break;
			case REGROUP:
				handleRegroup();
				break;
			case AVOID_WALL:
				handleAvoidWall();
				break;
			case RETREAT:
				handleRetreat();
				break;
		}
	}

	private void handleScout() {
		// Exécuter une analyse radar pour repérer les ennemis.
		ArrayList<IRadarResult> radarResults = detectRadar();

		// Gérer la détection frontale d'un mur
		if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
			// Implémenter un comportement d'évitement d'obstacle, comme un demi-tour
			state = State.AVOID_WALL;
			// Puis retourner pour éviter d'exécuter d'autres comportements
			return;
		}

		// Gérer la détection radar d'une épave
		for (IRadarResult radarResult : radarResults) {
			if (radarResult.getObjectType() == IRadarResult.Types.Wreck) {
				// Éviter la zone de l'épave, en supposant que teamASecondaryBotSpeed et
				// stepTurnAngle sont définis dans Parameters
				avoidWreck(radarResult);
				// Puis retourner pour éviter d'exécuter d'autres comportements
				return;
			}
		}
		for (IRadarResult result : radarResults) {
			// Vérifier si l'objet détecté est un ennemi.
			if (result.getObjectType() == IRadarResult.Types.OpponentMainBot
					|| result.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				// Si un ennemi est détecté, stocker ses coordonnées et changer l'état pour
				// REPORT.
				myTarget = result; // myTarget est une variable hypothétique stockant le résultat du radar.
				state = State.REPORT;
				break;

			}
		}

		// Si aucun ennemi n'est détecté, continuer à patrouiller.

		patrolArea();
		// Si nous sommes encore en SCOUT après avoir vérifié pour les ennemis, cela
		// signifie qu'aucun ennemi direct n'a été détecté.
		// Nous pouvons ajouter des comportements supplémentaires ici, comme se déplacer
		// vers un nouveau point de patrouille.
	}

	private void avoidWreck(IRadarResult wreck) {
		// Méthode d'évitement simplifiée - tourne dans la direction opposée à l'épave
		double directionToWreck = wreck.getObjectDirection();
		if (directionToWreck < 0) {
			// Épave sur la gauche, tourner à droite
			stepTurn(Parameters.Direction.RIGHT);
		} else {
			// Épave sur la droite, tourner à gauche
			stepTurn(Parameters.Direction.LEFT);
		}
		// Effectuer une action pour éloigner le robot de l'épave
		moveBack(); // Supposons que cette méthode existe pour faire reculer le robot
	}

	private void patrolArea() {
		myMove();
	}

	private void handleReport() {
		// Assurez-vous que myTarget est défini par une autre méthode après avoir détecté un ennemi
		if (myTarget != null) {
			// Préparer le message à envoyer
			String message = String.format(
					"Enemy spotted at x: %f, y: %f, heading: %f",
					myX, myY, myHeading
			);

			// Envoyer le message aux MainBots
			broadcast(message);

			// Après avoir signalé la position de l'ennemi, décider de l'action suivante
			// Peut-être retourner en mode SCOUT ou passer en EVADE si le robot est en danger
			state = State.EVADE; // ou State.EVADE si le robot doit esquiver des tirs ennemis
		}
	}

	private void handleEvade() {
		for (IRadarResult radar : detectRadar()) {
			if (radar.getObjectType() == IRadarResult.Types.OpponentMainBot
					|| radar.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				// Enregistrez la direction et la distance de la dernière détection ennemie
				lastEnemyBearing = radar.getObjectDirection();
				lastEnemyDistance = radar.getObjectDistance();
				updateLastEnemyPosition(radar);
				decideAvoidance();
				state = State.SCOUT;
				break;
			}
		}
		if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
			state = State.SCOUT;
		}
	}


	private void updateLastEnemyPosition(IRadarResult radarResult) {
		int enemyX = (int) (myX + Math.cos(radarResult.getObjectDirection()) * radarResult.getObjectDistance());
		int enemyY = (int) (myY + Math.sin(radarResult.getObjectDirection()) * radarResult.getObjectDistance());
		lastEnemyX = enemyX; lastEnemyY = enemyY;
		Point enemyPosition = new Point(enemyX, enemyY);

		// Vérifier si la position de l'ennemi est déjà dans la liste
		boolean isNewPosition = enemyPositions.stream().noneMatch(p -> p.distance(enemyPosition) < SAFE_DISTANCE);

		// Si la position est nouvelle, l'ajouter à la liste et broadcaster la position
		if (isNewPosition) {
			enemyPositions.add(enemyPosition);
		}
	}

	private void decideAvoidance() {
		double enemyBearingFromNorth = calculateBearingFromNorth(lastEnemyBearing);
		// Déterminer les zones cardinales
		boolean enemyInNorth = enemyBearingFromNorth > WEST && enemyBearingFromNorth < EAST;
		boolean enemyInEast = enemyBearingFromNorth > NORTH && enemyBearingFromNorth < SOUTH;

		// Choisir la direction d'évitement opposée
		if (enemyInNorth) {
			// Si l'ennemi est au nord, nous choisissons sud comme direction d'évitement
			avoidanceDirection = (enemyInEast) ? Direction.LEFT : Direction.RIGHT;
		} else {
			// Si l'ennemi est au sud, nous choisissons nord comme direction d'évitement
			avoidanceDirection = (enemyInEast) ? Direction.RIGHT : Direction.LEFT;
		}

		evade();
	}

	// Méthode pour calculer la direction relative à partir du nord
	private double calculateBearingFromNorth(double enemyBearing) {
		// Convertir la direction de l'ennemi en une direction relative à partir du nord
		double bearingFromNorth = Math.PI / 2 - (getHeading() + enemyBearing);

		// Normaliser la direction pour qu'elle soit entre 0 et 2*Math.PI
		bearingFromNorth = normalizeAngle(bearingFromNorth);

		return bearingFromNorth;
	}

	// Méthode pour normaliser un angle entre 0 et 2*Math.PI
	private double normalizeAngle(double angle) {
		while (angle < 0)
			angle += 2 * Math.PI;
		while (angle >= 2 * Math.PI)
			angle -= 2 * Math.PI;
		return angle;
	}

	private void evade() {
		if (avoidanceDirection != null) {
			stepTurn(avoidanceDirection);
			myMove(); // Exécuter le mouvement après le virage pour augmenter la distance
			avoidanceDirection = null; // Réinitialiser pour le prochain appel
		}

		chooseSafeDirection(); // Vérifiez et ajustez la direction avant de s'éloigner
		sendLogMessage(myName + " free");
		state = State.SCOUT; // Sinon, retournez à la patrouille
		regroupe = true;
	}

	private void chooseSafeDirection() {
		double bestHeading = getHeading();
		double maxDistanceFromEnemies = 0;
		// Essayer différentes directions
		for (int i = 0; i < 360; i += 45) {
			double testHeading = Math.toRadians(i);
			double potentialX = myX + Parameters.teamASecondaryBotSpeed * Math.cos(testHeading);
			double potentialY = myY + Parameters.teamASecondaryBotSpeed * Math.sin(testHeading);

			double minDistanceToEnemies = calculateMinDistanceToEnemies(potentialX, potentialY);

			// Sélectionner la direction offrant la plus grande distance de sécurité
			if (minDistanceToEnemies > maxDistanceFromEnemies) {
				maxDistanceFromEnemies = minDistanceToEnemies;
				bestHeading = testHeading;
			}
		}

		// Appliquer la direction choisie
		if (Math.abs(bestHeading - myGetHeading()) > HEADINGPRECISION) {
			myHeading = bestHeading;
			adjustHeading();
		}
	}

	private double calculateMinDistanceToEnemies(double potentialX, double potentialY) {
		double minDistance = Double.MAX_VALUE; // Initialiser à une très grande valeur
		Point potentialPosition = new Point((int) potentialX, (int) potentialY);

		// Parcourir chaque position ennemie connue
		for (Point enemyPos : enemyPositions) {
			double distanceToEnemy = potentialPosition.distance(enemyPos);

			// Si la distance à cet ennemi est inférieure à la distance minimale précédente,
			// la mettre à jour
			if (distanceToEnemy < minDistance) {
				minDistance = distanceToEnemy;
			}
		}

		// Retourner la distance minimale trouvée entre la position potentielle et les
		// ennemis
		return minDistance;
	}

	private void adjustHeading() {
		double headingDifference = myHeading - myGetHeading();
		if (Math.abs(headingDifference) > HEADINGPRECISION) {
			if (headingDifference > 0) {
				stepTurn(Parameters.Direction.RIGHT);
			} else {
				stepTurn(Parameters.Direction.LEFT);
			}
		}
	}

	private void handleDistract() {
		// Logique pour distraire l'ennemi
		// Transition vers EVADE ou REGROUP
	}

	private void handleRegroup() {
		double targetX = lastEnemyX; // Coordonnée X du point de rendez-vous
		double targetY = lastEnemyY; // Coordonnée Y du point de rendez-vous
		lastEnemyX = -1; lastEnemyY = -1;
		// Calculer l'angle de déplacement requis pour se diriger vers le point de rendez-vous
		double angleToTarget = Math.atan2(targetY - myY, targetX - myX);

		// S'assurer que le robot est orienté dans la bonne direction avant de se déplacer
		adjustHeadingTowards(angleToTarget);

		// Détection par radar pour les obstacles et ennemis
		ArrayList<IRadarResult> radarResults = detectRadar();
		for (IRadarResult radarResult : radarResults) {
			if (radarResult.getObjectType() == IRadarResult.Types.OpponentMainBot ||
					radarResult.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				// Un ennemi est détecté; passer en REPORT
				state = State.REPORT;
				return;
			}
		}

		// Détection frontale pour gérer les murs et les obstacles immédiats
		IFrontSensorResult frontResult = detectFront();
		if (frontResult.getObjectType() == IFrontSensorResult.Types.WALL ||
				frontResult.getObjectType() == IFrontSensorResult.Types.Wreck ||
				frontResult.getObjectType() == IFrontSensorResult.Types.TeamMainBot ||
				frontResult.getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
			// Gérer la détection d'un obstacle en tournant légèrement pour l'éviter
			stepTurn(Parameters.Direction.LEFT); // Choix arbitraire, pourrait être ajusté selon la situation
		} else {
			// Aucun obstacle immédiat; avancer vers le point de rendez-vous
			myMove();
		}

		// Vérifier si le robot a atteint le point de rendez-vous
		if (isAtRegroupPoint(targetX, targetY)) {
			state = State.SCOUT; // Arrivé au point de rendez-vous, retourner en mode SCOUT
		}
	}

	private void adjustHeadingTowards(double targetDirection) {
		double currentHeading = myGetHeading();
		double angleDifference = normalizeAngle(targetDirection - currentHeading);

		if (Math.abs(angleDifference) > HEADINGPRECISION) {
			Parameters.Direction turnDirection = angleDifference > 0 ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT;
			stepTurn(turnDirection);
		}
	}

	private boolean isAtRegroupPoint(double targetX, double targetY) {
		return Math.hypot(targetX - myX, targetY - myY) < Parameters.teamASecondaryBotSpeed; // Distance moins que un pas
	}


	private void handleRetreat() {
		// Logique pour se replier en toute sécurité
		// Peut-être un état final à moins que le robot puisse être réparé
	}

	// Méthode utilitaire pour détecter les ennemis et autres objets
	private void performRadarScan() {
		// Utiliser detectRadar pour scanner l'environnement
		// Exemple d'utilisation (à adapter) :
		ArrayList<IRadarResult> radarResults = detectRadar();
		for (IRadarResult r : radarResults) {
			// Analyser les résultats et prendre des décisions
		}
	}

	private void handleAvoidWall() {
		IFrontSensorResult frontResult = detectFront();
		if (frontResult.getObjectType() == IFrontSensorResult.Types.WALL) {
			// Déterminer la direction de rotation basée sur l'orientation actuelle du robot
			decideDirectionToAvoidWall();
		} else {
			// Si aucun mur n'est détecté devant, continuer à avancer
			myMove();
			state = State.SCOUT;
		}
	}

	private void decideDirectionToAvoidWall() {
		// Utilisez l'orientation actuelle du robot pour décider si tourner à droite ou à gauche
		double heading = getHeading();

		if (heading >= Parameters.NORTH && heading < Parameters.EAST) {
			// Si le robot est orienté vers le nord, tournez à gauche pour éviter le mur
			stepTurn(Parameters.Direction.LEFT);
		} else if (heading >= Parameters.EAST && heading < Parameters.SOUTH) {
			// Si le robot est orienté vers l'est, tournez à gauche aussi
			stepTurn(Parameters.Direction.LEFT);
		} else if (heading >= Parameters.SOUTH && heading < Parameters.WEST) {
			// Si le robot est orienté vers le sud, tournez à droite pour éviter le mur
			stepTurn(Parameters.Direction.RIGHT);
		} else {
			// Si le robot est orienté vers l'ouest, tournez à droite aussi
			stepTurn(Parameters.Direction.RIGHT);
		}

		state = State.SCOUT; // Changez l'état pour exécuter le mouvement d'évitement au prochain step
	}
	private boolean isHeadingTowards(double direction) {
		double heading = getHeading();
		return Math.abs(heading - direction) < HEADINGPRECISION;
	}




	private void myMove() {
		// Fait avancer le robot
		move();
		myX += Parameters.teamASecondaryBotSpeed * Math.cos(myGetHeading());
		myY += Parameters.teamASecondaryBotSpeed * Math.sin(myGetHeading());
		myHeading = myGetHeading();
	}

	private double myGetHeading() {
		double result = getHeading();
		while (result < 0)
			result += 2 * Math.PI;
		while (result > 2 * Math.PI)
			result -= 2 * Math.PI;
		return result;
	}

	private boolean isSameDirection(double dir1, double dir2) {
		return Math.abs(dir1 - dir2) < ANGLEPRECISION;
	}

}