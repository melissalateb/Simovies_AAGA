package algorithms;

import robotsimulator.Brain;
import characteristics.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MoveAction;

import static characteristics.Parameters.*;

public class BrainCanevas extends Brain {
	private static final double SAFE_DISTANCE = teamBSecondaryBotRadius * 12;
	private static final double HEADINGPRECISION = 0.001;
	private static final double ANGLEPRECISION = 0.1;

	// Liste des noms stylés inspirés du monde du gaming
	private static final List<String> availableNames = List.of("Shadow", "Phoenix", "Vortex", "Titan", "Nova", "Blitz",
			"Cipher", "Echo", "Falcon", "Razor");

	private enum State {
		INIT, PATROL, DETECT_ENEMY, EVADE, DECIDE_AVOIDANCE, DETECT_TEAMMATE, CALCULATE_AVOIDANCE, AVOID_TEAMMATE,
		DETECT_WRECK, AVOID_WRECK, MEMORIZE_WRECK_POSITION, COMMUNICATE, KEEP_DISTANCE, DETECT_WALL, AVOID_WALL, DECIDE_AVOID_TEAMMATE
	}

	private State state = State.INIT;

	private double myX, myY, zoneX, zoneY;
	private double myHeading;
	private String myName;
	private double lastEnemyX = -1;
	private double lastEnemyY = -1;
	private double lastEnemyBearing = Double.NaN;
	private double lastEnemyDistance = Double.NaN;
	private List<Point> enemyPositions = new ArrayList<>();
	private double lastTeammateX = -1;
	private double lastTeammateY = -1;
	private Direction avoidanceDirection;
	private List<Point> wreckPositions = new ArrayList<>();
	private double lastTeammateBearing;
	// Compteur static pour suivre le nombre de robots activés
	private static int robotsActivated = 0;

	public BrainCanevas() {
		super();
	}

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
		state = State.PATROL;
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
		System.out.println(state);
		sendLogMessage(myName + " " + state);
		switch (state) {
			case PATROL:
				patrol();
				break;
			case DETECT_ENEMY:
				detectEnemy();
				state = State.DECIDE_AVOIDANCE; // Passez à l'état d'évasion
				break;
			case DECIDE_AVOIDANCE:
				decideAvoidance();
				break;
			case EVADE:
				evade();
				break;
			case DETECT_WALL:
				detectWall();
				break;
			case AVOID_WALL:
				avoidWall();
				break;
			case COMMUNICATE:
				// communicate();
				break;
			case DETECT_WRECK:
				detectWreck();
				break;
			case AVOID_WRECK:
				avoidWreck();
				break;
			case KEEP_DISTANCE:
				// keepDistance();
				break;
			case DETECT_TEAMMATE:
				moveBack();
				stepTurn(Direction.RIGHT);
				myMove();
				state = state.PATROL;
				break;
			default:
				break;
		}
	}

	private boolean isPathClear;

	private void detectAndEvaluateTeammate() {
		isPathClear = true;
		moveBack();
		for (IRadarResult radar : detectRadar()) {
			if (radar.getObjectType() == IRadarResult.Types.TeamMainBot ||
					radar.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
				// Évaluer la situation: direction, distance et action appropriée
				double bearing = radar.getObjectDirection();
				double distance = radar.getObjectDistance();
				isPathClear = false;

				// Déterminer l'action en fonction de la position relative
				if (bearing > -Math.PI / 4 && bearing < Math.PI / 4) {
					// L'objet est devant
					stepTurn(Direction.RIGHT); // Tournez à droite pour éviter
				} else if (bearing >= Math.PI / 4 && bearing <= 3 * Math.PI / 4) {
					// L'objet est sur la gauche
					stepTurn(Direction.LEFT); // Tournez à gauche pour éviter
				} else if (bearing <= -Math.PI / 4 && bearing >= -3 * Math.PI / 4) {
					// L'objet est sur la droite
					stepTurn(Direction.RIGHT); // Tournez à droite pour éviter
				} else {
					// L'objet est derrière, continuer
					isPathClear = true;
				}

				if (distance < SAFE_DISTANCE) {
					// Si trop proche, s'éloigner avant de continuer
					moveBack();
					stepTurn(Direction.RIGHT); // Et puis tournez à droite
				}
				state = State.PATROL;
				break; // Sortir de la boucle après avoir pris une décision
			}
		}
	}


	private void patrol() {
		// Détecter si un ennemi est directement devant nous
		for (IRadarResult radarResult : detectRadar()) {
			// Détecter les ennemis et changer l'état pour la fuite ou la communication
			if (radarResult.getObjectType() == IRadarResult.Types.OpponentMainBot
					|| radarResult.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				System.out.println("BrainCanevas.patrol() trouver " + radarResult.getObjectType());
				// moveBack();
				sendLogMessage(myName + " ennemie " + detectFront().getObjectType());
				state = State.DETECT_ENEMY;// ennemy state
				return;
			}

		}
		for (IRadarResult radarResult : detectRadar()) {
			if (radarResult.getObjectType() == IRadarResult.Types.TeamMainBot
					|| radarResult.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
				state = state.DETECT_TEAMMATE;
				return;
			}
		}
//		if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot
//				|| detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
//			state = state.DETECT_TEAMMATE;
//			return;
//		}
		if (detectFront().getObjectType() == IFrontSensorResult.Types.Wreck) {
			state = state.DETECT_WRECK;
		}
		if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
			state = state.DETECT_WALL;
		}

		chooseSafeDirection(); // Vérifiez la direction sûre avant de vous déplacer
		myMove();
		randomTurn();

	}

	private void detectWall() {
		IFrontSensorResult frontSensorResult = detectFront();
		if (frontSensorResult.getObjectType() == IFrontSensorResult.Types.WALL) {
			if (getHeading() >= EAST && getHeading() < SOUTH) {
				// Face à l'Est ou vers le Sud-Est, tourner à gauche
				System.out.println("BrainCanevas.detectWall() turn left east south");
				sendLogMessage(myName + " turn left east south");
				stepTurn(Direction.LEFT);
			} else if (getHeading() >= SOUTH && getHeading() < WEST) {
				// Face au Sud ou vers le Sud-Ouest, tourner à droite
				System.out.println("BrainCanevas.detectWall() turn right south west");
				sendLogMessage(myName + " turn right south west");
				stepTurn(Direction.RIGHT);
			} else if (getHeading() >= WEST && getHeading() < NORTH) {
				System.out.println("BrainCanevas.detectWall() turn left west north");
				sendLogMessage(myName + " turn left west north");
				// Face à l'Ouest ou vers le Nord-Ouest, tourner à gauche
				stepTurn(Direction.LEFT);
			} else {
				stepTurn(Direction.LEFT);
			}
			System.out.println("BrainCanevas.detectWall()");

		}
		if (frontSensorResult.getObjectType() == IFrontSensorResult.Types.WALL) {
			state = State.AVOID_WALL;
		} else {
			sendLogMessage(myName + " no more wall ");
			state = state.PATROL;
		}
	}

	private void avoidWall() {;
		System.out.println("BrainCanevas.avoidWall()");
		if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
			sendLogMessage(myName + " try to avoid wall");
			// moveBack();
			// Choix de la direction basée sur l'orientation actuelle
			if (getHeading() >= EAST && getHeading() < SOUTH) {
				// Face à l'Est ou vers le Sud-Est, tourner à gauche
				System.out.println("BrainCanevas.detectWall() turn left east south");
				sendLogMessage(myName + " turn left east south");
				stepTurn(Direction.LEFT);
			} else if (getHeading() >= SOUTH && getHeading() < WEST) {
				// Face au Sud ou vers le Sud-Ouest, tourner à droite
				System.out.println("BrainCanevas.detectWall() turn right south west");
				sendLogMessage(myName + " turn right south west");
				stepTurn(Direction.LEFT);
			} else if (getHeading() >= WEST && getHeading() < NORTH) {
				System.out.println("BrainCanevas.detectWall() turn left west north");
				sendLogMessage(myName + " turn left west north");
				// Face à l'Ouest ou vers le Nord-Ouest, tourner à gauche
				stepTurn(Direction.RIGHT);
			} else {
				stepTurn(Direction.LEFT);
			}
		}
		if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
			state = State.AVOID_WALL;
		} else {
			// Retour à la patrouille ou autre activité principale
			myMove();
			state = State.PATROL;
		}

	}

	private void detectEnemy() {
		for (IRadarResult radar : detectRadar()) {
			if (radar.getObjectType() == IRadarResult.Types.OpponentMainBot
					|| radar.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
				System.out.println("BrainCanevas.detectEnemy() trouver");
				// Enregistrez la direction et la distance de la dernière détection ennemie
				lastEnemyBearing = radar.getObjectDirection();
				lastEnemyDistance = radar.getObjectDistance();
				updateLastEnemyPosition(radar);
				state = State.DECIDE_AVOIDANCE;

				break;
			}
		}
		if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
			state = State.PATROL;
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

		state = State.EVADE;
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

		System.out.println("BrainCanevas.evade()");
		sendLogMessage(myName + " evade");
		chooseSafeDirection(); // Vérifiez et ajustez la direction avant de s'éloigner
		sendLogMessage(myName + " free");
		state = State.PATROL; // Sinon, retournez à la patrouille

	}

	private void updateLastEnemyPosition(IRadarResult radarResult) {
		int enemyX = (int) (myX + Math.cos(radarResult.getObjectDirection()) * radarResult.getObjectDistance());
		int enemyY = (int) (myY + Math.sin(radarResult.getObjectDirection()) * radarResult.getObjectDistance());

		Point enemyPosition = new Point(enemyX, enemyY);

		// Vérifier si la position de l'ennemi est déjà dans la liste
		boolean isNewPosition = enemyPositions.stream().noneMatch(p -> p.distance(enemyPosition) < SAFE_DISTANCE);

		// Si la position est nouvelle, l'ajouter à la liste et broadcaster la position
		if (isNewPosition) {
			enemyPositions.add(enemyPosition);
			broadcastEnemyPositions();
		}
	}

	// Une nouvelle méthode pour diffuser les positions des ennemis
	private void broadcastEnemyPositions() {
		StringBuilder message = new StringBuilder();
		for (Point position : enemyPositions) {
			message.append(String.format("Enemy at (%d, %d); ", position.x, position.y));
			System.out.println("msg " + String.format("Enemy at (%d, %d); ", position.x, position.y));
		}
		broadcast(message.toString().trim());
	}

	// Modifiez cette méthode pour qu'elle itère sur la liste de Point
	private boolean isSafeDirection(double targetX, double targetY) {
		Point targetPosition = new Point((int) targetX, (int) targetY);
		for (Point enemyPos : enemyPositions) {
			if (targetPosition.distance(enemyPos) < SAFE_DISTANCE) {
				return false; // La direction n'est pas sûre
			}
		}
		return true; // La direction est sûre
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


	private Direction evaluateDirectionToAvoid(double targetX, double targetY) {
		double angleToTarget = Math.atan2(targetY - myY, targetX - myX) - getHeading();
		// Normalize the angle
		angleToTarget = (angleToTarget + 2 * Math.PI) % (2 * Math.PI);
		// Decide on direction based on where the teammate is relative to the robot's
		// current heading
		if (angleToTarget > 0 && angleToTarget < Math.PI) {
			return Direction.RIGHT; // Turn right to avoid
		} else {
			return Direction.LEFT; // Turn left to avoid
		}
	}

	// Détecte les épaves avec le radar
	private void detectWreck() {
		for (IRadarResult radar : detectRadar()) {
			if (radar.getObjectType() == IRadarResult.Types.Wreck) {
				// Mémorisez la position de l'épave
				Point wreckPosition = new Point(
						(int) (myX + Math.cos(radar.getObjectDirection()) * radar.getObjectDistance()),
						(int) (myY + Math.sin(radar.getObjectDirection()) * radar.getObjectDistance()));
				wreckPositions.add(wreckPosition);
				state = State.AVOID_WRECK;
				break;
			}
		}
	}

	// Évite l'épave détectée
	private void avoidWreck() {
		// Évitez l'épave en effectuant un virage logique basé sur la direction actuelle
		// et la position de l'épave
		double wreckBearing = calculateBearingToLastWreck();
		if (wreckBearing > 0) {
			// Si l'épave est à droite, tournez à gauche
			stepTurn(Direction.LEFT);
		} else {
			// Si l'épave est à gauche, tournez à droite
			stepTurn(Direction.RIGHT);
		}
		// Passez à Patrol
		state = State.PATROL;
	}

	// Calcule la direction vers la dernière épave détectée
	private double calculateBearingToLastWreck() {
		Point lastWreck = wreckPositions.get(wreckPositions.size() - 1);
		return Math.atan2(lastWreck.y - myY, lastWreck.x - myX) - myGetHeading();
	}

	// Méthode pour effectuer un virage aléatoire
	private void randomTurn() {
		double random = Math.random();
		if (random < 0.1) {
			stepTurn(Direction.LEFT);
		} else if (random < 0.2) {
			stepTurn(Direction.RIGHT);
		}
	}

	private double normalizeHeading(double heading) {
		// Normalisez la direction pour qu'elle soit entre 0 et 2*PI.
		while (heading < 0)
			heading += 2 * Math.PI;
		while (heading >= 2 * Math.PI)
			heading -= 2 * Math.PI;
		return heading;
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