/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters;
import characteristics.Parameters.Direction;
import robotsimulator.Brain;

import java.util.ArrayList;
import java.util.HashMap;


public class SecondaryRobot extends Brain {

    /*************************   IMPORTANT   ******************************/
    /** Modifier cette variable en fonction du côté qui nous est attribué **/
    // - à gauche => true
    // - à droite => false
    private boolean isLeftTeam = true;

    /*********************************************************************/


    //---PARAMETERS---//
    private static final double ANGLEPRECISION = 0.001;
    private static final double ANGLEPRECISIONBIS = 0.01;


    private static final double MAIN = 0xAAAAFFFF;
    private static final double SECONDARY = 0xFFFFAAAA;

    private static final int ROCKY = 0x1EBDDB;
    private static final int MARIO = 0x5ECD;
    private static final int ALPHA = 0x1EADDA;
    private static final int BETA = 0x5EC0;
    private static final int GAMMA = 0x333;
    private static final int TEAM = 0xBADDAD;
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE = 0xB52;
    private static final int POSITION = 0x7E57;
    private static final int OVER = 0xC00010FF;

    private static final int TURNNORTHTASK = 1;
    private static final int TURNSOUTHTASK = 2;
    private static final int TURNEASTTASK = 3;
    private static final int TURNWESTTASK = 4;
    private static final int MOVETASK = 5;
    private static final int FIRSTMOVETASK = 6;
    private static final int FLEE = 7;
    private static final int TURNLEFTTASK = 8;
    private static final int MOVEBACKTASK = 9;
    private static final int TURNRIGHTTASK = 10;
    private static final int FIRSTTURNNORTHTASK = 11;
    private static final int FIRSTTURNSOUTHTASK = 22;
    private static final int SINK = 0xBADC0DE1;

    //---VARIABLES---//
    private int state;
    private double myX,myY;
    private boolean isMoving;
    private int whoAmI;
    private HashMap<Integer, ArrayList<Double>> allies;
    private ArrayList<IRadarResult> ennemies;
    private double endTaskDirection;
    private int stepNumber, stepNumberMoveBack;
    private boolean isMovingBack;
    private boolean leftTeam;

    //---CONSTRUCTORS---//
    public SecondaryRobot() {
        super();
        allies = new HashMap<Integer, ArrayList<Double>>(5);
        ArrayList<Double> temp = new ArrayList<Double>(2);
        temp.add(0.);
        temp.add(0.);
        allies.put(ALPHA, temp);
        allies.put(BETA, temp);
        allies.put(GAMMA, temp);
        allies.put(ROCKY, temp);
        allies.put(MARIO, temp);
        ennemies = new ArrayList<IRadarResult>();
    }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        //ODOMETRY CODE
        whoAmI = ROCKY;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=MARIO;

        if (isLeftTeam) {
            if (whoAmI == ROCKY){
                myX=Parameters.teamASecondaryBot1InitX;
                myY=Parameters.teamASecondaryBot1InitY;
                state=FIRSTTURNNORTHTASK;
            } else {
                myX=Parameters.teamASecondaryBot2InitX;
                myY=Parameters.teamASecondaryBot2InitY;
                state=FIRSTTURNSOUTHTASK;
            }

        } else {
            if (whoAmI == ROCKY){
                myX=Parameters.teamBSecondaryBot1InitX;
                myY=Parameters.teamBSecondaryBot1InitY;
                state=FIRSTTURNNORTHTASK;
            } else {
                myX=Parameters.teamBSecondaryBot2InitX;
                myY=Parameters.teamBSecondaryBot2InitY;
                state=FIRSTTURNSOUTHTASK;
            }
        }

        if (myX==2500) {
            leftTeam = false;
        } else {
            leftTeam = true;
        }
        //System.out.println("lefteam "+leftTeam);

        //INIT
        isMoving=false;
        stepNumber=0;
        stepNumberMoveBack=0;
        isMovingBack=false;
    }
    public void step() {
        stepNumber++;

        if (getHealth()==0) state=SINK;

        //ODOMETRY CODE
        if (isMoving){
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
            realCoords();
            isMoving=false;
        }
        if (isMovingBack) {
            myX-=Parameters.teamAMainBotSpeed*Math.cos(myGetHeading());
            myY-=Parameters.teamAMainBotSpeed*Math.sin(myGetHeading());
            realCoords();
            isMovingBack=false;
        }
        //DEBUG MESSAGE
        if (whoAmI == ROCKY) sendLogMessage("#ROCKY *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+")." + "#state:"+state);
        else sendLogMessage("#MARIO *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+")."+ "#state:"+state);

        //RADAR DETECTION
        for (IRadarResult o: detectRadar()){
            if (o.getObjectType()== Types.OpponentMainBot || o.getObjectType()== Types.OpponentSecondaryBot) {
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+(o.getObjectType()== Types.OpponentMainBot?MAIN:SECONDARY)+":"+enemyX+":"+enemyY+":"+OVER);
/*        for (IRadarResult last : lastSeenEnnemies) {
        		double eX = myX+last.getObjectDistance()*Math.cos(last.getObjectDirection());
        		double eY = myY+last.getObjectDistance()*Math.sin(last.getObjectDirection());
        		if (Math.abs(eX-enemyX)<1 && Math.abs(eY-enemyY)<1) {
        			broadcast(whoAmI+":"+TEAM+":"+CAMP+":"+enemyX+":"+enemyY+":"+OVER);
        			//System.out.println("trouvé un camp en "+enemyX +" "+enemyY);
        		}
        }*/
                ennemies.add(o);
            }
        }
        //lastSeenEnnemies = new ArrayList<>(ennemies);
        //COMMUNICATION
        broadcast(whoAmI+":"+TEAM+":"+POSITION+":"+myX+":"+myY+":"+myGetHeading()+":"+OVER);

        ennemies.clear();
        for (IRadarResult o: detectRadar()){
            //détection des ennemies
            if ((o.getObjectType()== Types.OpponentMainBot &&
                    o.getObjectDistance() <= Parameters.teamBMainBotFrontalDetectionRange + 100) ||
                    (o.getObjectType()== Types.OpponentSecondaryBot &&
                            o.getObjectDistance() <= Parameters.teamASecondaryBotFrontalDetectionRange - 150)) {
                ennemies.add(o);
                if (state==MOVETASK)
                    state=FLEE;
            }
            //détection de collision avec un membre de mon équipe ou une épave
            if (o.getObjectDistance() < 120 && o.getObjectType() != Types.BULLET) {
                if (state==MOVETASK) {
                    //System.out.println("detected something, moving back");
                    state=MOVEBACKTASK;
                    stepNumberMoveBack = stepNumber;
                }
            }
      /*if (keepGoing && (o.getObjectType()==IRadarResult.Types.TeamMainBot 
    		  || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot 
    		  || o.getObjectType()==IRadarResult.Types.Wreck)) {
    	  	if (o.getObjectDistance() < o.getObjectRadius()+Parameters.teamAMainBotRadius+MINDISTANCE) {
    	  		//System.out.println("secondary detecct an obstacle - > keepGoin = false");
    	  		//System.out.println(whoAmI+" getObjectDirection = " + o.getObjectDirection());
    	  		sendLogMessage("detecct an obstacle - > keepGoin = false");
    	  		keepGoing = false;
    	  		turnDirection = o.getObjectDirection() > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT;
    	  		turnAngle = o.getObjectDirection() > 0 ? -MINANGLE : MINANGLE;
    	  	}
      }*/
        }

        //AUTOMATON

        /* when bot is sticked to wall */
        if (myX<=Parameters.teamASecondaryBotRadius) {
            if (isHeading(Parameters.EAST)) {
                state=MOVETASK;
                return;
            }
            state=TURNEASTTASK;
            return;
        }
        if (myX>=(3000-Parameters.teamASecondaryBotRadius)) {
            if (isHeading(Parameters.WEST)) {
                state=MOVETASK;
                return;
            }
            state=TURNWESTTASK;
            return;
        }
        if (myY<=Parameters.teamASecondaryBotRadius) {
            if (isHeading(Parameters.SOUTH)) {
                state=MOVETASK;
                return;
            }
            state=TURNSOUTHTASK;
            return;
        }
        if (myY>=(2000-Parameters.teamASecondaryBotRadius)) {
            state=TURNNORTHTASK;
            if (isHeading(Parameters.NORTH)) {
                state=MOVETASK;
                return;
            }
            return;
        }

        //FIRST MOVE
        if (state==FIRSTMOVETASK) {
            myMove(); //And what to do when blind blocked?
            if (whoAmI == MARIO) {
                if (myY>1800) { //mario fait un tour plus grand
                    if (leftTeam) {
                        state=TURNEASTTASK;
                    } else {
                        state = TURNWESTTASK;
                    }
                    return;
                }
            } else {
                if (myY<500) {
                    if (leftTeam) {
                        state=TURNEASTTASK;
                    } else {
                        state = TURNWESTTASK;
                    }
                    return;
                }
            }
        }

        if (state==FIRSTTURNNORTHTASK && !(isHeading(Parameters.NORTH))) {
            //System.out.println("getHeading " + (myGetHeading()*180/Math.PI));
            if (myGetHeading() < Math.PI / 2 || myGetHeading() > 3 * Math.PI / 2) {
                stepTurn(Direction.LEFT);
            }
            else {
                //System.out.println("else");
                stepTurn(Direction.RIGHT);
            }
            return;
        }
        if (state==FIRSTTURNNORTHTASK && isHeading(Parameters.NORTH)) {
            state=FIRSTMOVETASK;
            myMove();
            return;
        }
        if (state==FIRSTTURNSOUTHTASK && !(isHeading(Parameters.SOUTH))) {
            if (myGetHeading() < Math.PI / 2 || myGetHeading() > 3 * Math.PI / 2) {
                stepTurn(Direction.RIGHT);
            }
            else {
                stepTurn(Direction.LEFT);
            }
            return;
        }
        if (state==FIRSTTURNSOUTHTASK && isHeading(Parameters.SOUTH)) {
            //System.out.println("heading de south "+ (getHeading()*180/Math.PI));
            state=FIRSTMOVETASK;
            myMove();
            return;
        }

        /* 4 directions turning */
        if (state==TURNNORTHTASK && !(isHeading(Parameters.NORTH))) {
            //System.out.println("getHeading " + (myGetHeading()*180/Math.PI));
            if (myGetHeading() < Math.PI / 2 || myGetHeading() > 3 * Math.PI / 2) {
                stepTurn(Direction.LEFT);
            }
            else {
                //System.out.println("else");
                stepTurn(Direction.RIGHT);
            }
            return;
        }
        if (state==TURNNORTHTASK && isHeading(Parameters.NORTH)) {
            state=MOVETASK;
            myMove();
            return;
        }
        if (state==TURNSOUTHTASK && !(isHeading(Parameters.SOUTH))) {
            if (myGetHeading() < Math.PI / 2 || myGetHeading() > 3 * Math.PI / 2) {
                stepTurn(Direction.RIGHT);
            }
            else {
                stepTurn(Direction.LEFT);
            }
            return;
        }
        if (state==TURNSOUTHTASK && isHeading(Parameters.SOUTH)) {
            //System.out.println("heading de south "+ (getHeading()*180/Math.PI));
            state=MOVETASK;
            myMove();
            return;
        }
        if (state==TURNEASTTASK && !(isHeading(Parameters.EAST))) {
            if (myGetHeading() < Math.PI && myGetHeading() > 0) {
                stepTurn(Direction.LEFT);
            }
            else {
                stepTurn(Direction.RIGHT);
            }
            return;
        }
        if (state==TURNEASTTASK && isHeading(Parameters.EAST)) {
            state=MOVETASK;
            myMove();
            return;
        }
        if (state==TURNWESTTASK && !(isHeading(Parameters.WEST))) {
            if (myGetHeading() < Math.PI && myGetHeading() > 0) {
                stepTurn(Direction.RIGHT);
            }
            else {
                stepTurn(Direction.LEFT);
            }
            return;
        }
        if (state==TURNWESTTASK && isHeading(Parameters.WEST)) {
            state=MOVETASK;
            myMove();
            return;
        }


        if (state==MOVETASK) {
            if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
                if (whoAmI == MARIO) {
                    if ((myX>2800 && myY>1800) || (myX > 2800 && myY<200) || (myX<200 && myY<200) || (myX<200 && myY>1800)) {
                        state=TURNLEFTTASK;
                        endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                        stepTurn(Direction.LEFT);
                        return;
                    } else if (myX>2800 || myX<200) {
                        if (isHeading(Parameters.EAST) || isHeading(Parameters.WEST)){
                            state=TURNLEFTTASK;
                            endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                            stepTurn(Direction.LEFT);
                            return;
                        } else {
                            myMove();
                            return;
                        }
                    } else if (myY>1800 || myY<200){
                        if (isHeading(Parameters.NORTH) || isHeading(Parameters.SOUTH)){
                            state=TURNLEFTTASK;
                            endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                            stepTurn(Direction.LEFT);
                            return;
                        } else {
                            myMove();
                            return;
                        }
                    } else {
                        myMove();
                        return;
                    }
    				
    		    	 /* if ((myY>1800 || myY<200) && (myX>2800 || myX<200)){ //mario arrive au coin
    		    		  System.out.println("hohohoho");
    		    		  state=TURNLEFTTASK;
    		    		  endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
    		    		  stepTurn(Parameters.Direction.LEFT);
    		    		  return;
    		    	  }
    		    	   
    		    	  if (myY>1800 || myY<200 || myX>2800 || myX<200) { //quand c'est pas le coin
    		    		  System.out.println("ohllhlhhlh");
    		    		  state=TURNLEFTTASK;
    		    		  endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
    		    		  stepTurn(Parameters.Direction.LEFT);
    		    		  return;
    		    	  } else {
    		    		  myMove();
    		    		  return;
    		    	  }*/
                } else { //rocky fait un tour normal
                    //System.out.println("hehehehe");
                    state=TURNLEFTTASK;
                    endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                    stepTurn(Direction.LEFT);
                    return;
                }
            }
            myMove();
            return;
    		/*else {
    			if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
    		        myMove(); 
    		        	return;
    		     } else {
    		    	 	System.out.println("il existe des cas dans le else");
    		    	 	if (Math.random() < 0.5) {	    	 		
    		    	 		state=TURNLEFTTASK;
    		    	 		endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
    		    	 		stepTurn(Parameters.Direction.LEFT);
    		    	 	} else {
    		    	 		state=TURNRIGHTTASK;
    		    	 		endTaskDirection = getHeading() + Parameters.RIGHTTURNFULLANGLE;
    		    	 		stepTurn(Parameters.Direction.RIGHT);
    		    	 	}
    		    	 	return;
    		     }
    		}
    	*/

        }

        if (state==TURNRIGHTTASK) {
            if (isHeading(endTaskDirection)) {
                state=MOVETASK;
                myMove();
            } else {
                stepTurn(Direction.RIGHT);
            }
            return;
        }

        if (state==TURNLEFTTASK) {
            if (isHeading(endTaskDirection)) {
                state=MOVETASK;
                myMove();
            } else {
                stepTurn(Direction.LEFT);
            }
            return;
        }

        if (state==MOVEBACKTASK) {
            if (stepNumber < stepNumberMoveBack + 25) {
                myMoveBack();
                return;
            } else {
                if (Math.random() < 0.5) {
                    state=TURNLEFTTASK;
                    endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                    stepTurn(Direction.LEFT);
                } else {
                    state=TURNRIGHTTASK;
                    endTaskDirection = getHeading() + Parameters.RIGHTTURNFULLANGLE;
                    stepTurn(Direction.RIGHT);
                }
                return;
            }
        }



        if (state==FLEE) {
            if ((myX>2900 || myX<100) && (myY>1900 || myX<100)){
                state=TURNRIGHTTASK;
                endTaskDirection = getHeading() + Parameters.RIGHTTURNFULLANGLE;
                stepTurn(Direction.RIGHT);
                return;
            } else if (myX>2900 || myX<100) {
                if (isHeading(Parameters.EAST) || isHeading(Parameters.WEST)){
                    state=TURNRIGHTTASK;
                    endTaskDirection = getHeading() + Parameters.RIGHTTURNFULLANGLE;
                    stepTurn(Direction.RIGHT);
                    return;
                } else {
                    moveBack();
                    myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                    myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
                    realCoords();
                    if (ennemies.isEmpty()) {
                        state=MOVETASK;
                    }
                    return;
                }
            } else if (myY>1900 || myY<100){
                if (isHeading(Parameters.NORTH) || isHeading(Parameters.SOUTH)){
                    state=TURNRIGHTTASK;
                    endTaskDirection = getHeading() + Parameters.RIGHTTURNFULLANGLE;
                    stepTurn(Direction.RIGHT);
                    return;
                } else {
                    moveBack();
                    myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                    myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
                    realCoords();
                    if (ennemies.isEmpty()) {
                        state=MOVETASK;
                    }
                    return;
                }
            } else {
                moveBack();
                myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
                realCoords();
                if (ennemies.isEmpty()) {
                    state=MOVETASK;
                }
                return;
            }

        }

        if (state==SINK) {
            //myMove();
            return;
        }
        if (true) {
            return;
        }
    }
    private void myMove(){
        isMoving=true;
        move();
    }
    private void myMoveBack() {
        isMovingBack=true;
        moveBack();
    }
    private double myGetHeading(){
        return normalizeRadian(getHeading());
    }
    private double normalizeRadian(double angle){
        double result = angle;
        while(result<0) result+=2*Math.PI;
        while(result>=2*Math.PI) result-=2*Math.PI;
        return result;
    }

    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(dir1-dir2)<ANGLEPRECISION;
    }
    private boolean isHeading(double dir){
        return Math.abs(Math.sin(getHeading()-dir))<ANGLEPRECISIONBIS;
    }
    private void realCoords() {
        myX = myX < 0 ? 0 : myX;
        myX = myX > 3000 ? 3000 : myX;
        myY = myY < 0 ? 0 : myY;
        myY = myY > 2000 ? 2000 : myY;
    }
}